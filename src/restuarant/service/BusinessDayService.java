package restuarant.service;
import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.model.*;
import restuarant.persistence.RestaurantState;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
public final class BusinessDayService implements Serializable {
    private static final long serialVersionUID=1L;
    private LocalDate date=LocalDate.now();
    private boolean open=true;
    private final List<DayCloseRecord> closures=new ArrayList<>();
    public LocalDate getDate(){return date;}
    public boolean isOpen(){return open;}
    public void requireOpen(){if(!open)throw new IllegalStateException("Business day is closed; a Manager/Admin must open the next business date");}
    public List<DayCloseRecord> history(){return Collections.unmodifiableList(closures);}
    public List<String> exceptions(RestaurantState state) {
        List<String> results=new ArrayList<>();
        OrderServices orders=state.getOrderService();
        for(int i=0;i<orders.getOrderCount();i++) {
            Order o=orders.getOrderAt(i);
            if(o.getStatus()==Order.OrderStatus.OPEN) {
                restuarant.billing.Bill b=state.getBillingService().findBill(o.getOrderId());
                results.add("Open order #"+o.getOrderId()+" "+o.getOrderType()+" | "+(b==null?"unbilled subtotal "+o.subtotalMoney():"balance "+b.getBalance()));
                for(int j=0;j<o.getItemCount();j++) if(o.getItem(j).getStatus()==OrderItem.Status.PENDING || o.getItem(j).getStatus()==OrderItem.Status.PREPARING)
                    results.add("Kitchen #"+o.getOrderId()+" item "+(j+1)+" "+o.getItem(j).getStatus());
            }
        }
        for(RestuarantTable t:orders.getTables())if(t!=null && t.getStatus()!=RestuarantTable.Status.AVAILABLE)results.add("Table "+t.getTableNumber()+" "+t.getStatus());
        for(CancellationRequest q:state.getCancellations().history())if(q.getStatus()==CancellationRequest.Status.PENDING)results.add("Pending cancellation #"+q.id);
        return results;
    }
    public DayCloseRecord close(RestaurantState state,String note) {
        StaffAccount actor=Access.require(Role.ADMIN,Role.MANAGER);requireOpen();
        List<String> pending=exceptions(state);
        if(!pending.isEmpty() && (note==null || note.trim().isEmpty()))throw new IllegalArgumentException("Explain how every unresolved exception will be handled before closing");
        DayCloseRecord record=new DayCloseRecord(date,actor.getEmployeeId(),note==null?"":note.trim(),new ReportingService(state).sales(date),pending);
        closures.add(record);open=false;state.audit("Employee "+actor.getEmployeeId()+" closed business date "+date+" | exceptions "+pending+" | "+note);return record;
    }
    public void openNext(LocalDate next,RestaurantState state) {
        StaffAccount actor=Access.require(Role.ADMIN,Role.MANAGER);
        if(open || next==null || !next.isAfter(date))throw new IllegalArgumentException("Close the current date first, then choose a later business date");
        date=next;open=true;state.audit("Employee "+actor.getEmployeeId()+" opened business date "+date);
    }
}
