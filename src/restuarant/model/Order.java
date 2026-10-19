package restuarant.model;

public class Order {

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
    private final int tableNumber;

    private final OrderItem[] items;
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

    public void addItem(Menuitem menuItem, int quantity,
                        String instructions) {

        if (status != OrderStatus.OPEN) {
            throw new IllegalStateException(
                "Cannot modify a closed order"
            );
        }

        if (itemCount >= items.length) {
            throw new IllegalStateException(
                "Order item limit reached"
            );
        }

        OrderItem item = new OrderItem(
            menuItem, quantity, instructions
        );

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

        return total;
    }

    public void closeOrder() {
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

