package restuarant.auth;
import restuarant.auth.StaffAccount.Role;
import restuarant.model.Order;
/** Thread-confined login session. Only StaffService can establish authentication. */
public final class Access {
    private Access() {}
    private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();
    private static final class Session {
        final StaffService staff; final StaffAccount user; final long version;
        Session(StaffService s, StaffAccount a) { staff=s; user=a; version=a.getCredentialVersion(); }
    }
    static void establish(StaffService s, StaffAccount a) { CURRENT.set(new Session(s,a)); }
    public static void logout() { CURRENT.remove(); }
    public static StaffAccount current() {
        Session s=CURRENT.get();
        if(s==null || !s.user.isActive() || s.staff.findAccount(s.user.getEmployeeId())!=s.user
            || s.version!=s.user.getCredentialVersion()) throw new SecurityException("Login required or session expired");
        return s.user;
    }
    public static StaffAccount require(Role... roles) {
        StaffAccount a=current();
        for(Role role:roles) if(a.getRole()==role) return a;
        throw new SecurityException("Permission denied for "+a.getRole());
    }
    public static StaffAccount activeEmployee(int id,Role role) {
        current(); StaffAccount a=CURRENT.get().staff.findAccount(id);
        if(a==null || !a.isActive() || a.getRole()!=role) throw new IllegalArgumentException("Active "+role+" required");
        return a;
    }
    public static void staff(StaffService staff) {
        require(Role.ADMIN);
        if(CURRENT.get().staff!=staff) throw new SecurityException("Different staff registry");
    }
    public static void order(Order order) {
        StaffAccount a=require(Role.ADMIN,Role.MANAGER,Role.WAITER,Role.CASHIER);
        if(a.getRole()==Role.WAITER && (order.getOrderType()!=Order.OrderType.DINE_IN
            || order.getWaiterId()!=a.getEmployeeId())) throw new SecurityException("Order belongs to another waiter");
        if(a.getRole()==Role.CASHIER && order.getOrderType()!=Order.OrderType.TAKEAWAY)
            throw new SecurityException("Cashier may edit takeaway orders only");
    }
}
