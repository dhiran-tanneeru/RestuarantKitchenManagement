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
    private java.math.BigDecimal exactPrice;
    private transient Order owner;
    void attach(Order o) { owner=o; }
    public java.math.BigDecimal priceMoney() { return exactPrice==null ? java.math.BigDecimal.valueOf(unitPrice).setScale(2,java.math.RoundingMode.HALF_UP) : exactPrice; }
    public java.math.BigDecimal totalMoney() { return priceMoney().multiply(java.math.BigDecimal.valueOf(quantity)); }
    private long queuedAt=System.currentTimeMillis();
    public long getQueuedAt(){return queuedAt;}
    void updatePending(int count,String notes){
        if(owner==null)throw new IllegalStateException("Unattached dish");
        owner.ensureEditable();
        if(status!=Status.PENDING)throw new IllegalStateException("Only pending dishes can be updated");
        if(count<=0)throw new IllegalArgumentException("Quantity must be positive; request cancellation to remove a line");
        quantity=count;instructions=notes==null?"":notes.trim();
    }
    public void cancel() {
        if(owner==null)throw new IllegalStateException("Unattached dish");
        synchronized(owner){ synchronized(this){
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER);
        if(owner==null) throw new IllegalStateException("Unattached dish");
        owner.ensureEditable();
        if(status!=Status.PENDING) throw new IllegalStateException("Only pending dishes can be cancelled");
        status=Status.CANCELLED;
        }}
    }
    public synchronized void recoverInterrupted() {
        if(status==Status.PREPARING) { status=Status.PENDING; assignedChefId=0; preparationStartedAt=0; preparationFinishedAt=0; }
    }
    private int quantity;
    private final double unitPrice;
    private String instructions;
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

        this.menuItem = new Menuitem(menuitem.getId(),menuitem.getName(),menuitem.getCategory(),menuitem.getPrice(),menuitem.getPreparationTime());
        this.exactPrice=menuitem.priceMoney();
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
        return totalMoney().doubleValue();
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


    public synchronized void markServed() {
        if(owner==null || owner.getOrderType()!=Order.OrderType.DINE_IN) throw new IllegalStateException("Dine-in dish required");
        restuarant.auth.Access.order(owner);
        if (status != Status.READY) {
            throw new IllegalStateException("Item is not ready");
        }
        status = Status.SERVED;
    }

    public synchronized void markCollected() {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.CASHIER);
        if(owner==null || owner.getOrderType()!=Order.OrderType.TAKEAWAY || !owner.isSettled()) throw new IllegalStateException("Fully paid takeaway required");
        if (status != Status.READY) {
            throw new IllegalStateException("Item is not ready");
        }
        status = Status.COLLECTED;
    }

    public String toString() {
        return String.format(
            "%s | Qty: %d | Rs. %s | %s | %s",
            menuItem.getName(),
            quantity,
            totalMoney().toPlainString(),
            status,
            instructions.isEmpty()
                ? "No special instructions" : instructions
        );
    }
}

