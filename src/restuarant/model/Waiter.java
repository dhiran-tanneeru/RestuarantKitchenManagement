package restuarant.model;

public class Waiter extends Employee {

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

