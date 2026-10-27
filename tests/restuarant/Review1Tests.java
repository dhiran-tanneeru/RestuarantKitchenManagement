package restuarant;
import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.billing.*;
import restuarant.model.*;
import restuarant.persistence.*;
import restuarant.service.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;

/** Dependency-free executable regression suite. Each case names the behavior it actually exercises. */
@SuppressWarnings("try")
public final class Review1Tests {
    private static int passed;
    private static final char[] PASSWORD="Review1-Test!".toCharArray();
    private static void check(String name,boolean ok) { if(!ok)throw new AssertionError(name);passed++;System.out.println("PASS | "+name); }
    private static void rejects(String name,Runnable operation) {
        try {operation.run();}catch(IllegalArgumentException|IllegalStateException|SecurityException ex){check(name,true);return;}
        throw new AssertionError("Expected rejection: "+name);
    }
    private static void login(StaffService staff,int id) {staff.login(id,PASSWORD);}
    private static Menuitem dish(int id,String category,String price) {Menuitem m=new Menuitem(id,"Dish "+id,category,Double.parseDouble(price),1);m.setPrice(new BigDecimal(price));return m;}
    public static void main(String[] args)throws Exception {
        System.setProperty("rkm.millisPerMinute","1000");
        Path directory=Files.createTempDirectory("rkm-review1-tests-");
        int persistedOrder;
        try(RestaurantApplication app=new RestaurantApplication(directory)) {
            StaffService staff=app.state.getStaff();OrderServices orders=app.state.getOrderService();BillingService billing=app.state.getBillingService();
            staff.addAccount(new StaffAccount(1,"Admin",Role.ADMIN,PASSWORD));
            check("First-time Admin creation",staff.getCount()==1);
            rejects("Incorrect password",()->staff.login(1,"wrong password".toCharArray()));
            rejects("Invalid employee login",()->staff.login(999,PASSWORD));
            login(staff,1);
            rejects("Blank password even with eight spaces",()->staff.addAccount(new StaffAccount(9,"Blank",Role.WAITER,"        ".toCharArray())));
            staff.addAccount(new StaffAccount(2,"Manager",Role.MANAGER,PASSWORD));
            staff.addAccount(new StaffAccount(3,"Waiter A",Role.WAITER,PASSWORD));
            staff.addAccount(new StaffAccount(4,"Waiter B",Role.WAITER,PASSWORD));
            staff.addAccount(new StaffAccount(6,"Chef B",Role.CHEF,PASSWORD));
            staff.addAccount(new StaffAccount(5,"Chef A",Role.CHEF,PASSWORD));
            staff.addAccount(new StaffAccount(7,"Cashier",Role.CASHIER,PASSWORD));
            for(int id=1;id<=7;id++){login(staff,id);check("Login role ID "+id,Access.current().getEmployeeId()==id);}
            login(staff,1);
            rejects("Duplicate account ID",()->staff.addAccount(new StaffAccount(3,"Duplicate",Role.WAITER,PASSWORD)));
            rejects("Last active Admin deactivation",()->staff.deactivateAccount(1));
            rejects("Last active Admin demotion",()->staff.updateEmployeeRole(1,Role.WAITER));
            staff.deactivateAccount(4);rejects("Inactive account login",()->login(staff,4));login(staff,1);staff.activateAccount(4);
            login(staff,4);check("Account reactivation",Access.current().getEmployeeId()==4);login(staff,1);
            staff.updateEmployeeName(3,"Updated waiter");check("Employee rename",staff.findAccount(3).getName().equals("Updated waiter"));
            staff.findAccount(5).setSpecializations("Main");staff.findAccount(6).setSpecializations("Main");
            check("Chef specialization stored",!staff.findAccount(5).canCook("Drinks"));
            orders.addTable(1,2);orders.addTable(2,4);orders.addTable(3,6);orders.addTable(4,8);orders.configureTable(4,7);
            rejects("Duplicate table ID",()->orders.addTable(1,4));
            app.state.getMenuService().addMenuItem(dish(10,"Main","100.10"));
            app.state.getMenuService().addMenuItem(dish(11,"Drinks","20.00"));
            rejects("Duplicate menu ID",()->app.state.getMenuService().addMenuItem(dish(10,"Main","1")));
            app.state.getMenuService().updateItem(11,"Drink","Drinks",new BigDecimal("25.00"),1,false);
            check("Menu edit and availability",!app.state.getMenuService().findItem(11).isAvailable());
            login(staff,3);
            Order o=app.seat(2);check("Smallest suitable table and least-loaded waiter",o.getTableNumber()==1 && o.getWaiterId()==3);
            Order second=app.seat(3);check("Waiter workload balancing",second.getTableNumber()==2 && second.getWaiterId()==4);
            rejects("Unrelated waiter order mutation",()->orders.addItemToOrder(second.getOrderId(),dish(20,"Main","1"),1,""));
            rejects("Waiter cannot create takeaway",()->orders.createTakeawayOrder());
            rejects("Waiter cannot manage menu",()->app.state.getMenuService().addMenuItem(dish(50,"Main","1")));
            rejects("Active-order rollback forbidden",()->orders.findTable(1).rollbackSeating());
            rejects("Active-order cleaning forbidden",()->orders.findTable(1).markForCleaning());
            rejects("Occupied table reuse",()->app.seatManual(1,2,3));
            rejects("Invalid quantity",()->orders.addItemToOrder(o.getOrderId(),dish(12,"Main","1"),0,""));
            rejects("Invalid menu item",()->orders.addItemToOrder(o.getOrderId(),null,1,""));
            rejects("Unavailable dish",()->orders.addItemToOrder(o.getOrderId(),app.state.getMenuService().findItem(11),1,""));
            login(staff,1);
            rejects("Invalid manual waiter ID",()->app.seatManual(3,2,999));
            rejects("Manual seating wrong staff role",()->app.seatManual(3,2,7));
            rejects("No suitable table",()->app.seat(100));
            staff.deactivateAccount(3);staff.deactivateAccount(4);
            rejects("No active waiter",()->app.seat(1));
            staff.activateAccount(3);staff.activateAccount(4);
            check("Failed seating does not occupy table",orders.findTable(3).getStatus()==RestuarantTable.Status.AVAILABLE);
            rejects("Unsafe capacity edit",()->orders.configureTable(1,1));
            app.transfer(second.getOrderId(),3,"Larger group");
            check("Table transfer preserves order and cleans old table",second.getTableNumber()==3 && orders.findTable(2).getStatus()==RestuarantTable.Status.CLEANING);
            app.reassign(second.getOrderId(),3,"Cover colleague");check("Waiter reassignment synchronized",second.getWaiterId()==3 && orders.findTable(3).getWaiterId()==3);
            orders.clean(2);check("Transfer source reusable after cleaning",orders.findTable(2).getStatus()==RestuarantTable.Status.AVAILABLE);
            Menuitem meal=app.state.getMenuService().findItem(10);
            orders.addItemToOrder(o.getOrderId(),meal,2,"Less spice");orders.addItemToOrder(o.getOrderId(),meal,1,"");
            CancellationRequest request=app.state.getCancellations().request(o,1,"Customer changed mind");
            rejects("Duplicate pending cancellation",()->app.state.getCancellations().request(o,1,"Again"));
            login(staff,3);rejects("Waiter cannot approve cancellation",()->app.state.getCancellations().decide(request.id,true,"ok",orders));
            login(staff,2);app.state.getCancellations().decide(request.id,true,"Approved before cooking",orders);
            check("Cancellation approval changes actual dish",o.getItem(1).getStatus()==OrderItem.Status.CANCELLED);
            rejects("Repeated cancellation decision",()->app.state.getCancellations().decide(request.id,false,"again",orders));
            login(staff,1);
            // Rejected requests release the kitchen hold.
            orders.addItemToOrder(o.getOrderId(),meal,1,"");
            CancellationRequest rejected=app.state.getCancellations().request(o,2,"Request");
            app.state.getCancellations().decide(rejected.id,false,"Customer keeps dish",orders);
            check("Cancellation rejection releases hold",!app.state.getCancellations().isHeld(o.getOrderId(),2));
            app.kitchen.dispatch();app.kitchen.awaitIdle();
            check("Shared automatic kitchen completes eligible dishes",o.getItem(0).getStatus()==OrderItem.Status.READY && o.getItem(2).getStatus()==OrderItem.Status.READY);
            check("Two real chef threads overlap preparation",o.getItem(0).getAssignedChefId()!=o.getItem(2).getAssignedChefId() && Math.max(o.getItem(0).getPreparationStartedAt(),o.getItem(2).getPreparationStartedAt()) < Math.min(o.getItem(0).getPreparationFinishedAt(),o.getItem(2).getPreparationFinishedAt()));
            check("Preparation timing and special instructions retained",o.getItem(0).getActualPreparationTime()>0 && o.getItem(0).getInstructions().equals("Less spice"));
            rejects("Cannot cancel prepared dish",()->app.state.getCancellations().request(o,0,"Too late"));
            login(staff,3);orders.serve(o.getOrderId(),0);orders.serve(o.getOrderId(),2);
            Bill bill=billing.generateBill(o);check("Exact decimal subtotal",bill.getSubtotal().equals(new BigDecimal("300.30")));
            rejects("Unauthorized waiter discount",()->billing.discount(o.getOrderId(),new BigDecimal("10")));
            rejects("Unauthorized direct payment service",()->billing.pay(o.getOrderId(),Payment.Method.CASH,BigDecimal.ONE));
            rejects("Forged Cashier object cannot authorize payment",()->billing.recordPayment(o.getOrderId(),Payment.Method.CASH,BigDecimal.ONE,new Cashier(7,"Fake")));
            login(staff,2);billing.discount(o.getOrderId(),new BigDecimal("10"));
            check("Tax after discount with decimal rounding",bill.getTotal().equals(new BigDecimal("283.78")));
            rejects("Discount above 100",()->billing.discount(o.getOrderId(),new BigDecimal("101")));
            login(staff,7);billing.pay(o.getOrderId(),Payment.Method.CASH,new BigDecimal("100.00"));
            rejects("Partial payment prevents closure",()->billing.closePaidOrder(o,orders.findTable(1)));
            rejects("Overpayment",()->billing.pay(o.getOrderId(),Payment.Method.CARD,new BigDecimal("999")));
            rejects("Zero payment",()->billing.pay(o.getOrderId(),Payment.Method.CASH,BigDecimal.ZERO));
            rejects("Payment fraction beyond paise",()->billing.pay(o.getOrderId(),Payment.Method.CASH,new BigDecimal("0.001")));
            login(staff,1);
            rejects("Direct service add after payment",()->orders.addItemToOrder(o.getOrderId(),meal,1,""));
            rejects("Direct model add after payment",()->o.addItem(meal,1,""));
            rejects("Discount frozen after partial payment",()->billing.discount(o.getOrderId(),BigDecimal.ZERO));
            rejects("Cancellation after payment",()->app.state.getCancellations().request(o,0,"Paid"));
            billing.pay(o.getOrderId(),Payment.Method.UPI,new BigDecimal("100.00"));billing.pay(o.getOrderId(),Payment.Method.CARD,new BigDecimal("83.78"));
            check("Split payment balance reaches zero",bill.isFullyPaid() && bill.getPaymentCount()==3);
            rejects("Repeated payment on fully paid bill",()->billing.pay(o.getOrderId(),Payment.Method.CASH,BigDecimal.ONE));
            billing.closePaidOrder(o,orders.findTable(1));check("Paid served order closes to cleaning",o.getStatus()==Order.OrderStatus.CLOSED && orders.findTable(1).getStatus()==RestuarantTable.Status.CLEANING);
            rejects("Closed order cannot reopen",()->orders.addItemToOrder(o.getOrderId(),meal,1,""));
            login(staff,3);orders.clean(1);check("Checkout cleaning and reuse",orders.findTable(1).getStatus()==RestuarantTable.Status.AVAILABLE);
            Order reused=app.seat(2);check("Reused table receives unique new order",reused.getTableNumber()==1 && reused.getOrderId()!=o.getOrderId());
            login(staff,7);
            Order take=orders.createTakeawayOrder();persistedOrder=take.getOrderId();orders.addItemToOrder(take.getOrderId(),meal,1,"");
            check("Takeaway requires no table and has unique token",take.getTableNumber()==0 && take.getOrderId()!=reused.getOrderId());
            rejects("Collect unpaid takeaway",()->app.collect(take.getOrderId()));
            app.kitchen.dispatch();app.kitchen.awaitIdle();
            billing.generateBill(take);billing.pay(take.getOrderId(),Payment.Method.UPI,billing.findBill(take.getOrderId()).getTotal());app.collect(take.getOrderId());
            check("Takeaway kitchen payment collection complete",take.getStatus()==Order.OrderStatus.CLOSED && take.getItem(0).getStatus()==OrderItem.Status.COLLECTED);
            check("Revenue report reconciles receipts",app.report().contains("388.89") && app.report().contains("CARD=83.78"));
            login(staff,1);
            Order drink=orders.createTakeawayOrder();Menuitem beverage=dish(77,"Drinks","1.00");orders.addItemToOrder(drink.getOrderId(),beverage,1,"");app.kitchen.dispatch();app.kitchen.awaitIdle();
            check("No qualified chef leaves task pending",drink.getItem(0).getStatus()==OrderItem.Status.PENDING);
            staff.findAccount(6).setSpecializations("Drinks");staff.deactivateAccount(6);app.kitchen.dispatch();app.kitchen.awaitIdle();
            check("Inactive chef gets no task",drink.getItem(0).getStatus()==OrderItem.Status.PENDING);
            staff.activateAccount(6);app.kitchen.dispatch();app.kitchen.awaitIdle();check("Chef activation and category dispatch",drink.getItem(0).getAssignedChefId()==6 && drink.getItem(0).getStatus()==OrderItem.Status.READY);
            app.save();app.save();check("Repeated atomic save creates backup",Files.exists(directory.resolve("review1.dat.bak")));
            check("Cancellation and audit persisted in shared state",app.state.getCancellations().history().size()==2 && !app.state.getHistory().isEmpty());
            Access.logout();rejects("Logged-out service mutation",()->orders.createTakeawayOrder());login(staff,1);
            // Beyond all former menu/order/item limits; no cooking needed.
            for(int i=100;i<205;i++)app.state.getMenuService().addMenuItem(dish(i,"Main","1"));
            check("Menu grows beyond 100",app.state.getMenuService().getItems().length>100);
            Order large=orders.createTakeawayOrder();for(int i=0;i<101;i++)orders.addItemToOrder(large.getOrderId(),meal,1,"");
            check("Order grows beyond 100 lines",large.getItemCount()==101);
            for(int i=0;i<501;i++)orders.createTakeawayOrder();check("Order registry grows beyond 500",orders.getOrderCount()>500);
            Set<Integer> ids=new HashSet<>();for(int i=0;i<orders.getOrderCount();i++)ids.add(orders.getOrderAt(i).getOrderId());check("All generated order IDs unique",ids.size()==orders.getOrderCount());
        }
        try(RestaurantApplication app=new RestaurantApplication(directory)) {
            login(app.state.getStaff(),1);
            check("Login after restart",Access.current().getRole()==Role.ADMIN);
            check("Paid takeaway survives reload",app.state.getOrderService().requireOrder(persistedOrder).getStatus()==Order.OrderStatus.CLOSED);
            check("Payments survive reload",app.report().contains("388.89"));
            check("Cancellation history survives reload",app.state.getCancellations().history().size()==2);
            Order o=app.state.getOrderService().createTakeawayOrder();check("Continue operations after loading",o.getOrderId()>persistedOrder);
            rejects("Loaded payment lock still enforced",()->app.state.getOrderService().addItemToOrder(1001,dish(1,"Main","1"),1,""));
            boolean locked=false;try(RestaurantApplication ignored=new RestaurantApplication(directory)){}catch(java.io.IOException ex){locked=true;}
            check("Second application cannot concurrently use same data",locked);
        }
        byte[] valid=Files.readAllBytes(directory.resolve("review1.dat"));Files.write(directory.resolve("review1.dat"),new byte[]{1,2,3});
        boolean corrupt=false;try(RestaurantApplication ignored=new RestaurantApplication(directory)){}catch(java.io.IOException ex){corrupt=true;}
        check("Corrupt snapshot fails closed",corrupt && Files.size(directory.resolve("review1.dat"))==3);
        Files.write(directory.resolve("review1.dat"),valid);
        interruptTest();legacyTest(args);saveFailureTest();extraTests();
        System.out.println("TOTAL PASS: "+passed);
    }
    private static void extraTests() throws Exception {
        Path dir=Files.createTempDirectory("rkm-edge-");
        try(RestaurantApplication app=new RestaurantApplication(dir)) {
            StaffService staff=app.state.getStaff();OrderServices orders=app.state.getOrderService();BillingService billing=app.state.getBillingService();
            staff.addAccount(new StaffAccount(1,"Admin",Role.ADMIN,PASSWORD));login(staff,1);
            staff.addAccount(new StaffAccount(2,"Waiter",Role.WAITER,PASSWORD));staff.addAccount(new StaffAccount(3,"Chef",Role.CHEF,PASSWORD));
            orders.addTable(1,2);
            Order empty=app.seat(1);app.abandonEmpty(empty.getOrderId(),"Guests left before ordering");
            check("Empty-session abandonment follows cleaning workflow",empty.getStatus()==Order.OrderStatus.CLOSED && orders.findTable(1).getStatus()==RestuarantTable.Status.CLEANING);
            orders.clean(1);
            rejects("Bill null order rejected",()->billing.generateBill(null));
            Order o=orders.createTakeawayOrder();orders.addItemToOrder(o.getOrderId(),dish(1,"Main","100"),1,"");
            Bill b=billing.generateBill(o);
            billing.applyPredefinedDiscount(o.getOrderId(),new Cashier(9,"Compatibility parameter"));
            check("Legacy predefined discount respects manager/admin policy",b.getDiscountAmount().equals(new BigDecimal("10.00")));
            billing.discount(o.getOrderId(),BigDecimal.ZERO);
            rejects("Fabricated duplicate order cannot obtain canonical bill",()->billing.generateBill(new Order(o.getOrderId(),Order.OrderType.TAKEAWAY,0)));
            rejects("Empty order cannot be billed",()->billing.generateBill(orders.createTakeawayOrder()));
            Payment payment=billing.pay(o.getOrderId(),Payment.Method.CASH,BigDecimal.ONE);
            rejects("Duplicate payment ID",()->b.addPayment(payment));
            for(int i=0;i<21;i++)billing.pay(o.getOrderId(),Payment.Method.UPI,BigDecimal.ONE);
            check("Payment storage grows beyond 20 entries",b.getPaymentCount()==22);
            billing.pay(o.getOrderId(),Payment.Method.CARD,b.getBalance());
            rejects("Fully paid unfinished dish still prevents order closure",()->billing.closePaidOrder(o,null));
            // Two chef threads try claiming the same pending line: exactly one can acquire it.
            Order claim=orders.createTakeawayOrder();orders.addItemToOrder(claim.getOrderId(),dish(2,"Other","1"),1,"");
            staff.findAccount(3).setSpecializations("Other");
            java.util.concurrent.atomic.AtomicInteger claims=new java.util.concurrent.atomic.AtomicInteger();
            Runnable work=()->{login(staff,3);try {if(app.kitchen.claimNextTask(3)!=null)claims.incrementAndGet();}finally{Access.logout();}};
            Thread first=new Thread(work),second=new Thread(work);first.start();second.start();first.join();second.join();
            check("Concurrent claims cannot duplicate a dish",claims.get()==1 && claim.getItem(0).getStatus()==OrderItem.Status.PREPARING);
            login(staff,3);app.kitchen.markReady(claim.getOrderId(),0);check("Chef manual ready works for assigned dish",claim.getItem(0).getStatus()==OrderItem.Status.READY);
            rejects("Chef cannot impersonate another chef",()->app.kitchen.claimNextTask(123));
            rejects("Chef cannot approve cancellation directly",()->o.getItem(0).cancel());
            // Invalidate a session from another thread and verify the old session cannot operate.
            login(staff,2);
            Thread deactivate=new Thread(()->{login(staff,1);staff.deactivateAccount(2);Access.logout();});deactivate.start();deactivate.join();
            rejects("Previously logged-in deactivated employee cannot act",()->app.seat(1));
            login(staff,1);staff.activateAccount(2);staff.resetPassword(2,"Replacement!".toCharArray());
            rejects("Old password rejected after reset",()->login(staff,2));staff.login(2,"Replacement!".toCharArray());check("New reset password authenticates",Access.current().getEmployeeId()==2);
            login(staff,1);staff.updateEmployeeRole(2,Role.MANAGER);staff.login(2,"Replacement!".toCharArray());check("Changed employee role used by login",Access.current().getRole()==Role.MANAGER);
            login(staff,1);
            for(int i=10;i<111;i++)staff.addAccount(new StaffAccount(i,"Staff "+i,Role.WAITER,PASSWORD));check("Staff registry grows beyond 100",staff.getCount()>100);
            Order failure=orders.createTakeawayOrder();orders.addItemToOrder(failure.getOrderId(),dish(31,"Failure","1"),1,"");staff.findAccount(3).setSpecializations("Failure");
            KitchenService failing=new KitchenService(app.state,()->{throw new IllegalStateException("Injected save failure");},1);
            login(staff,3);rejects("Kitchen claim save failure is reported",()->failing.claimNextTask(3));
            check("Failed claim persistence returns dish to pending",failure.getItem(0).getStatus()==OrderItem.Status.PENDING);failing.close();login(staff,1);
            app.save();String bytes=new String(Files.readAllBytes(dir.resolve("review1.dat")),java.nio.charset.StandardCharsets.ISO_8859_1);
            check("Snapshot contains no plaintext test passwords",!bytes.contains("Review1-Test!") && !bytes.contains("Replacement!"));
            // Competing payment and addition: serialized by the order lock, so no line can appear after payment began.
            Order race=orders.createTakeawayOrder();orders.addItemToOrder(race.getOrderId(),dish(9,"Main","100"),1,"");billing.generateBill(race);
            java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
            Thread payer=new Thread(()->{try{login(staff,1);billing.pay(race.getOrderId(),Payment.Method.CASH,BigDecimal.ONE);}catch(Throwable ex){error.set(ex);}finally{Access.logout();}});
            Thread adder=new Thread(()->{try{login(staff,1);orders.addItemToOrder(race.getOrderId(),dish(9,"Main","100"),1,"");}catch(IllegalStateException expected){}catch(Throwable ex){error.set(ex);}finally{Access.logout();}});
            payer.start();adder.start();payer.join();adder.join();
            check("Concurrent payment/addition leaves locked consistent order",error.get()==null && race.hasPaymentStarted() && billing.findBill(race.getOrderId()).getPaidAmount().compareTo(BigDecimal.ONE)==0);
        }
    }
    private static void interruptTest() throws Exception {
        Path dir=Files.createTempDirectory("rkm-interrupt-");System.setProperty("rkm.millisPerMinute","10000");int id;
        try(RestaurantApplication app=new RestaurantApplication(dir)){
            StaffService s=app.state.getStaff();s.addAccount(new StaffAccount(1,"Admin",Role.ADMIN,PASSWORD));login(s,1);s.addAccount(new StaffAccount(2,"Chef",Role.CHEF,PASSWORD));
            Order o=app.state.getOrderService().createTakeawayOrder();id=o.getOrderId();app.state.getOrderService().addItemToOrder(id,dish(1,"Main","1"),1,"");app.kitchen.dispatch();
            long until=System.currentTimeMillis()+3000;while(o.getItem(0).getStatus()!=OrderItem.Status.PREPARING && System.currentTimeMillis()<until)Thread.sleep(5);
            check("Worker actually entered preparing",o.getItem(0).getStatus()==OrderItem.Status.PREPARING);
            app.save();
            RestaurantState crash=SnapshotStore.load(dir.resolve("review1.dat"));check("Crash reload reconstructs interrupted task",crash.getOrderService().requireOrder(id).getItem(0).getStatus()==OrderItem.Status.PENDING);
            app.kitchen.close();check("Interrupted worker requeues task",o.getItem(0).getStatus()==OrderItem.Status.PENDING);
        }
        System.setProperty("rkm.millisPerMinute","1");
        try(RestaurantApplication app=new RestaurantApplication(dir)){login(app.state.getStaff(),1);app.kitchen.dispatch();app.kitchen.awaitIdle();check("Recovered task cooks after restart",app.state.getOrderService().requireOrder(id).getItem(0).getStatus()==OrderItem.Status.READY);}
    }
    private static void legacyTest(String[] args)throws Exception{
        if(args.length==0)return;
        Path dir=Files.createTempDirectory("rkm-legacy-");Path source=Paths.get(args[0]);
        for(String name:new String[]{"restaurant.dat","staff-accounts.dat"})Files.copy(source.resolve(name),dir.resolve(name));
        byte[] before=Files.readAllBytes(dir.resolve("restaurant.dat"));
        try(RestaurantApplication app=new RestaurantApplication(dir)){
            check("Uploaded legacy saves load without resetting accounts",app.state.getStaff().getCount()>0 && app.state.getOrderService()!=null);
            app.save();
        }
        check("Original legacy restaurant bytes unchanged",Arrays.equals(before,Files.readAllBytes(dir.resolve("restaurant.dat"))));
        check("Legacy accounts unchanged",Arrays.equals(Files.readAllBytes(source.resolve("staff-accounts.dat")),Files.readAllBytes(dir.resolve("staff-accounts.dat"))));
        try(RestaurantApplication app=new RestaurantApplication(dir)){check("Migrated combined snapshot loads again",app.state.getStaff().getCount()>0);}
        Files.write(dir.resolve("review1.dat"),new byte[]{0});boolean failed=false;
        try(RestaurantApplication ignored=new RestaurantApplication(dir)){}catch(java.io.IOException ex){failed=true;}
        check("Corrupt unified save does not fall back to stale legacy",failed);
        Path corruptDir=Files.createTempDirectory("rkm-bad-accounts-");Files.write(corruptDir.resolve("staff-accounts.dat"),new byte[]{0});failed=false;
        try(RestaurantApplication ignored=new RestaurantApplication(corruptDir)){}catch(java.io.IOException ex){failed=true;}
        check("Corrupt accounts do not trigger Admin reset",failed && Files.size(corruptDir.resolve("staff-accounts.dat"))==1);
    }
    private static void saveFailureTest()throws Exception {
        Path dir=Files.createTempDirectory("rkm-save-failure-");
        try(RestaurantApplication app=new RestaurantApplication(dir)){
            app.state.getStaff().addAccount(new StaffAccount(1,"Admin",Role.ADMIN,PASSWORD));login(app.state.getStaff(),1);app.save();
            Path backup=dir.resolve("review1.dat.bak");Files.createDirectory(backup);Files.write(backup.resolve("block"),new byte[]{1});
            boolean failed=false;try{app.save();}catch(java.io.IOException ex){failed=true;}check("Save failure propagated",failed);
            rejects("Application blocks further changes after save failure",()->app.healthy());
            Files.delete(backup.resolve("block"));Files.delete(backup);app.save();app.healthy();check("Explicit save retry recovers",true);
        }
    }
}
