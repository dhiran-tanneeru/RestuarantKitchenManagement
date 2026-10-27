package restuarant.service;
import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.model.*;
import java.io.Serializable;
import java.util.*;
public class CancellationService implements Serializable {
    private static final long serialVersionUID=1L;
    private final List<CancellationRequest> requests=new ArrayList<>();
    public CancellationRequest request(Order order,int index,String reason) {
        Access.order(order); order.ensureEditable();
        if(order.getItem(index).getStatus()!=OrderItem.Status.PENDING) throw new IllegalStateException("Only pending dishes can be cancelled");
        for(CancellationRequest r:requests) if(r.orderId==order.getOrderId() && r.itemIndex==index && r.getStatus()==CancellationRequest.Status.PENDING) throw new IllegalStateException("Cancellation already requested");
        CancellationRequest r=new CancellationRequest(requests.size()+1,order.getOrderId(),index,Access.current().getEmployeeId(),reason);
        requests.add(r); return r;
    }
    public boolean isHeld(int order,int item) {
        for(CancellationRequest r:requests) if(r.orderId==order && r.itemIndex==item && r.getStatus()==CancellationRequest.Status.PENDING) return true;
        return false;
    }
    public void decide(int id,boolean approve,String reason,OrderServices orders) {
        StaffAccount a=Access.require(Role.ADMIN,Role.MANAGER);
        if(reason==null || reason.trim().isEmpty()) throw new IllegalArgumentException("Decision reason required");
        CancellationRequest found=null; for(CancellationRequest r:requests) if(r.id==id) found=r;
        if(found==null) throw new IllegalArgumentException("Unknown request");
        if(found.getStatus()!=CancellationRequest.Status.PENDING) throw new IllegalStateException("Request already decided");
        if(approve) orders.requireOrder(found.orderId).getItem(found.itemIndex).cancel();
        found.decide(approve,a.getEmployeeId(),reason);
    }
    public List<CancellationRequest> history() { return Collections.unmodifiableList(requests); }
}
