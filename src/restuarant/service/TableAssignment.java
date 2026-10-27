
package restuarant.service;

public class TableAssignment {

    private final int tableNumber;
    private final int waiterId;
    private final String waiterName;
    private final int guestCount;

    public TableAssignment(int tableNumber,
                           int waiterId,
                           String waiterName,
                           int guestCount) {

        this.tableNumber = tableNumber;
        this.waiterId = waiterId;
        this.waiterName = waiterName;
        this.guestCount = guestCount;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public int getWaiterId() {
        return waiterId;
    }

    public int getGuestCount() {
        return guestCount;
    }

    @Override
    public String toString() {
        return "\nAssignment Successful!"
            + "\nTable: " + tableNumber
            + "\nGuests: " + guestCount
            + "\nWaiter: " + waiterName
            + " (ID: " + waiterId + ")";
    }
}

