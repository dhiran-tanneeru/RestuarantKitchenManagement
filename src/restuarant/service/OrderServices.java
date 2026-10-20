
package restuarant.service;
import java.io.Serializable;
import restuarant.model.*;

public class OrderServices implements Serializable{
    private static final long serialVersionUID = 1l;

    private final Order[] orders;
    private final RestuarantTable[] tables;
    private int orderCount;
    private int nextOrderId;

    public OrderServices(RestuarantTable[] tables) {
        if (tables == null) {
            throw new IllegalArgumentException( "Tables cannot be null");
        }

        this.tables = tables;
        this.orders = new Order[500];
        this.orderCount = 0;
        this.nextOrderId = 1001;
    }

    public RestuarantTable findTable(int number) {
        for (RestuarantTable table : tables) {
            if (table != null && table.getTableNumber() == number) {
                return table;
            }
        }
        return null;
    }

    public Order findOrder(int orderId) {
        for (int i = 0; i < orderCount; i++) {
            if (orders[i].getOrderId() == orderId) {
                return orders[i];
            }
        }
        return null;
    }

    public Order findActiveOrderForTable(int tableNumber) {
        for (int i = 0; i < orderCount; i++) {
            Order order = orders[i];

            if (order.getOrderType()== Order.OrderType.DINE_IN && order.getTableNumber() == tableNumber && order.getStatus() == Order.OrderStatus.OPEN) {
                return order;
            }
        }
        return null;
    }

    public Order createDineInOrder(int tableNumber) {

        RestuarantTable table = findTable(tableNumber);

        if (table == null) {
            throw new IllegalArgumentException("Table not found");
        }

        if (table.getStatus()
                != RestuarantTable.Status.OCCUPIED) {
            throw new IllegalStateException("Seat customers before creating an order");
        }

        if (findActiveOrderForTable(tableNumber) != null) {
            throw new IllegalStateException("Table already has an active order");
        }

        checkOrderCapacity();

        Order order = new Order( nextOrderId++, Order.OrderType.DINE_IN, tableNumber);
        orders[orderCount++] = order;
        return order;
    }

    public Order createTakeawayOrder() {
        checkOrderCapacity();

        Order order = new Order( nextOrderId++, Order.OrderType.TAKEAWAY, 0);
        orders[orderCount++] = order;
        return order;
    }

    private void checkOrderCapacity() {
        if (orderCount >= orders.length) {
            throw new IllegalStateException("Order storage is full");
        }
    }

    public void addItemToOrder(
        int orderId,
        Menuitem menuItem,
        int quantity,
        String instructions
    ) {
        Order order = findOrder(orderId);

        if (order == null) {
            throw new IllegalArgumentException( "Order not found");
        }

        order.addItem(menuItem, quantity, instructions);
    }

    public void displayActiveOrders() {
        System.out.println("\n====== ACTIVE ORDERS ======");

        boolean found = false;

        for (int i = 0; i < orderCount; i++) {
            if (orders[i].getStatus() == Order.OrderStatus.OPEN) { System.out.println(orders[i]);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No active orders.");
        }
    }

    
    public int getOrderCount() {
        return orderCount;
    }

    public Order getOrderAt(int index) {
        if (index < 0 || index >= orderCount) {
            throw new IllegalArgumentException( "Invalid order index");
        }
        return orders[index];
    }

    public RestuarantTable[] getTables() {
        return tables.clone();
    }

}
