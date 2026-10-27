package restuarant.service;
import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.model.*;
import java.io.Serializable;
import java.util.*;
public class CancellationService implements Serializable {
    private static final long serialVersionUID=1L;
    private final List<CancellationRequest> requests=new ArrayList<>();
    private transient OrderServices registry;
    public void bind(OrderServices orders){registry=orders;}
    private void canonical(Order order){if(order==null || registry!=null && registry.findOrder(order.getOrderId())!=order)throw new IllegalArgumentException("Canonical order required");}
    public CancellationRequest request(Order order,int index,String reason) {
        canonical(order);Access.order(order);
        synchronized(order){
            order.ensureEditable();
            if(index<0)throw new IllegalArgumentException("Use whole-order cancellation");
            if(order.getItem(index).getStatus()!=OrderItem.Status.PENDING)throw new IllegalStateException("Only pending dishes can be cancelled");
            if(isHeld(order.getOrderId(),index))throw new IllegalStateException("Cancellation already requested");
            CancellationRequest r=new CancellationRequest(requests.size()+1,order.getOrderId(),index,Access.current().getEmployeeId(),reason);requests.add(r);return r;
        }
    }
    public CancellationRequest requestWhole(Order order,String reason){
        canonical(order);Access.order(order);
        synchronized(order){
            order.ensureEditable();if(hasPending(order.getOrderId()))throw new IllegalStateException("Resolve existing item requests first");
            for(int i=0;i<order.getItemCount();i++)if(order.getItem(i).getStatus()!=OrderItem.Status.PENDING && order.getItem(i).getStatus()!=OrderItem.Status.CANCELLED)throw new IllegalStateException("Whole-order cancellation requires pending dishes");
            CancellationRequest r=new CancellationRequest(requests.size()+1,order.getOrderId(),-1,Access.current().getEmployeeId(),reason);requests.add(r);return r;
        }
    }
    public boolean hasPending(int order){for(CancellationRequest r:requests)if(r.orderId==order && r.getStatus()==CancellationRequest.Status.PENDING)return true;return false;}
    public boolean isWholeHeld(int order){for(CancellationRequest r:requests)if(r.orderId==order && r.isWholeOrder() && r.getStatus()==CancellationRequest.Status.PENDING)return true;return false;}
    public boolean isHeld(int order,int item){for(CancellationRequest r:requests)if(r.orderId==order && (r.isWholeOrder() || r.itemIndex==item) && r.getStatus()==CancellationRequest.Status.PENDING)return true;return false;}
    public void decide(int id,boolean approve,String reason,OrderServices orders) {
        StaffAccount a=Access.require(Role.ADMIN,Role.MANAGER);
        if(registry!=null && registry!=orders)throw new IllegalArgumentException("Wrong order registry");
        if(reason==null || reason.trim().isEmpty())throw new IllegalArgumentException("Decision reason required");
        CancellationRequest found=null;for(CancellationRequest r:requests)if(r.id==id)found=r;
        if(found==null)throw new IllegalArgumentException("Unknown request");
        Order order=orders.requireOrder(found.orderId);
        synchronized(order){
            order.requireOperational();if(found.getStatus()!=CancellationRequest.Status.PENDING)throw new IllegalStateException("Request already decided");
            if(approve){
                if(found.isWholeOrder()){
                    RestuarantTable table=order.getOrderType()==Order.OrderType.DINE_IN?orders.requireTable(order.getTableNumber()):null;
                    if(table!=null && table.getStatus()!=RestuarantTable.Status.OCCUPIED)throw new IllegalStateException("Occupied source table required");
                    order.cancelWhole();if(table!=null)table.markForCleaning();
                }else order.getItem(found.itemIndex).cancel();
            }
            found.decide(approve,a.getEmployeeId(),reason);
        }
    }
    public List<CancellationRequest> history(){return Collections.unmodifiableList(requests);}
}
