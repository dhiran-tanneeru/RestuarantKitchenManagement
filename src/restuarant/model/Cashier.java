package restuarant.model;

public class Cashier extends Employee {

    public Cashier(int employeeId, String name) {
        super(employeeId, name);
    }

    public String getRole() {
        return "Cashier";
    }

    public void performDuty() {
        System.out.println(
            getName() + " is managing customer payments."
        );
    }

    public void generateBill(int orderId) {
        if (!isActive()) {
            throw new IllegalStateException("Cashier is inactive");
        }

        System.out.println(
            getName() + " is generating bill for Order "
            + orderId
        );
    }
}

