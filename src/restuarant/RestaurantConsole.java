package restuarant;

import java.util.Scanner;

import restuarant.model.*;
import restuarant.service.*;

import java.math.BigDecimal;

import restuarant.billing.*;

public class RestaurantConsole {

    private final Scanner scanner;
    private final MenuService menuService;
    private final OrderServices orderService;
    private final RestuarantTable[] tables;
    private final KitchenService kitchenService;
    private final Chef[] chefs;
    private final BillingService billingService;
    private final Manager manager;
    private final Cashier cashier;

    public RestaurantConsole() {
        scanner = new Scanner(System.in);

        menuService = new MenuService();

        tables = new RestuarantTable[20];

        for (int i = 0; i < tables.length; i++) {
            tables[i] = new RestuarantTable(i + 1, 4);
        }

        orderService = new OrderServices(tables);

        kitchenService = new KitchenService();

        chefs = new Chef[2];
        chefs[0] = new Chef(301, "Kiran");
        chefs[1] = new Chef(302, "Ravi");

        billingService = new BillingService( new BigDecimal("5"));

        manager = new Manager(101, "Arjun");
        cashier = new Cashier(401, "Priya");

        loadSampleMenu();
    }

    private void loadSampleMenu() {
        menuService.addMenuItem(new Menuitem( 101, "Chicken Biryani", "Main Course", 250, 25));
        menuService.addMenuItem(new Menuitem( 102, "Paneer Tikka", "Starters", 180, 15));
        menuService.addMenuItem(new Menuitem(103, "Butter Naan", "Breads", 50, 8 ));
        menuService.addMenuItem(new Menuitem( 104, "Fresh Lime Soda", "Beverages", 80, 5 ));
    }

    public void start() {
        int choice;

        do {
            displayMainMenu();
            choice = readInt("Enter choice: ");

            try {
                switch (choice) {
                    case 1:
                        menuService.displayMenu();
                        break;

                    case 2:
                        viewTables();
                        break;

                    case 3:
                        seatCustomers();
                        break;

                    case 4:
                        createDineInOrder();
                        break;

                    case 5:
                        createTakeawayOrder();
                        break;

                    case 6:
                        addItemsToOrder();
                        break;

                    case 7:
                        orderService.displayActiveOrders();
                        break;
                    case 8:
                        viewKitchenQueue();
                        break;

                    case 9:
                        processKitchenOrders();
                        break;

                    case 10:
                        serveFood();
                        break;
                    
                    case 11:
                        generateBill();
                        break;

                    case 12:
                        applyPredefinedDiscount();
                        break;

                    case 13:
                        applySpecialDiscount();
                        break;

                    case 14:
                        recordPayment();
                        break;

                    case 15:
                        closePaidOrder();
                        break;

                    case 16:
                        System.out.println("Exiting restaurant system...");
                        break;


                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println( "Operation failed: " + e.getMessage());
            }

        } while (choice != 16);
    }

    
    private void displayMainMenu() {

        System.out.println(
            "\n====== RESTAURANT MANAGEMENT ======"
        );

        System.out.println("1. View Menu");
        System.out.println("2. View Tables");
        System.out.println("3. Seat Customers");
        System.out.println("4. Create Dine-In Order");
        System.out.println("5. Create Takeaway Order");
        System.out.println("6. Add Items to Existing Order");
        System.out.println("7. View Active Orders");
        System.out.println("8. View Kitchen Queue");
        System.out.println("9. Process Pending Kitchen Orders");
        System.out.println("10. Mark Food Served / Collected");
        System.out.println("11. Generate / View Bill");
        System.out.println("12. Apply Predefined Discount");
        System.out.println("13. Apply Special Discount");
        System.out.println("14. Record Payment");
        System.out.println("15. Close Paid Order");
        System.out.println("16. Exit");
    }



    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();

            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println( "Please enter a valid integer.");
            }
        }
    }

    private void viewTables() {
        System.out.println("\n====== TABLES ======");

        for (RestuarantTable table : tables) {
            System.out.println(table);
        }
    }

    private void seatCustomers() {
        int number = readInt("Table number: ");
        int guests = readInt("Number of guests: ");
        int waiterId = readInt("Waiter ID: ");

        RestuarantTable table =
            orderService.findTable(number);

        if (table == null) {
            throw new IllegalArgumentException( "Table does not exist");
        }

        table.seatGuests(guests, waiterId);
        System.out.println( "Customers seated successfully.");
    }

    private void createDineInOrder() {
        int number = readInt("Table number: ");
        Order order = orderService.createDineInOrder(number);
        System.out.println( "Dine-in order #" + order.getOrderId() + " created." );
    }

    private void createTakeawayOrder() {
        Order order = orderService.createTakeawayOrder();
        System.out.println("Takeaway order #" + order.getOrderId()+ " created.");
    }

    private void addItemsToOrder() {
        int orderId = readInt("Order ID: ");
        int itemId = readInt("Menu item ID: ");
        int quantity = readInt("Quantity: ");
        System.out.print("Special instructions: ");
        String instructions = scanner.nextLine();

        Order existingOrder = orderService.findOrder(orderId);
        if (existingOrder == null) {
            throw new IllegalArgumentException( "Order not found" );
        }
        if (billingService.hasPayments(orderId)) {
            throw new IllegalStateException("Cannot add dishes after payment begins");
        }

        Menuitem item = menuService.findItem(itemId);

        if (item == null) {
            throw new IllegalArgumentException( "Menu item not found");
        }

        orderService.addItemToOrder( orderId, item, quantity, instructions);
        System.out.println( "Item added to order successfully.");
    }

    
    private void viewKitchenQueue() {

        kitchenService.loadPendingOrders(orderService);
        kitchenService.displayQueue();
    }

    private void processKitchenOrders() {

        kitchenService.loadPendingOrders(orderService);

        if (kitchenService.getTaskCount() == 0) {
            System.out.println("No pending kitchen orders.");
            return;
        }

        System.out.println("\nStarting kitchen workers...");

        Thread[] workers = new Thread[chefs.length];
        int started = 0;

        for (int i = 0; i < chefs.length; i++) {

            if (!chefs[i].isActive()) {
                continue;
            }

            ChefWorker worker = new ChefWorker( chefs[i], kitchenService );

            workers[started] = new Thread( worker, "Chef-" + chefs[i].getEmployeeId());

            workers[started].start();
            started++;
        }

        if (started == 0) {
            System.out.println("No active chefs available.");
            return;
        }

        boolean interrupted = false;

        for (int i = 0; i < started; i++) {
            boolean joined = false;

            while (!joined) {
                try {
                    workers[i].join();
                    joined = true;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        System.out.println( "\nKitchen processing finished.");
    }

    private void serveFood() {

        int orderId = readInt("Order ID: ");
        int itemNumber = readInt("Order item number: ");

        Order order = orderService.findOrder(orderId);

        if (order == null) {
            throw new IllegalArgumentException( "Order not found");
        }

        OrderItem item = order.getItem(itemNumber - 1);

        if (order.getOrderType() == Order.OrderType.DINE_IN) {
            item.markServed();
            System.out.println("Dish marked as served.");
        } else {
            item.markCollected();
            System.out.println("Takeaway dish marked as collected.");
        }
    }

    
    private void generateBill() {

        int orderId = readInt("Order ID: ");

        Order order = orderService.findOrder(orderId);

        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        Bill bill = billingService.generateBill(order);

        System.out.println(bill);
    }

    private void applyPredefinedDiscount() {

        int orderId = readInt("Order ID: ");

        billingService.applyPredefinedDiscount(orderId, cashier);

        System.out.println("Predefined 10% discount applied.");
    }

    private void applySpecialDiscount() {

        int orderId = readInt("Order ID: ");
        int percentage = readInt("Discount percentage: ");

        billingService.applySpecialDiscount(
            orderId,
            BigDecimal.valueOf(percentage),
            manager
        );

        System.out.println("Special discount applied.");
    }

    private void recordPayment() {

        int orderId = readInt("Order ID: ");

        System.out.println("1. Cash");
        System.out.println("2. UPI");
        System.out.println("3. Card");

        int choice = readInt("Payment method: ");

        Payment.Method method;

        switch (choice) {
            case 1:
                method = Payment.Method.CASH;
                break;
            case 2:
                method = Payment.Method.UPI;
                break;
            case 3:
                method = Payment.Method.CARD;
                break;
            default:
                throw new IllegalArgumentException("Invalid payment method");
        }

        System.out.print("Amount: ");
        BigDecimal amount;

        try {
            amount = new BigDecimal(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException( "Invalid payment amount");
        }

        Payment payment = billingService.recordPayment( orderId, method, amount, cashier);

        System.out.println("Payment recorded:");
        System.out.println(payment);

        Bill bill = billingService.findBill(orderId);
        System.out.println("Remaining: Rs. " + bill.getBalance());
    }

    private void closePaidOrder() {

        int orderId = readInt("Order ID: ");

        Order order = orderService.findOrder(orderId);

        if (order == null) {
            throw new IllegalArgumentException( "Order not found");
        }

        RestuarantTable table = null;

        if (order.getOrderType() == Order.OrderType.DINE_IN) {
            table = orderService.findTable( order.getTableNumber() );
        }

        billingService.closePaidOrder(order, table);

        System.out.println( "Order closed successfully.");

        if (table != null) {
            System.out.println( "Table is now awaiting cleaning.");
        }
    }


}
