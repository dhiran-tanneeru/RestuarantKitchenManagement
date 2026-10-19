package restuarant.model;

public class OrderItem {

    public enum Status {
        PENDING,
        PREPARING,
        READY,
        SERVED,
        COLLECTED,
        CANCELLED
    }

    private final Menuitem menuItem;
    private final int quantity;
    private final double unitPrice;
    private final String instructions;

    private Status status;

    public OrderItem(Menuitem menuitem, int quantity,
                     String instructions) {

        if (menuitem == null) {
            throw new IllegalArgumentException("Menu item cannot be null");
        }

        if (!menuitem.isAvailable()) {
            throw new IllegalStateException("Dish is currently unavailable");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.menuItem = menuitem;
        this.quantity = quantity;
        this.unitPrice = menuItem.getPrice();
        this.instructions = instructions == null? "" : instructions.trim();

        this.status = Status.PENDING;
    }

    public Menuitem getMenuItem() {
        return menuItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public String getInstructions() {
        return instructions;
    }

    public Status getStatus() {
        return status;
    }

    public double getTotalPrice() {
        return unitPrice * quantity;
    }

    public void startPreparing() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Item is not pending");
        }
        status = Status.PREPARING;
    }

    public void markReady() {
        if (status != Status.PREPARING) {
            throw new IllegalStateException("Item is not being prepared");
        }
        status = Status.READY;
    }

    public void markServed() {
        if (status != Status.READY) {
            throw new IllegalStateException("Item is not ready");
        }
        status = Status.SERVED;
    }

    public void markCollected() {
        if (status != Status.READY) {
            throw new IllegalStateException("Item is not ready");
        }
        status = Status.COLLECTED;
    }

    public String toString() {
        return String.format(
            "%s | Qty: %d | Rs. %.2f | %s | %s",
            menuItem.getName(),
            quantity,
            getTotalPrice(),
            status,
            instructions.isEmpty()
                ? "No special instructions" : instructions
        );
    }
}

