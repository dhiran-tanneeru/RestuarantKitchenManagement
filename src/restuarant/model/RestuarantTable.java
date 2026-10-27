package restuarant.model;
import java.io.Serializable;

public class RestuarantTable implements Serializable{
    private static final long serialVersionUID = 1L;
    public enum Status{
        AVAILABLE,
        OCCUPIED,
        CLEANING
    }
    private final int TableNumber;
    private int capacity;
    private Order activeOrder;
    public void attachOrder(Order order) { activeOrder=order; }
    private void noActiveOrder() { if(activeOrder!=null && activeOrder.getStatus()==Order.OrderStatus.OPEN) throw new IllegalStateException("Table still has an active order"); }
    public void setCapacity(int value) {
        if(value<=0 || status!=Status.AVAILABLE) throw new IllegalStateException("Positive capacity and available table required");
        noActiveOrder(); capacity=value;
    }
    public void reassignWaiter(int id) { if(id<=0 || status!=Status.OCCUPIED) throw new IllegalStateException("Occupied table and valid waiter required"); waiterId=id; }
    public void releaseForTransfer(Order order) { if(activeOrder!=order || order.getTableNumber()==TableNumber) throw new IllegalStateException("Order has not moved"); activeOrder=null; status=Status.CLEANING; }

    private int GuestCount;
    private int waiterId;
    private Status status;

    public RestuarantTable(int TableNumber,int capacity){
        if(TableNumber <= 0 || capacity <= 0){
            throw new IllegalArgumentException("Invalid table number or capacity");
        }
        this.TableNumber = TableNumber;
        this.capacity = capacity;
        this.GuestCount = 0;
        this.waiterId = 0;
        this.status = Status.AVAILABLE;
    }

    public int getTableNumber(){
        return TableNumber;
    }

    public int getCapacity(){
        return capacity;
    }

    public int getWaiterId(){
        return waiterId;
    }

    public int getGuestCount(){
        return GuestCount;
    }

    public Status getStatus(){
        return status;
    }

    public synchronized void seatGuests(int guests, int waiterId){
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.WAITER);
        restuarant.auth.Access.activeEmployee(waiterId,restuarant.auth.StaffAccount.Role.WAITER);
        noActiveOrder();
        if(status != Status.AVAILABLE){
            throw new IllegalStateException("Table is not available");
        }
        if(guests <= 0 || guests > capacity){
            throw new IllegalArgumentException("Invalid number of guests");
        }
        if(waiterId <= 0){
            throw new IllegalArgumentException("Invalid waiter ID");
        }
        this.GuestCount = guests;
        this.waiterId = waiterId;
        this.status = Status.OCCUPIED;
    }

    public void markForCleaning(){
        noActiveOrder();
        if(status != Status.OCCUPIED){
            throw new IllegalArgumentException("Table is not occupied");
        }
        status = Status.CLEANING;
    }

    public void finishCleaning(){
        noActiveOrder();
        if(status != Status.CLEANING){
            throw new IllegalArgumentException("Table is not being cleaned");
        }
        GuestCount = 0;
        waiterId = 0;
        status = Status.AVAILABLE;
    }

    public String toString(){
        return String.format( "Table %d | Capacity: %d | Guests: %d | Waiter ID: %d | Status: %s", TableNumber, capacity, GuestCount, waiterId, status);
    }

    public void rollbackSeating() {
        noActiveOrder();

        if (status != Status.OCCUPIED) {
            throw new IllegalStateException(
                "Only occupied tables can be rolled back"
            );
        }

        status = Status.AVAILABLE;
        GuestCount = 0;
        waiterId = 0;
    }

}