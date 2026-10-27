package restuarant.model;

public class Chef extends Employee {
    private static final long serialVersionUID = -3275741172915098960L;

    public Chef(int employeeId, String name) {
        super(employeeId, name);
    }

    public String getRole() {
        return "Chef";
    }

    public void performDuty() {
        System.out.println(
            getName() + " is preparing kitchen orders."
        );
    }

    public void prepareDish(String dishName) {
        if (!isActive()) {
            throw new IllegalStateException("Chef is inactive");
        }

        if (dishName == null || dishName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid dish");
        }

        System.out.println(
            getName() + " is preparing " + dishName
        );
    }
}

