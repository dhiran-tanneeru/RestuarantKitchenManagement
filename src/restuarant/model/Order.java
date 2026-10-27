package restuarant.model;
import java.io.Serializable;

public class Order implements Serializable{
    private static final long serialVersionUID = 1L;

    public enum OrderType {
        DINE_IN,
        TAKEAWAY
    }

    public enum OrderStatus {
        OPEN,
        CLOSED
    }

    private final int orderId;
    private final OrderType orderType;
    private int tableNumber;
    private int waiterId;
    private boolean urgent;
    private String priorityReason="";
    private long createdAt=System.currentTimeMillis();
    private transient restuarant.persistence.RestaurantState context;
    private transient restuarant.service.CancellationService cancellationService;
    public long getCreatedAt(){return createdAt;}
    public boolean isUrgent(){return urgent;}
    public void attachContext(restuarant.persistence.RestaurantState state){context=state;cancellationService=state.getCancellations();}
    public void requireOperational(){if(context!=null)context.getBusinessDay().requireOpen();}
    public boolean hasPendingCancellation(){return cancellationService!=null && cancellationService.hasPending(orderId);}
    public boolean isCancellationHeld(int index){return cancellationService!=null && cancellationService.isHeld(orderId,index);}
    public void ensureNoCancellation(){if(hasPendingCancellation())throw new IllegalStateException("Resolve pending cancellation before changing the bill or taking payment");}
    public synchronized void setUrgent(boolean value,String reason){
        restuarant.auth.Access.order(this);ensureEditable();
        if(reason==null || reason.trim().isEmpty())throw new IllegalArgumentException("Priority change reason required");
        urgent=value;priorityReason=reason.trim();
        if(context!=null)context.audit("Employee "+restuarant.auth.Access.current().getEmployeeId()+" priority for order "+orderId+" = "+value+" | "+reason);
    }
    public synchronized void updateItem(int index,int quantity,String instructions){
        restuarant.auth.Access.order(this);ensureEditable();
        if(isCancellationHeld(index))throw new IllegalStateException("Item has a pending cancellation");
        OrderItem item=getItem(index);
        synchronized(item){item.updatePending(quantity,instructions);}
        if(context!=null)context.audit("Employee "+restuarant.auth.Access.current().getEmployeeId()+" updated order "+orderId+" item "+(index+1)+" quantity "+quantity+" instructions "+instructions);
    }
    public synchronized void cancelWhole(){
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER);
        ensureEditable();
        for(int i=0;i<itemCount;i++)if(items[i].getStatus()!=OrderItem.Status.PENDING && items[i].getStatus()!=OrderItem.Status.CANCELLED)throw new IllegalStateException("Whole-order cancellation requires every remaining dish to be pending");
        for(int i=0;i<itemCount;i++)if(items[i].getStatus()==OrderItem.Status.PENDING)items[i].cancel();
        status=OrderStatus.CLOSED;
    }

    private boolean paymentStarted;
    private restuarant.billing.Bill bill;
    public boolean isSettled() { return bill!=null && bill.isFullyPaid(); }
    public void abandonEmpty() {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER);
        ensureEditable(); if(itemCount!=0) throw new IllegalStateException("Only empty orders can be abandoned");status=OrderStatus.CLOSED;
    }
    public int getWaiterId() { return waiterId; }
    public void assignWaiter(int id) { waiterId=id; }
    public boolean hasPaymentStarted() { return paymentStarted; }
    public synchronized void lockForPayment() { paymentStarted=true; }
    public void attachBill(restuarant.billing.Bill value) { if(value==null || value.getOrder()!=this || (bill!=null && bill!=value)) throw new IllegalArgumentException("Conflicting bill"); bill=value; }
    public void transferTo(int table) { if(status!=OrderStatus.OPEN) throw new IllegalStateException("Order closed"); tableNumber=table; }
    public synchronized void ensureEditable() {
        requireOperational();
        if(status!=OrderStatus.OPEN || paymentStarted) throw new IllegalStateException("Order closed or payment started");
    }
    public java.math.BigDecimal subtotalMoney() {
        java.math.BigDecimal sum=java.math.BigDecimal.ZERO;
        for(int i=0;i<itemCount;i++) if(items[i].getStatus()!=OrderItem.Status.CANCELLED) sum=sum.add(items[i].totalMoney());
        return sum.setScale(2,java.math.RoundingMode.HALF_UP);
    }

    private OrderItem[] items;
    private int itemCount;

    private OrderStatus status;

    public Order(int orderId, OrderType orderType,
                 int tableNumber) {

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                "Invalid order ID"
            );
        }

        if (orderType == null) {
            throw new IllegalArgumentException(
                "Order type is required"
            );
        }

        if (orderType == OrderType.DINE_IN
                && tableNumber <= 0) {
            throw new IllegalArgumentException(
                "Dine-in order requires a table"
            );
        }

        if (orderType == OrderType.TAKEAWAY
                && tableNumber != 0) {
            throw new IllegalArgumentException(
                "Takeaway orders must not use tables"
            );
        }

        this.orderId = orderId;
        this.orderType = orderType;
        this.tableNumber = tableNumber;

        this.items = new OrderItem[100];
        this.itemCount = 0;
        this.status = OrderStatus.OPEN;
    }

    public int getOrderId() {
        return orderId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public int getItemCount() {
        return itemCount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public synchronized void addItem(Menuitem menuItem, int quantity,
                        String instructions) {

        ensureEditable();
        if(cancellationService!=null && cancellationService.isWholeHeld(orderId))throw new IllegalStateException("Whole-order cancellation is pending");
        restuarant.auth.Access.order(this);
        if (status != OrderStatus.OPEN) {
            throw new IllegalStateException(
                "Cannot modify a closed order"
            );
        }

        if (itemCount >= items.length) {
            items=java.util.Arrays.copyOf(items, Math.max(1,items.length*2));
        }

        OrderItem item = new OrderItem(
            menuItem, quantity, instructions
        );

        item.attach(this);
        items[itemCount] = item;
        itemCount++;
    }

    public OrderItem getItem(int index) {
        if (index < 0 || index >= itemCount) {
            throw new IllegalArgumentException(
                "Invalid item index"
            );
        }

        return items[index];
    }

    public double calculateSubtotal() { return subtotalMoney().doubleValue(); }

    public synchronized void closeOrder() {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.CASHIER);
        if(bill==null || !bill.isFullyPaid()) throw new IllegalStateException("Bill must be fully settled");
        if (status == OrderStatus.CLOSED) {
            throw new IllegalStateException(
                "Order already closed"
            );
        }

        for (int i = 0; i < itemCount; i++) {
            OrderItem.Status itemStatus =
                items[i].getStatus();

            if (itemStatus != OrderItem.Status.SERVED
                    && itemStatus != OrderItem.Status.COLLECTED
                    && itemStatus != OrderItem.Status.CANCELLED) {

                throw new IllegalStateException(
                    "Order has unfinished items"
                );
            }
        }

        status = OrderStatus.CLOSED;
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject(); for(int i=0;i<itemCount;i++) items[i].attach(this);
    }
    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();

        result.append("\nOrder ID: ").append(orderId);
        result.append("\nType: ").append(orderType);

        if (orderType == OrderType.DINE_IN) {
            result.append("\nTable: ").append(tableNumber);
        }

        result.append("\nStatus: ").append(status);
        result.append("\nPriority: ").append(urgent?"URGENT":"NORMAL").append(" | ").append(priorityReason==null?"":priorityReason);
        result.append("\n-----------------------------");

        for (int i = 0; i < itemCount; i++) {
            result.append("\n")
                  .append(i + 1)
                  .append(". ")
                  .append(items[i]);
        }

        result.append("\n-----------------------------");

        result.append("\nSubtotal: Rs. ").append(subtotalMoney().toPlainString());

        return result.toString();
    }
}

