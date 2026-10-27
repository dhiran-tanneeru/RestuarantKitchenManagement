
package restuarant.service;
import java.io.Serializable;
import restuarant.model.*;

public class OrderServices implements Serializable{
    private static final long serialVersionUID = 1l;

    private Order[] orders;
    private RestuarantTable[] tables;
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

    public synchronized Order createDineInOrder(int tableNumber) {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.WAITER);

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

        restuarant.auth.Access.activeEmployee(table.getWaiterId(),restuarant.auth.StaffAccount.Role.WAITER);
        checkOrderCapacity();

        Order order = new Order( nextOrderId++, Order.OrderType.DINE_IN, tableNumber);
        orders[orderCount++] = order;
        order.assignWaiter(table.getWaiterId());
        table.attachOrder(order);
        return order;
    }

    public synchronized Order createTakeawayOrder() {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.CASHIER);
        checkOrderCapacity();

        Order order = new Order( nextOrderId++, Order.OrderType.TAKEAWAY, 0);
        orders[orderCount++] = order;
        return order;
    }

    private void checkOrderCapacity() {
        if (orderCount >= orders.length) {
            orders=java.util.Arrays.copyOf(orders, Math.max(1,orders.length*2));
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

    public void addTable(int number, int capacity) {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN);
        if(findTable(number)!=null) throw new IllegalArgumentException("Duplicate table");
        RestuarantTable table=new RestuarantTable(number,capacity);
        tables=java.util.Arrays.copyOf(tables,tables.length+1); tables[tables.length-1]=table;
    }
    public void configureTable(int number,int capacity) { restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN); requireTable(number).setCapacity(capacity); }
    public RestuarantTable requireTable(int number) { RestuarantTable t=findTable(number); if(t==null) throw new IllegalArgumentException("Unknown table"); return t; }
    public Order requireOrder(int id) { Order o=findOrder(id); if(o==null) throw new IllegalArgumentException("Unknown order"); return o; }
    public void serve(int id,int index) {
        Order o=requireOrder(id); restuarant.auth.Access.order(o);
        if(o.getOrderType()!=Order.OrderType.DINE_IN) throw new IllegalStateException("Use takeaway collection");
        o.getItem(index).markServed();
    }
    public void clean(int number) {
        restuarant.auth.StaffAccount a=restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.WAITER);
        RestuarantTable t=requireTable(number);
        if(a.getRole()==restuarant.auth.StaffAccount.Role.WAITER && a.getEmployeeId()!=t.getWaiterId()) throw new SecurityException("Assigned waiter required");
        if(findActiveOrderForTable(number)!=null) throw new IllegalStateException("Table has an active order");
        t.finishCleaning();
    }
    public void reconnect() {
        int max=1000;
        java.util.Set<Integer> ids=new java.util.HashSet<>();
        for(int i=0;i<orderCount;i++) {
            Order o=orders[i]; if(!ids.add(o.getOrderId())) throw new IllegalStateException("Duplicate saved order IDs");
            max=Math.max(max,o.getOrderId());
            if(o.getStatus()==Order.OrderStatus.OPEN && o.getOrderType()==Order.OrderType.DINE_IN) {
                RestuarantTable t=requireTable(o.getTableNumber());
                if(findActiveOrderForTable(o.getTableNumber())!=o || t.getStatus()!=RestuarantTable.Status.OCCUPIED) throw new IllegalStateException("Inconsistent saved table/order");
                t.attachOrder(o); o.assignWaiter(t.getWaiterId());
            }
        }
        nextOrderId=Math.max(nextOrderId,max+1);
    }
    public RestuarantTable[] getTables() {
        return tables.clone();
    }

}
