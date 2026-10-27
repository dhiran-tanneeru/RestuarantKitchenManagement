package restuarant;
import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.auth.Access;
import restuarant.model.*;
import restuarant.service.*;
import restuarant.billing.*;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;

/** Original login input handling, integrated with every role and a shared operational console. */
public class LoginDemo {
    private final Console console=System.console();
    private final BufferedReader input=new BufferedReader(new InputStreamReader(System.in));
    private boolean passwordVisibilityWarningShown;
    private RestaurantApplication app;
    private StaffService staff;
    private static final class EndOfInput extends RuntimeException { private static final long serialVersionUID=1L; }
    private String readLine(String prompt) {
        if (console != null) {
            return console.readLine("%s", prompt);
        }

        System.out.print(prompt);

        try {
            return input.readLine();
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to read input", e
            );
        }
    }

    private String readRequiredLine(String prompt) {
        String value = readLine(prompt);
        if (value == null) {
            throw new EndOfInput();
        }
        return value;
    }

    private int readInt(String prompt) {
        while (true) {
            String value = readRequiredLine(prompt);

            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.out.println(
                    "Please enter a valid number."
                );
            }
        }
    }


    private char[] readPassword(String prompt) {

        if (console != null) {
            char[] password = console.readPassword(
                "%s", prompt
            );

            if (password == null) {
                throw new EndOfInput();
            }

            return password;
        }

        if (!passwordVisibilityWarningShown) {
            System.err.println(
                "Warning: password input is visible because "
                + "a secure system console is unavailable."
            );
            passwordVisibilityWarningShown = true;
        }

        String value = readRequiredLine(prompt);
        return value.toCharArray();
    }

    private char[] readConfirmedPassword(
            String passwordPrompt,
            String confirmationPrompt) {

        while (true) {
            char[] password = readPassword(passwordPrompt);
            char[] confirmation = null;
            boolean matches = false;

            try {
                confirmation = readPassword(confirmationPrompt);

                if (Arrays.equals(password, confirmation)) {
                    matches = true;
                    return password;
                }

                System.out.println(
                    "Passwords do not match. Please try again."
                );
            } finally {
                if (confirmation != null) {
                    Arrays.fill(confirmation, '\0');
                }
                if (!matches) {
                    Arrays.fill(password, '\0');
                }
            }
        }
    }


    public void start() throws IOException,ClassNotFoundException {
        try(RestaurantApplication application=new RestaurantApplication(Paths.get(System.getProperty("rkm.dataDir","data")))) {
            app=application;staff=app.state.getStaff();
            try {
                if(staff.getCount()==0) setupAdmin();
                while(true) {
                    System.out.println("\n===== RESTAURANT STAFF LOGIN =====\n1. Login\n0. Exit");
                    int choice=readInt("Choice: ");if(choice==0) return;if(choice!=1) continue;
                    int id=readInt("Employee ID: ");char[] password=readPassword("Password: ");
                    try {
                        StaffAccount account=staff.login(id,password);
                        System.out.println("Welcome, "+account.getName());
                        app.kitchen.dispatch();
                        dashboard();
                    } catch(SecurityException ex) { System.out.println(ex.getMessage()); }
                    finally { Arrays.fill(password,'\0');Access.logout(); }
                }
            } catch(EndOfInput ex) { System.out.println("Input ended; saving and exiting."); }
        }
    }
    private void setupAdmin() throws IOException {
        while(staff.getCount()==0) {
            System.out.println("FIRST-TIME ADMIN SETUP (ID 1)");
            String name=readRequiredLine("Administrator name: ");
            char[] password=readConfirmedPassword("Create password: ","Confirm password: ");
            try { staff.addAccount(new StaffAccount(1,name,Role.ADMIN,password));app.save(); }
            catch(IllegalArgumentException ex) { System.out.println(ex.getMessage()); }
            finally { Arrays.fill(password,'\0'); }
        }
    }
    private void dashboard() throws IOException {
        while(true) {
            StaffAccount a=Access.current();Role role=a.getRole();
            System.out.println("\n===== "+role+" DASHBOARD =====");
            System.out.println("1. Menu  2. Tables / assignments  3. Orders  4. Kitchen");
            if(role==Role.ADMIN) System.out.println("5. Employees  6. Configure menu  7. Configure tables");
            if(role==Role.ADMIN || role==Role.MANAGER || role==Role.WAITER) System.out.println("8. Automatic seating  9. Add dishes  10. Serve dish  11. Request / view bill  12. Request cancellation  13. Finish cleaning");
            if(role==Role.ADMIN || role==Role.MANAGER) System.out.println("14. Review cancellations  15. Transfer table  16. Reassign waiter  17. Discount  18. Reports  19. Operational history  20. Manual seating  29. Abandon empty order");
            if(role==Role.ADMIN || role==Role.CASHIER) System.out.println("21. Record payment (repeat for split / partial payments)");
            if(role==Role.ADMIN || role==Role.CASHIER || role==Role.MANAGER) System.out.println("22. Close paid dine-in order  23. Takeaway workspace  24. View bill / reports");
            if(role==Role.ADMIN || role==Role.MANAGER || role==Role.CHEF) System.out.println("25. Run automatic kitchen  26. Chef: claim next dish  27. Chef: mark assigned dish ready");
            System.out.println("28. Save / retry save  0. Logout");
            int choice=readInt("Choice: ");if(choice==0) return;
            try {
                if(choice==5) { employees();continue; }
                if(choice==23) { takeaway();continue; }
                // Collect arguments before taking the state monitor: background cooking never waits on user input.
                Runnable action=action(choice);
                synchronized(app.state) { app.healthy();action.run();app.commit("Dashboard action "+choice); }
                app.kitchen.dispatch();
            } catch(EndOfInput ex) { throw ex; }
            catch(RuntimeException ex) { System.out.println("Operation failed: "+ex.getMessage()); }
            catch(IOException ex) { System.err.println("SAVE FAILED. Changes remain in memory. Choose Save to retry: "+ex.getMessage()); }
        }
    }
    private Runnable action(int choice) throws IOException {
        OrderServices orders=app.state.getOrderService();BillingService billing=app.state.getBillingService();
        switch(choice) {
            case 1:return ()->app.state.getMenuService().displayMenu();
            case 2:return ()->{ for(RestuarantTable t:orders.getTables()) if(t!=null) System.out.println(t);new TableAllocationService(orders.getTables(),staff).displayWaiterWorkload(); };
            case 3:return this::showOrders;
            case 4:return ()->app.kitchen.displayQueue();
            case 6:{ Access.require(Role.ADMIN);int id=readInt("Dish ID (existing updates): ");String name=readRequiredLine("Name: "), category=readRequiredLine("Category: ");BigDecimal price=money("Price: ");int minutes=readInt("Menu preparation minutes: ");boolean available=yes("Available (yes/no): ");return ()->{
                MenuService menu=app.state.getMenuService();
                if(menu.findItem(id)==null) { Menuitem item=new Menuitem(id,name,category,price.doubleValue(),minutes);item.setPrice(price);item.setAvailable(available);menu.addMenuItem(item); }
                else menu.updateItem(id,name,category,price,minutes,available);
            }; }
            case 7:{ Access.require(Role.ADMIN);int id=readInt("Table number: "),capacity=readInt("Capacity: ");return ()->{ if(orders.findTable(id)==null) orders.addTable(id,capacity);else orders.configureTable(id,capacity); }; }
            case 8:{ int guests=readInt("Guests: ");return ()->System.out.println(app.seat(guests)); }
            case 9:return addDish(false);
            case 10:{ int id=readInt("Order ID: "),index=readInt("Item number (1-based): ")-1;return ()->orders.serve(id,index); }
            case 11:{ int id=readInt("Order ID: ");return ()->System.out.println(billing.generateBill(orders.requireOrder(id))); }
            case 12:return cancel(false);
            case 13:{ int table=readInt("Cleaned table number: ");return ()->orders.clean(table); }
            case 14:{ Access.require(Role.ADMIN,Role.MANAGER);for(CancellationRequest request:app.state.getCancellations().history()) System.out.println(request);int id=readInt("Request ID (0 back): ");if(id==0)return ()->{};boolean approve=yes("Approve (yes/no): ");String reason=readRequiredLine("Decision reason: ");return ()->app.state.getCancellations().decide(id,approve,reason,orders); }
            case 15:{ int id=readInt("Order ID: "),target=readInt("Destination table: ");String reason=readRequiredLine("Transfer reason: ");return ()->app.transfer(id,target,reason); }
            case 16:{ int id=readInt("Order ID: "),waiter=readInt("New waiter ID: ");String reason=readRequiredLine("Override reason: ");return ()->app.reassign(id,waiter,reason); }
            case 17:{ int id=readInt("Order ID: ");BigDecimal rate=money("Discount percent 0..100: ");return ()->{billing.discount(id,rate);app.state.audit("Employee "+Access.current().getEmployeeId()+" discount "+rate+"% on order "+id);}; }
            case 18:return ()->System.out.println(app.report());
            case 19:return ()->{Access.require(Role.ADMIN,Role.MANAGER);for(String line:app.state.getHistory())System.out.println(line);for(CancellationRequest request:app.state.getCancellations().history())System.out.println(request);};
            case 20:{int table=readInt("Table: "),guests=readInt("Guests: "),waiter=readInt("Waiter ID: ");return ()->System.out.println(app.seatManual(table,guests,waiter));}
            case 21:return payment(false);
            case 22:{ int id=readInt("Order ID: ");return ()->{Order o=orders.requireOrder(id);if(o.getOrderType()!=Order.OrderType.DINE_IN)throw new IllegalArgumentException("Use takeaway collection");billing.closePaidOrder(o,orders.requireTable(o.getTableNumber()));}; }
            case 24:{int id=readInt("Order ID (0 for reports): ");return ()->{if(id==0)System.out.println(app.report());else System.out.println(billing.generateBill(orders.requireOrder(id)));};}
            case 25:return ()->app.kitchen.dispatch();
            case 26:return ()->{OrderItem item=app.kitchen.claimNextTask(Access.current().getEmployeeId());System.out.println(item==null?"No eligible task available":item);};
            case 27:{int id=readInt("Order ID: "),index=readInt("Item number: ")-1;return ()->app.kitchen.markReady(id,index);}
            case 29:{int id=readInt("Empty order ID: ");String reason=readRequiredLine("Reason: ");return ()->app.abandonEmpty(id,reason);}
            case 28:app.save();System.out.println("Saved.");return ()->{};
            default:throw new IllegalArgumentException("Invalid option");
        }
    }
    private void showOrders() {
        StaffAccount a=Access.current();OrderServices orders=app.state.getOrderService();
        for(int i=0;i<orders.getOrderCount();i++) {
            Order o=orders.getOrderAt(i);
            if(a.getRole()!=Role.WAITER || o.getWaiterId()==a.getEmployeeId())System.out.println(o);
        }
    }
    private Runnable addDish(boolean takeaway) {
        int id=readInt("Order ID: "),dish=readInt("Dish ID: "),quantity=readInt("Quantity: ");String note=readRequiredLine("Special instructions: ");
        return ()->{if(takeaway)requireTakeaway(id);app.state.getOrderService().addItemToOrder(id,app.state.getMenuService().findItem(dish),quantity,note);};
    }
    private Runnable cancel(boolean takeaway) {
        int id=readInt("Order ID: "),index=readInt("Item number: ")-1;String reason=readRequiredLine("Reason: ");
        return ()->{if(takeaway)requireTakeaway(id);System.out.println(app.state.getCancellations().request(app.state.getOrderService().requireOrder(id),index,reason));};
    }
    private Runnable payment(boolean takeaway) {
        int id=readInt("Order ID: ");Payment.Method method=Payment.Method.valueOf(readRequiredLine("CASH / UPI / CARD: ").trim().toUpperCase(Locale.ROOT));BigDecimal amount=money("Amount: ");
        return ()->{if(takeaway)requireTakeaway(id);System.out.println(app.state.getBillingService().pay(id,method,amount));System.out.println(app.state.getBillingService().findBill(id));};
    }
    private void requireTakeaway(int id) {if(app.state.getOrderService().requireOrder(id).getOrderType()!=Order.OrderType.TAKEAWAY)throw new IllegalArgumentException("Takeaway order required");}
    private void takeaway() throws IOException {
        Access.require(Role.ADMIN,Role.MANAGER,Role.CASHIER);
        while(true) {
            System.out.println("\n===== TAKEAWAY WORKSPACE =====\n1. Create  2. Add dishes  3. Track active / completed  4. Bill  5. Pay  6. Collect and close  7. Request cancellation  0. Back");
            int choice=readInt("Choice: ");if(choice==0)return;
            try {
                Runnable action;
                switch(choice) {
                    case 1:action=()->System.out.println("Pickup token / order ID: "+app.state.getOrderService().createTakeawayOrder().getOrderId());break;
                    case 2:action=addDish(true);break;
                    case 3:action=()->{for(int i=0;i<app.state.getOrderService().getOrderCount();i++){Order o=app.state.getOrderService().getOrderAt(i);if(o.getOrderType()==Order.OrderType.TAKEAWAY){boolean ready=o.getItemCount()>0;for(int j=0;j<o.getItemCount();j++)if(o.getItem(j).getStatus()!=OrderItem.Status.READY && o.getItem(j).getStatus()!=OrderItem.Status.CANCELLED)ready=false;System.out.println(o+"\nReady for collection: "+ready);}}};break;
                    case 4:{int id=readInt("Order ID: ");action=()->{requireTakeaway(id);System.out.println(app.state.getBillingService().generateBill(app.state.getOrderService().requireOrder(id)));};break;}
                    case 5:action=payment(true);break;
                    case 6:{int id=readInt("Order ID: ");action=()->app.collect(id);break;}
                    case 7:action=cancel(true);break;
                    default:throw new IllegalArgumentException("Invalid option");
                }
                synchronized(app.state){app.healthy();action.run();app.commit("Takeaway action "+choice);}
                app.kitchen.dispatch();
            }catch(EndOfInput ex){throw ex;}catch(RuntimeException ex){System.out.println("Operation failed: "+ex.getMessage());}
            catch(IOException ex){System.err.println("SAVE FAILED; return to dashboard and retry Save: "+ex.getMessage());return;}
        }
    }
    private void employees() throws IOException {
        while(true) {
            Access.require(Role.ADMIN);
            System.out.println("\n===== EMPLOYEES =====\n1. List / workloads  2. Search  3. Add  4. Rename  5. Role  6. Activate  7. Deactivate  8. Reset password  9. Chef categories  0. Back");
            int choice=readInt("Choice: ");if(choice==0)return;
            char[] password=null;
            try {
                Runnable action;
                if(choice==1) action=()->{staff.displayStaff();new TableAllocationService(app.state.getOrderService().getTables(),staff).displayWaiterWorkload();ChefAssignmentService cs=new ChefAssignmentService();for(int i=0;i<staff.getCount();i++){StaffAccount a=staff.getAccountAt(i);if(a.getRole()==Role.CHEF)System.out.println(a+" | Categories: "+a.getSpecializations()+" | Preparing: "+cs.workload(app.state.getOrderService(),a.getEmployeeId()));}};
                else {
                    int id=readInt("Employee ID: ");
                    switch(choice) {
                        case 2:action=()->System.out.println(staff.findAccount(id));break;
                        case 3:{String name=readRequiredLine("Name: ");Role role=Role.valueOf(readRequiredLine("ADMIN / MANAGER / WAITER / CHEF / CASHIER: ").trim().toUpperCase(Locale.ROOT));password=readConfirmedPassword("Password: ","Confirm: ");final char[] pw=password;action=()->staff.addAccount(new StaffAccount(id,name,role,pw));break;}
                        case 4:{String name=readRequiredLine("New name: ");action=()->staff.updateEmployeeName(id,name);break;}
                        case 5:{Role role=Role.valueOf(readRequiredLine("New role: ").trim().toUpperCase(Locale.ROOT));action=()->staff.updateEmployeeRole(id,role);break;}
                        case 6:action=()->staff.activateAccount(id);break;
                        case 7:action=()->staff.deactivateAccount(id);break;
                        case 8:password=readConfirmedPassword("New password: ","Confirm: ");final char[] pw=password;action=()->staff.resetPassword(id,pw);break;
                        case 9:{String cats=readRequiredLine("Comma-separated exact category names (blank = all): ");action=()->{StaffAccount a=staff.findAccount(id);if(a==null)throw new IllegalArgumentException("Unknown employee");a.setSpecializations(cats);};break;}
                        default:throw new IllegalArgumentException("Invalid option");
                    }
                }
                synchronized(app.state){app.healthy();int actor=Access.current().getEmployeeId();action.run();app.state.audit("Employee "+actor+" staff action "+choice);app.save();}
                // A self password/role/status change expires this session immediately.
                Access.current();app.kitchen.dispatch();
            }catch(EndOfInput ex){throw ex;}catch(RuntimeException ex){System.out.println("Operation failed: "+ex.getMessage());}
            catch(IOException ex){System.err.println("SAVE FAILED; return to dashboard and retry Save: "+ex.getMessage());return;}
            finally {if(password!=null)Arrays.fill(password,'\0');}
        }
    }
    private BigDecimal money(String prompt){return new BigDecimal(readRequiredLine(prompt).trim());}
    private boolean yes(String prompt){String v=readRequiredLine(prompt).trim();if(!v.equalsIgnoreCase("yes")&&!v.equalsIgnoreCase("no"))throw new IllegalArgumentException("Enter yes or no");return v.equalsIgnoreCase("yes");}
    public static void main(String[] args){Main.main(args);}
}
