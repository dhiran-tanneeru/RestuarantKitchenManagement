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

    public double calculateSubtotal() {
        double total = 0;

        for (int i = 0; i < itemCount; i++) {
            if (items[i].getStatus()
                    != OrderItem.Status.CANCELLED) {
                total += items[i].getTotalPrice();
            }
        }

        return subtotalMoney().doubleValue();
    }

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
        result.append("\n-----------------------------");

        for (int i = 0; i < itemCount; i++) {
            result.append("\n")
                  .append(i + 1)
                  .append(". ")
                  .append(items[i]);
        }

        result.append("\n-----------------------------");

        result.append(
            String.format(
                "\nSubtotal: Rs. %.2f",
                calculateSubtotal()
            )
        );

        return result.toString();
    }
}

