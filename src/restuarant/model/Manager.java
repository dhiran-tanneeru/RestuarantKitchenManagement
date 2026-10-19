
package restuarant.model;

public class Manager extends Employee {

    public Manager(int employeeId, String name) {
        super(employeeId, name);
    }

    public String getRole() {
        return "Manager";
    }

    public void performDuty() {
        System.out.println(
            getName() + " is supervising restaurant operations."
        );
    }

    public void approveCancellation(int orderId) {
        if (!isActive()) {
            throw new IllegalStateException("Manager is inactive");
        }

        System.out.println(
            "Manager " + getName()
            + " approved cancellation request for order "
            + orderId
        );
    }
}

