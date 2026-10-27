
package restuarant.auth;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class StaffAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Role {
        ADMIN, MANAGER, WAITER, CHEF, CASHIER
    }

    private final int employeeId;
    private String name;
    private Role role;

    private boolean active;
    private long credentialVersion;
    private java.util.Set<String> specializations = new java.util.HashSet<>();
    private transient StaffService owner;
    void attach(StaffService owner) { this.owner=owner; }
    public long getCredentialVersion() { return credentialVersion; }
    private void admin() { if(owner!=null) Access.staff(owner); }
    public boolean canCook(String category) { return specializations==null || specializations.isEmpty() || specializations.contains(category.trim().toLowerCase(java.util.Locale.ROOT)); }
    public String getSpecializations() { return specializations==null || specializations.isEmpty() ? "ALL" : specializations.toString(); }
    public void setSpecializations(String csv) {
        admin();
        if(role!=Role.CHEF) throw new IllegalStateException("Chef required");
        java.util.Set<String> next=new java.util.HashSet<>();
        for(String value:csv.split(",")) if(!value.trim().isEmpty()) next.add(value.trim().toLowerCase(java.util.Locale.ROOT));
        specializations=next;
    }
    private byte[] salt;
    private byte[] passwordHash;

    private static final int ITERATIONS = 150000;
    private static final int KEY_LENGTH = 256;

    public StaffAccount(int employeeId, String name, Role role, char[] password) {

        if (employeeId <= 0 || role == null || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException( "Invalid employee details");
        }

        validatePassword(password);

        this.employeeId = employeeId;
        this.name = name.trim();
        this.role = role;
        this.active = true;

        salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        passwordHash = hash(password, salt);
    }

    
    public void setName(String name) {
        admin();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Employee name cannot be empty"
            );
        }

        this.name = name.trim();
    }

    public void setRole(Role role) {
        admin();
        if(owner!=null) owner.protectLastAdmin(this, role, active);
        if (role == null) {
            throw new IllegalArgumentException(
                "Role cannot be null"
            );
        }

        this.role = role;
        credentialVersion++;
    }


    private static void validatePassword(char[] password) {
        if (password == null || password.length < 8 || new String(password).trim().isEmpty()) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
    }

    private static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec( password, salt, ITERATIONS, KEY_LENGTH);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing failed", e);
        } finally {
            spec.clearPassword();
        }
    }

    public boolean verifyPassword(char[] password) {
        if (password == null) {
            return false;
        }

        byte[] candidate = hash(password, salt);

        try {
            return java.security.MessageDigest.isEqual(candidate, passwordHash);
        } finally {
            Arrays.fill(candidate, (byte) 0);
        }
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        admin();
        if(owner!=null) owner.protectLastAdmin(this, role, active);
        this.active = active;
        credentialVersion++;
    }

    
    public void changeOwnPassword(char[] currentPassword,char[] newPassword){
        if(Access.current()!=this || !verifyPassword(currentPassword))throw new SecurityException("Current password is incorrect");
        replacePassword(newPassword);
    }
    public void changePassword(char[] newPassword) {
        admin();replacePassword(newPassword);
    }
    private void replacePassword(char[] newPassword){
        validatePassword(newPassword);
        credentialVersion++;

        byte[] newSalt = new byte[16];
        new SecureRandom().nextBytes(newSalt);

        byte[] newHash = hash(newPassword, newSalt);

        Arrays.fill(passwordHash, (byte) 0);

        this.salt = newSalt;
        this.passwordHash = newHash;
    }

    public String toString() {
        return employeeId + " | " + name
            + " | " + role
            + " | " + (active ? "Active" : "Inactive");
    }
}
