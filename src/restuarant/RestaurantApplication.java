package restuarant;

import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.model.*;
import restuarant.service.*;
import restuarant.persistence.*;
import restuarant.billing.*;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Shared application lifetime and persistence boundary. Never constructs a second operational state. */
public final class RestaurantApplication implements AutoCloseable {
    public final RestaurantState state;
    public final KitchenService kitchen;
    private final Path path;
    private IOException saveFailure;
    private final java.nio.channels.FileChannel lockChannel;
    private final java.nio.channels.FileLock fileLock;
    public RestaurantApplication(Path directory) throws IOException,ClassNotFoundException {
        Files.createDirectories(directory);
        lockChannel=java.nio.channels.FileChannel.open(directory.resolve("review1.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        java.nio.channels.FileLock acquired;
        try { acquired=lockChannel.tryLock(); }
        catch(RuntimeException ex) { lockChannel.close();throw new IOException("Another instance is using this data folder",ex); }
        if(acquired==null) { lockChannel.close();throw new IOException("Another instance is using this data folder"); }
        fileLock=acquired; path=directory.resolve("review1.dat");
        try {
            if(Files.exists(path)) state=SnapshotStore.load(path);
            else {
                Path legacy=directory.resolve("restaurant.dat"), accounts=directory.resolve("staff-accounts.dat");
                RestaurantState loaded=null; StaffService staff=null;
                if(Files.exists(legacy)) loaded=readLegacy(legacy,RestaurantState.class);
                if(Files.exists(accounts)) staff=readLegacy(accounts,StaffService.class);
                if(loaded!=null && staff==null) throw new IOException("Restaurant data exists but accounts are missing. Restore staff-accounts.dat; no reset performed.");
                if(staff==null) staff=new StaffService();
                if(loaded==null) loaded=new RestaurantState(new MenuService(),new OrderServices(new RestuarantTable[0]),new BillingService(new BigDecimal("5")),new Chef[0],new Manager(1,"Legacy compatibility"),new Cashier(1,"Legacy compatibility"));
                loaded.integrate(staff); state=loaded;
                if(Files.exists(legacy)) state.audit("Imported legacy restaurant and staff files; originals retained unchanged. Account registry is authoritative; legacy employee objects are historical only.");
            }
            long timing=Long.parseLong(System.getProperty("rkm.millisPerMinute","100"));
            kitchen=new KitchenService(state,this::saveUnchecked,timing);
        } catch(IOException|ClassNotFoundException|RuntimeException ex) { fileLock.release();lockChannel.close();throw ex; }
    }
    private static <T> T readLegacy(Path path,Class<T> type) throws IOException,ClassNotFoundException {
        try(ObjectInputStream in=new SnapshotStore.SafeInput(Files.newInputStream(path))) {
            Object o=in.readObject();if(!type.isInstance(o)) throw new IOException("Unexpected legacy format in "+path);return type.cast(o);
        }
    }
    public void healthy() { if(saveFailure!=null) throw new IllegalStateException("Saving failed; retry Save before further changes: "+saveFailure.getMessage()); }
    public void save() throws IOException {
        synchronized(state) {
            try { SnapshotStore.save(path,state);saveFailure=null; }
            catch(IOException ex) { saveFailure=ex;throw ex; }
        }
    }
    private void saveUnchecked() { try { save(); } catch(IOException ex) { throw new IllegalStateException("SAVE FAILED: "+ex.getMessage(),ex); } }
    public void commit(String action) throws IOException {
        state.audit("Employee "+Access.current().getEmployeeId()+" | "+action);save();
    }
    public Order seat(int guests) {
        healthy(); Access.require(Role.ADMIN,Role.MANAGER,Role.WAITER);
        OrderServices orders=state.getOrderService();
        return new DiningSessionService(orders.getTables(),new TableAllocationService(orders.getTables(),state.getStaff()),orders).seatAndCreateOrder(guests);
    }
    public Order seatManual(int number,int guests,int waiter) {
        healthy();Access.require(Role.ADMIN,Role.MANAGER);requireWaiter(waiter);
        OrderServices orders=state.getOrderService();RestuarantTable t=orders.requireTable(number);
        if(orders.findActiveOrderForTable(number)!=null) throw new IllegalStateException("Table has active order");
        t.seatGuests(guests,waiter);
        try { return orders.createDineInOrder(number); } catch(RuntimeException ex) { t.rollbackSeating();throw ex; }
    }
    private void requireWaiter(int id) {
        StaffAccount a=state.getStaff().findAccount(id);
        if(a==null || !a.isActive() || a.getRole()!=Role.WAITER) throw new IllegalArgumentException("Active waiter required");
    }
    public void transfer(int orderId,int targetNumber,String reason) {
        healthy();Access.require(Role.ADMIN,Role.MANAGER); reason(reason);
        OrderServices orders=state.getOrderService();Order o=orders.requireOrder(orderId);
        o.ensureEditable();o.ensureNoCancellation();if(o.getOrderType()!=Order.OrderType.DINE_IN) throw new IllegalStateException("Dine-in order required");
        RestuarantTable old=orders.requireTable(o.getTableNumber()), target=orders.requireTable(targetNumber);
        requireWaiter(old.getWaiterId());
        if(orders.findActiveOrderForTable(targetNumber)!=null) throw new IllegalStateException("Destination has active order");
        target.seatGuests(old.getGuestCount(),old.getWaiterId());
        o.transferTo(targetNumber);target.attachOrder(o);old.releaseForTransfer(o);
        state.audit("Employee "+Access.current().getEmployeeId()+" transferred order "+orderId+" to "+targetNumber+" | "+reason);
    }
    public void reassign(int orderId,int waiter,String reason) {
        healthy();Access.require(Role.ADMIN,Role.MANAGER);reason(reason);requireWaiter(waiter);
        Order o=state.getOrderService().requireOrder(orderId);o.requireOperational();
        if(o.getStatus()!=Order.OrderStatus.OPEN || o.getOrderType()!=Order.OrderType.DINE_IN) throw new IllegalStateException("Open dine-in order required");
        state.getOrderService().requireTable(o.getTableNumber()).reassignWaiter(waiter);o.assignWaiter(waiter);
        state.audit("Employee "+Access.current().getEmployeeId()+" reassigned order "+orderId+" to waiter "+waiter+" | "+reason);
    }
    private void reason(String value) { if(value==null || value.trim().isEmpty()) throw new IllegalArgumentException("Reason required"); }
    public void abandonEmpty(int id,String note) {
        healthy();Access.require(Role.ADMIN,Role.MANAGER);reason(note);
        Order o=state.getOrderService().requireOrder(id);o.abandonEmpty();
        if(o.getOrderType()==Order.OrderType.DINE_IN) state.getOrderService().requireTable(o.getTableNumber()).markForCleaning();
        state.audit("Employee "+Access.current().getEmployeeId()+" abandoned empty order "+id+" | "+note);
    }
    public void collect(int id) {
        healthy();Access.require(Role.ADMIN,Role.MANAGER,Role.CASHIER);
        Order o=state.getOrderService().requireOrder(id);o.requireOperational();Bill bill=state.getBillingService().findBill(id);
        if(o.getOrderType()!=Order.OrderType.TAKEAWAY || o.getStatus()!=Order.OrderStatus.OPEN || bill==null || !bill.isFullyPaid()) throw new IllegalStateException("Open fully paid takeaway required");
        for(int i=0;i<o.getItemCount();i++) if(o.getItem(i).getStatus()!=OrderItem.Status.READY && o.getItem(i).getStatus()!=OrderItem.Status.CANCELLED) throw new IllegalStateException("All dishes must be ready");
        for(int i=0;i<o.getItemCount();i++) if(o.getItem(i).getStatus()==OrderItem.Status.READY) o.getItem(i).markCollected();
        state.getBillingService().closePaidOrder(o,null);
    }
    public DayCloseRecord closeBusinessDay(String explanation){
        synchronized(state){healthy();return state.getBusinessDay().close(state,explanation);}
    }
    public void openBusinessDay(LocalDate next){synchronized(state){healthy();state.getBusinessDay().openNext(next,state);}}
    public String report(){return new ReportingService(state).sales(state.getBusinessDay().getDate());}
    public String report(LocalDate date){return new ReportingService(state).sales(date);}
    public void close() throws IOException {
        kitchen.close();
        try { if(state.getStaff().getCount()>0) save(); }
        finally { Access.logout();fileLock.release();lockChannel.close(); }
    }
}
