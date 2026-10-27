package restuarant.model;

public class Waiter extends Employee {
    private static final long serialVersionUID = -7724640019832518155L;

    public Waiter(int employeeId, String name) {
        super(employeeId, name);
    }

    public String getRole() {
        return "Waiter";
    }

    public void performDuty() {
        System.out.println(
            getName() + " is serving restaurant tables."
        );
    }

    public void takeOrder(int tableNumber) {
        if (!isActive()) {
            throw new IllegalStateException("Waiter is inactive");
        }

        System.out.println(
            getName() + " is taking an order at Table "
            + tableNumber
        );
    }
}

