package restuarant.service;
import restuarant.auth.*;
import restuarant.auth.StaffAccount.Role;
import restuarant.model.*;
import restuarant.billing.*;
import restuarant.persistence.RestaurantState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
/** Financial values are derived from persisted orders/payments, never supplied by a menu. */
public final class ReportingService {
    private final RestaurantState state;
    public ReportingService(RestaurantState state){this.state=state;}
    public String sales(LocalDate date) {
        Access.require(Role.ADMIN,Role.MANAGER,Role.CASHIER);
        if(date==null)throw new IllegalArgumentException("Report date required");
        BigDecimal received=BigDecimal.ZERO, daily=BigDecimal.ZERO, outstanding=BigDecimal.ZERO, completed=BigDecimal.ZERO;
        Map<Payment.Method,BigDecimal> methods=new EnumMap<>(Payment.Method.class);
        Map<String,Integer> dishes=new TreeMap<>();StringBuilder transactions=new StringBuilder();
        for(Payment.Method m:Payment.Method.values()) methods.put(m,BigDecimal.ZERO);
        for(Bill b:state.getBillingService().getBills()) {
            outstanding=outstanding.add(b.getBalance());
            if(b.isFullyPaid() && b.getOrder().getStatus()==Order.OrderStatus.CLOSED) completed=completed.add(b.getTotal());
            for(int i=0;i<b.getPaymentCount();i++) {
                Payment p=b.getPayment(i); received=received.add(p.getAmount());methods.put(p.getMethod(),methods.get(p.getMethod()).add(p.getAmount()));
                if(p.getBusinessDate().equals(date)) daily=daily.add(p.getAmount());
                transactions.append(p).append('\n');
            }
        }
        int open=0,closed=0,takeaway=0,unbilled=0;BigDecimal unbilledSubtotal=BigDecimal.ZERO;
        for(int i=0;i<state.getOrderService().getOrderCount();i++) {
            Order o=state.getOrderService().getOrderAt(i);
            if(o.getStatus()==Order.OrderStatus.OPEN) open++;else closed++;
            if(o.getOrderType()==Order.OrderType.TAKEAWAY) takeaway++;
            if(o.getStatus()==Order.OrderStatus.OPEN && state.getBillingService().findBill(o.getOrderId())==null) { unbilled++;unbilledSubtotal=unbilledSubtotal.add(o.subtotalMoney()); }
            if(o.getStatus()==Order.OrderStatus.CLOSED) for(int j=0;j<o.getItemCount();j++) {
                OrderItem item=o.getItem(j);if(item.getStatus()!=OrderItem.Status.CANCELLED) dishes.put(item.getMenuItem().getName(),dishes.getOrDefault(item.getMenuItem().getName(),0)+item.getQuantity());
            }
        }
        List<Map.Entry<String,Integer>> ranked=new ArrayList<>(dishes.entrySet());ranked.sort((a,b)->Integer.compare(b.getValue(),a.getValue()));
        return "Cash receipts (includes tax and partial payments): "+received+"\nBusiness-date receipts ("+date+"): "+daily+"\nClosed settled sales (includes tax): "+completed+"\nOutstanding billed balances: "+outstanding+"\nUnbilled open orders: "+unbilled+" | subtotal: "+unbilledSubtotal+"\nPayment methods: "+methods+"\nOrders: open="+open+", closed="+closed+", takeaway="+takeaway+"\nBest-selling dishes (closed orders, quantities): "+ranked+"\nTransactions:\n"+transactions;
    }
}
