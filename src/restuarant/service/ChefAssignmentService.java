package restuarant.service;
import restuarant.auth.*;
import restuarant.model.*;
/** Select an active qualified chef by current workload, then employee ID. */
public final class ChefAssignmentService {
    public StaffAccount choose(StaffService staff,OrderServices orders,OrderItem item) {
        StaffAccount best=null; int lowest=Integer.MAX_VALUE;
        for(int i=0;i<staff.getCount();i++) {
            StaffAccount a=staff.getAccountAt(i);
            if(!a.isActive() || a.getRole()!=StaffAccount.Role.CHEF || !a.canCook(item.getMenuItem().getCategory())) continue;
            int load=workload(orders,a.getEmployeeId());
            if(load<lowest || (load==lowest && (best==null || a.getEmployeeId()<best.getEmployeeId()))) { best=a;lowest=load; }
        }
        return best;
    }
    public int workload(OrderServices orders,int chef) {
        int count=0;
        for(int i=0;i<orders.getOrderCount();i++) for(int j=0;j<orders.getOrderAt(i).getItemCount();j++) {
            OrderItem item=orders.getOrderAt(i).getItem(j);
            if(item.getStatus()==OrderItem.Status.PREPARING && item.getAssignedChefId()==chef) count++;
        }
        return count;
    }
}
