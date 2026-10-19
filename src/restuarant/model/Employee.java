
package restuarant.model;

public abstract class Employee {

    private final int employeeId;
    private String name;
    private boolean active;

    public Employee(int employeeId, String name) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        this.employeeId = employeeId;
        setName(name);
        this.active = true;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid name");
        }
        this.name = name.trim();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public abstract String getRole();

    public abstract void performDuty();

    public String toString() {
        return "ID: " + employeeId
             + " | Name: " + name
             + " | Role: " + getRole()
             + " | Status: " + (active ? "Active" : "Inactive");
    }
}
