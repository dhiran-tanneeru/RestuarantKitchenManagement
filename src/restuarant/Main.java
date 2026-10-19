
package restuarant;

import restuarant.model.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("RESTAURANT MANAGEMENT SYSTEM");
        System.out.println("============================");

        
        Menuitem biryani = new Menuitem(101, "Chicken Biryani", "Main Course", 250, 25);

        Menuitem paneer = new Menuitem(102, "Paneer Tikka", "Starters", 180, 15);

        System.out.println("\nMENU:");
        System.out.println(biryani);
        System.out.println(paneer);

        
        RestuarantTable table = new RestuarantTable(7, 4);

        System.out.println("\nINITIAL TABLE:");
        System.out.println(table);

        table.seatGuests(4, 201);

        System.out.println("\nAFTER SEATING:");
        System.out.println(table);

        try {
            table.seatGuests(2, 202);
        } catch (IllegalStateException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        table.markForCleaning();
        table.finishCleaning();

        System.out.println("\nAFTER CLEANING:");
        System.out.println(table);

        
        System.out.println("\nSTAFF MANAGEMENT");
        System.out.println("================");

        Manager manager = new Manager(101, "Arjun");
        Waiter waiter = new Waiter(201, "Rahul");
        Chef chef = new Chef(301, "Kiran");
        Cashier cashier = new Cashier(401, "Priya");

        System.out.println(manager);
        System.out.println(waiter);
        System.out.println(chef);
        System.out.println(cashier);

        
        System.out.println("\nEMPLOYEE DUTIES:");

        Employee[] employees = {
            manager, waiter, chef, cashier
        };

        for (Employee employee : employees) {
            employee.performDuty();
        }

        
        System.out.println("\nROLE-SPECIFIC OPERATIONS:");

        waiter.takeOrder(7);
        chef.prepareDish("Chicken Biryani");
        cashier.generateBill(1001);
        manager.approveCancellation(1001);

        
        System.out.println("\nEXCEPTION HANDLING TEST:");

        try {
            Employee invalidEmployee = new Waiter(0, "Test");
            System.out.println(invalidEmployee);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        
        System.out.println("\nORDER MANAGEMENT");
        System.out.println("================");

        Order order = new Order(
            1001, Order.OrderType.DINE_IN, 7
        );

        order.addItem(biryani, 2, "Less spicy");
        order.addItem(paneer, 1, "No onions");

        System.out.println(order);

        order.addItem(biryani, 1, "Extra spicy");

        System.out.println("\nAFTER ADDITIONAL ORDER:");
        System.out.println(order);

        OrderItem firstItem = order.getItem(0);

        firstItem.startPreparing();
        firstItem.markReady();
        firstItem.markServed();

        System.out.println("\nAFTER SERVING FIRST ITEM:");
        System.out.println(order);

        try {
            order.addItem(paneer, -2, "");
        } catch (IllegalArgumentException e) {
            System.out.println(
                "\nExpected error: " + e.getMessage()
            );
        }

        Order takeaway = new Order(
            1002, Order.OrderType.TAKEAWAY, 0
        );

        takeaway.addItem(biryani, 1, "Extra raita");

        System.out.println("\nTAKEAWAY ORDER:");
        System.out.println(takeaway);

    }
}
