package restuarant.model;

public class RestuarantTable{
    public enum Status{
        AVAILABLE,
        OCCUPIED,
        CLEANING
    }
    private final int TableNumber;
    private final int capacity;
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

    public void seatGuests(int guests, int waiterId){
        if(status != Status.AVAILABLE){
            throw new IllegalArgumentException("Table is not available");
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
        if(status != Status.OCCUPIED){
            throw new IllegalArgumentException("Table is not occupied");
        }
        status = Status.CLEANING;
    }

    public void finishCleaning(){
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

}