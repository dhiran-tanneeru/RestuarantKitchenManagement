package restuarant.model;
import java.io.Serializable;

public class OrderItem implements Serializable{
    private static final long serialVersionUID = 1L;

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
    private int assignedChefId = 0;
    private long preparationStartedAt = 0;
    private long preparationFinishedAt = 0;

    private volatile Status status;

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


    
    public int getAssignedChefId() {
        return assignedChefId;
    }

    public long getPreparationStartedAt() {
        return preparationStartedAt;
    }

    public long getPreparationFinishedAt() {
        return preparationFinishedAt;
    }

    public long getActualPreparationTime() {
        if (preparationStartedAt == 0 || preparationFinishedAt == 0) {
            return 0;
        }
        return preparationFinishedAt - preparationStartedAt;
    }

    
    public synchronized void startPreparing(int chefId) {
        if (status != Status.PENDING) {
            throw new IllegalStateException( "Dish is not pending");
        }
        if (chefId <= 0) {
            throw new IllegalArgumentException( "Invalid chef ID");
        }

        assignedChefId = chefId;
        preparationStartedAt = System.currentTimeMillis();
        status = Status.PREPARING;
    }
    
    public synchronized void markReady() {
        if (status != Status.PREPARING) {
            throw new IllegalStateException("Dish is not being prepared");
        }

        preparationFinishedAt = System.currentTimeMillis();
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

