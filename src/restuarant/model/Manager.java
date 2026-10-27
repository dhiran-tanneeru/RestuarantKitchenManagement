
package restuarant.model;

public class Manager extends Employee {
    private static final long serialVersionUID = -7968944658915240968L;

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

        throw new IllegalStateException("Use CancellationService.decide with a cancellation request ID; order-only approval is unsafe");
    }
}
