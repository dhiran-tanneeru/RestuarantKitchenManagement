
package restuarant.auth;

import java.io.Serializable;

public class StaffService implements Serializable {

    private static final long serialVersionUID = 1L;

    private StaffAccount[] accounts;
    private int count;

    public StaffService() {
        accounts = new StaffAccount[100];
        count = 0;
    }

    public void addAccount(StaffAccount account) {

        if (count > 0) Access.staff(this);
        else if(account == null || account.getRole()!=StaffAccount.Role.ADMIN) throw new SecurityException("First account must be Admin");
        if (account == null) {
            throw new IllegalArgumentException( "Account cannot be null");
        }

        if (findAccount(account.getEmployeeId()) != null) {
            throw new IllegalArgumentException("Employee ID already exists");
        }

        if (count >= accounts.length) {
            accounts=java.util.Arrays.copyOf(accounts, Math.max(1, accounts.length*2));
        }

        accounts[count++] = account;
        account.attach(this);
    }

    public StaffAccount findAccount(int employeeId) {

        for (int i = 0; i < count; i++) {
            if (accounts[i].getEmployeeId() == employeeId) {
                return accounts[i];
            }
        }

        return null;
    }

    public StaffAccount login(int employeeId, char[] password) {

        Access.logout();
        StaffAccount account = findAccount(employeeId);

        if (account == null || !account.isActive()) {
            throw new SecurityException("Invalid credentials or inactive account");
        }

        if (!account.verifyPassword(password)) {
            throw new SecurityException("Invalid credentials or inactive account");
        }

        Access.establish(this,account);
        return account;
    }

    public void deactivateAccount(int employeeId) {

        StaffAccount account = findAccount(employeeId);

        if (account == null) {
            throw new IllegalArgumentException("Employee not found");
        }

        account.setActive(false);
    }

    public void displayStaff() {

        System.out.println("\n===== STAFF ACCOUNTS =====");

        for (int i = 0; i < count; i++) {
            System.out.println(accounts[i]);
        }
    }

    
    public void updateEmployeeName(int employeeId, String name) {

        StaffAccount account = findAccount(employeeId);

        if (account == null) {
            throw new IllegalArgumentException(
                "Employee not found"
            );
        }

        account.setName(name);
    }

    public void updateEmployeeRole(
            int employeeId, StaffAccount.Role newRole) {

        StaffAccount account = findAccount(employeeId);

        if (account == null) {
            throw new IllegalArgumentException(
                "Employee not found"
            );
        }

        

        account.setRole(newRole);
    }

    public void activateAccount(int employeeId) {

        StaffAccount account = findAccount(employeeId);

        if (account == null) {
            throw new IllegalArgumentException(
                "Employee not found"
            );
        }

        account.setActive(true);
    }

    public void resetPassword(
            int employeeId, char[] newPassword) {

        StaffAccount account = findAccount(employeeId);

        if (account == null) {
            throw new IllegalArgumentException(
                "Employee not found"
            );
        }

        account.changePassword(newPassword);
    }

    
    void protectLastAdmin(StaffAccount target, StaffAccount.Role next, boolean active) {
        if(target.isActive() && target.getRole()==StaffAccount.Role.ADMIN && (!active || next!=StaffAccount.Role.ADMIN)) {
            int n=0; for(int i=0;i<count;i++) if(accounts[i].isActive() && accounts[i].getRole()==StaffAccount.Role.ADMIN) n++;
            if(n<=1) throw new IllegalStateException("Cannot remove the last active Admin");
        }
    }
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        for(int i=0;i<count;i++) accounts[i].attach(this);
    }
    public int getCount() {
        return count;
    }

    public StaffAccount getAccountAt(int index) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException(
                "Invalid staff index"
            );
        }

        return accounts[index];
    }

    
}
