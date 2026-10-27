package restuarant.service;
import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.model.*;
import restuarant.persistence.RestaurantState;
import java.util.*;
/** The state monitor protects claims, completions, cancellation and saves together. */
public class KitchenService implements AutoCloseable {
    private final RestaurantState state;
    private final Runnable save;
    private final long millisPerMinute;
    private final ChefAssignmentService assignment=new ChefAssignmentService();
    private final Map<Integer,Thread> workers=new HashMap<>();
    private boolean stopping;
    public KitchenService(RestaurantState state,Runnable save,long timing) {
        if(state==null || timing<1 || timing>60000) throw new IllegalArgumentException("Timing must be 1..60000 ms per menu minute");
        this.state=state;this.save=save;millisPerMinute=timing;
    }
    public long getMillisPerMinute() { return millisPerMinute; }
    public void dispatch() {
        Access.require(Role.ADMIN,Role.MANAGER,Role.CHEF,Role.WAITER,Role.CASHIER);
        synchronized(state) {
            if(stopping || !state.getBusinessDay().isOpen()) return;
            for(int i=0;i<state.getStaff().getCount();i++) {
                StaffAccount a=state.getStaff().getAccountAt(i);
                if(a.isActive() && a.getRole()==Role.CHEF && !workers.containsKey(a.getEmployeeId())) {
                    Thread t=new Thread(new ChefWorker(new Chef(a.getEmployeeId(),a.getName()),this),"Chef-"+a.getEmployeeId());
                    workers.put(a.getEmployeeId(),t); t.start();
                }
            }
        }
    }
    // A public claim requires the logged-in chef. Background claims use the registered worker identity.
    public OrderItem claimNextTask(int chefId) {
        StaffAccount a=Access.require(Role.CHEF);
        if(a.getEmployeeId()!=chefId) throw new SecurityException("Cannot impersonate another chef");
        return claim(chefId,false);
    }
    boolean hasPendingFor(int chefId) {
        synchronized(state) {
            StaffAccount a=state.getStaff().findAccount(chefId);
            if(stopping || !state.getBusinessDay().isOpen() || a==null || !a.isActive() || a.getRole()!=Role.CHEF || assignment.workload(state.getOrderService(),chefId)>0) return false;
            for(int i=0;i<state.getOrderService().getOrderCount();i++) {
                Order o=state.getOrderService().getOrderAt(i);
                if(o.getStatus()!=Order.OrderStatus.OPEN) continue;
                for(int j=0;j<o.getItemCount();j++) if(o.getItem(j).getStatus()==OrderItem.Status.PENDING && a.canCook(o.getItem(j).getMenuItem().getCategory()) && !state.getCancellations().isHeld(o.getOrderId(),j)) return true;
            }
            return false;
        }
    }
    OrderItem workerClaim(int chefId) { return claim(chefId,true); }
    private static final class Task {
        final Order order;final int index;final OrderItem item;
        Task(Order order,int index){this.order=order;this.index=index;item=order.getItem(index);}
    }
    /** Explicit priority policy: urgent, arrival order, shortest preparation within that order, line number. */
    private List<Task> pendingTasks(){
        List<Task> tasks=new ArrayList<>();
        OrderServices orders=state.getOrderService();
        for(int i=0;i<orders.getOrderCount();i++){
            Order o=orders.getOrderAt(i);if(o.getStatus()!=Order.OrderStatus.OPEN)continue;
            for(int j=0;j<o.getItemCount();j++)if(o.getItem(j).getStatus()==OrderItem.Status.PENDING && !state.getCancellations().isHeld(o.getOrderId(),j))tasks.add(new Task(o,j));
        }
        tasks.sort((a,b)->{
            int urgent=Boolean.compare(b.order.isUrgent(),a.order.isUrgent());if(urgent!=0)return urgent;
            int arrival=Integer.compare(a.order.getOrderId(),b.order.getOrderId());if(arrival!=0)return arrival;
            int prep=Integer.compare(a.item.getMenuItem().getPreparationTime(),b.item.getMenuItem().getPreparationTime());
            return prep!=0?prep:Integer.compare(a.index,b.index);
        });return tasks;
    }
    public List<String> priorityQueue(){
        Access.current();synchronized(state){
            List<String> result=new ArrayList<>();
            for(Task t:pendingTasks())result.add("Order "+t.order.getOrderId()+" item "+(t.index+1)+" | "+(t.order.isUrgent()?"URGENT":"NORMAL")+" | "+t.item+" | "+(assignment.choose(state.getStaff(),state.getOrderService(),t.item)==null?"NO QUALIFIED ACTIVE CHEF":"eligible chef available"));
            return result;
        }
    }
    private OrderItem claim(int chefId,boolean worker) {
        synchronized(state) {
            if(worker && workers.get(chefId)!=Thread.currentThread())throw new SecurityException("Unregistered kitchen worker");
            StaffAccount a=state.getStaff().findAccount(chefId);
            if(stopping || !state.getBusinessDay().isOpen() || a==null || !a.isActive() || a.getRole()!=Role.CHEF)return null;
            if(assignment.workload(state.getOrderService(),chefId)>0)return null;
            for(Task task:pendingTasks()){
                OrderItem item=task.item;
                StaffAccount selected=assignment.choose(state.getStaff(),state.getOrderService(),item);
                if(selected!=a)continue;
                synchronized(task.order){
                    if(item.getStatus()!=OrderItem.Status.PENDING || state.getCancellations().isHeld(task.order.getOrderId(),task.index))continue;
                    item.startPreparing(chefId);
                }
                state.audit("Chef "+chefId+" claimed order "+task.order.getOrderId()+" item "+(task.index+1));
                try{save.run();}catch(RuntimeException ex){item.recoverInterrupted();throw ex;}return item;
            }return null;
        }
    }
    public String delayReport(){return delayReport(System.currentTimeMillis());}
    public String delayReport(long now){
        Access.require(Role.ADMIN,Role.MANAGER,Role.CHEF);
        synchronized(state){
            StringBuilder text=new StringBuilder("KITCHEN DELAYS (milliseconds; demo timing "+millisPerMinute+" ms/menu minute)\n");
            int waiting=0,cooking=0,finished=0,late=0;long total=0;
            OrderServices orders=state.getOrderService();
            for(int i=0;i<orders.getOrderCount();i++){
                Order o=orders.getOrderAt(i);
                for(int j=0;j<o.getItemCount();j++){
                    OrderItem item=o.getItem(j);if(item.getStatus()==OrderItem.Status.CANCELLED)continue;
                    long expected=item.getMenuItem().getPreparationTime()*millisPerMinute;
                    long queued=item.getQueuedAt(),start=item.getPreparationStartedAt(),end=item.getPreparationFinishedAt();
                    long wait=queued==0?-1:Math.max(0,(start==0?now:start)-queued);
                    long actual=start==0?0:Math.max(0,(end==0?now:end)-start);
                    long delay=Math.max(0,actual-expected);
                    if(item.getStatus()==OrderItem.Status.PENDING)waiting++;
                    if(item.getStatus()==OrderItem.Status.PREPARING)cooking++;
                    if(end>0){finished++;total+=actual;}
                    if(delay>0)late++;
                    text.append("Order ").append(o.getOrderId()).append(" item ").append(j+1).append(" | ").append(item.getStatus())
                        .append(" | chef ").append(item.getAssignedChefId()).append(" | queue wait ms ").append(wait<0?"unknown (legacy)":wait)
                        .append(" | expected prep ms ").append(expected).append(" | actual/elapsed prep ms ").append(actual).append(" | prep delay ms ").append(delay).append('\n');
                }
            }
            text.append("Pending: ").append(waiting).append(" | Preparing: ").append(cooking).append(" | Completed preparations: ").append(finished)
                .append(" | Late preparations: ").append(late).append(" | Average completed prep ms: ").append(finished==0?0:total/finished);
            return text.toString();
        }
    }
    public void displayMyWork(){
        StaffAccount chef=Access.require(Role.CHEF);
        synchronized(state){
            boolean found=false;
            for(int i=0;i<state.getOrderService().getOrderCount();i++){
                Order o=state.getOrderService().getOrderAt(i);
                for(int j=0;j<o.getItemCount();j++)if(o.getItem(j).getAssignedChefId()==chef.getEmployeeId()){
                    System.out.println("Order "+o.getOrderId()+" item "+(j+1)+" | "+o.getItem(j));found=true;
                }
            }
            if(!found)System.out.println("No assigned dishes.");
        }
    }
    public void markReady(int order,int index) {
        StaffAccount a=Access.require(Role.CHEF);
        synchronized(state) {
            state.getBusinessDay().requireOpen();
            OrderItem item=state.getOrderService().requireOrder(order).getItem(index);
            if(item.getAssignedChefId()!=a.getEmployeeId()) throw new SecurityException("Dish assigned to another chef");
            item.markReady();state.audit("Chef "+a.getEmployeeId()+" marked dish ready");save.run();
        }
    }
    void complete(int chef,OrderItem item) {
        synchronized(state) {
            if(workers.get(chef)!=Thread.currentThread()) throw new SecurityException("Unregistered kitchen worker");
            if(item.getStatus()==OrderItem.Status.PREPARING && item.getAssignedChefId()==chef) {
                item.markReady();state.audit("Chef "+chef+" completed "+item.getMenuItem().getName());save.run();
            }
        }
    }
    void recover(int chef,OrderItem item) {
        synchronized(state) { if(item!=null && item.getStatus()==OrderItem.Status.PREPARING && item.getAssignedChefId()==chef) { item.recoverInterrupted();state.audit("Interrupted task returned to pending: chef "+chef);save.run(); } }
    }
    void finished(int chef) { synchronized(state) { workers.remove(chef); } }
    public void displayQueue() {
        Access.current();
        synchronized(state) {
            System.out.println("Pending dispatch order:");for(String row:priorityQueue())System.out.println(row);
            System.out.println("All active order dishes:");
            for(int i=0;i<state.getOrderService().getOrderCount();i++) {
                Order o=state.getOrderService().getOrderAt(i);
                if(o.getStatus()==Order.OrderStatus.OPEN) for(int j=0;j<o.getItemCount();j++) System.out.println("Order "+o.getOrderId()+" item "+(j+1)+" | "+o.getItem(j)+" | Chef "+o.getItem(j).getAssignedChefId());
            }
        }
    }
    public void awaitIdle() throws InterruptedException {
        while(true) {
            Thread[] running; synchronized(state) { running=workers.values().toArray(new Thread[0]); }
            if(running.length==0) return;
            for(Thread t:running) t.join();
        }
    }
    public void close() {
        Thread[] running; synchronized(state) { stopping=true;running=workers.values().toArray(new Thread[0]); }
        for(Thread t:running) t.interrupt();
        boolean interrupted=false;
        for(Thread t:running) { while(t.isAlive()) try { t.join(); } catch(InterruptedException ex) { interrupted=true; } }
        if(interrupted) Thread.currentThread().interrupt();
    }
}
