
package restuarant.service;
import java.io.Serializable;
import restuarant.billing.*;
import restuarant.model.*;

import java.math.BigDecimal;

public class BillingService implements Serializable{
    private static final long serialVersionUID = 1L;

    private Bill[] bills = new Bill[500];
    private int billCount = 0;
    private int nextPaymentId = 1;

    private final BigDecimal taxRate;
    private transient OrderServices orderRegistry;
    public void bind(OrderServices registry) { if(orderRegistry!=null && orderRegistry!=registry) throw new IllegalStateException("Already bound"); orderRegistry=registry; }

    public BillingService(BigDecimal taxRate) {
        if (taxRate == null || taxRate.signum() < 0 || taxRate.compareTo( new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException( "Invalid tax rate");
        }

        this.taxRate = taxRate;
    }

    public Bill findBill(int orderId) {

        for (int i = 0; i < billCount; i++) {
            if (bills[i].getOrderId() == orderId) {
                return bills[i];
            }
        }

        return null;
    }

    public synchronized Bill generateBill(Order order) {
        if(order==null) throw new IllegalArgumentException("Order required");
        if(orderRegistry!=null && orderRegistry.findOrder(order.getOrderId())!=order) throw new IllegalArgumentException("Order is not in the restaurant registry");
        restuarant.auth.StaffAccount actor=restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.CASHIER,restuarant.auth.StaffAccount.Role.WAITER);
        if(actor.getRole()==restuarant.auth.StaffAccount.Role.WAITER) restuarant.auth.Access.order(order);

        if (order == null) {
            throw new IllegalArgumentException( "Order not found");
        }

        Bill existing = findBill(order.getOrderId());

        if (existing != null) {
            if(existing.getOrder()!=order) throw new IllegalArgumentException("Conflicting order identity");
            return existing;
        }

        if (order.getStatus()!= Order.OrderStatus.OPEN) {
            throw new IllegalStateException( "Order already closed" );
        }

        if (order.getItemCount() == 0) {
            throw new IllegalStateException( "Cannot bill an empty order");
        }

        if (billCount >= bills.length) {
            bills=java.util.Arrays.copyOf(bills, Math.max(1,bills.length*2));
        }

        Bill bill = new Bill(order, taxRate);
        bills[billCount++] = bill;

        return bill;
    }

    public void applyPredefinedDiscount(
            int orderId, Cashier cashier) {

        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.CASHIER);
        if (cashier == null || !cashier.isActive()) {
            throw new IllegalStateException( "Active cashier required");
        }

        Bill bill = requireBill(orderId);

        bill.applyPredefinedOffer();
    }

    public void predefinedOffer(int orderId){requireBill(orderId).applyPredefinedOffer();}
    public void applySpecialDiscount( int orderId, BigDecimal rate, Manager manager) {

        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER);
        if (manager == null || !manager.isActive()) {
            throw new IllegalStateException("Manager approval required");
        }

        Bill bill = requireBill(orderId);
        bill.setDiscountRate(rate);

        System.out.println("Special discount approved by " + restuarant.auth.Access.current().getName());
    }

    public synchronized Payment recordPayment( int orderId, Payment.Method method, BigDecimal amount, Cashier cashier) {

        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.CASHIER);
        if (cashier == null || !cashier.isActive()) {
            throw new IllegalStateException( "Active cashier required");
        }

        Bill bill = requireBill(orderId);

        if (bill.isFullyPaid()) {
            throw new IllegalStateException("Bill already fully paid");
        }

        Payment payment = new Payment(nextPaymentId, orderId, method, amount, orderRegistry==null?java.time.LocalDate.now():orderRegistry.businessDate());

        bill.addPayment(payment);
        nextPaymentId++;

        return payment;
    }

    public void closePaidOrder( Order order, RestuarantTable table) {
        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER,restuarant.auth.StaffAccount.Role.CASHIER);

        if (order == null) {
            throw new IllegalArgumentException( "Order required");
        }

        order.requireOperational();
        Bill bill = requireBill(order.getOrderId());
        if(bill.getOrder()!=order || (orderRegistry!=null && orderRegistry.findOrder(order.getOrderId())!=order)) throw new IllegalArgumentException("Order does not match canonical bill");
        if(order.getOrderType()==Order.OrderType.DINE_IN && orderRegistry!=null && orderRegistry.findTable(order.getTableNumber())!=table) throw new IllegalArgumentException("Canonical restaurant table required");

        if (!bill.isFullyPaid()) {
            throw new IllegalStateException( "Outstanding payment remains" );
        }

        if (order.getItemCount() == 0) {
            throw new IllegalStateException("Cannot close an empty order");
        }

        if (order.getOrderType() == Order.OrderType.DINE_IN) {

            if (table == null || table.getTableNumber() != order.getTableNumber() || table.getStatus() != RestuarantTable.Status.OCCUPIED) {
                throw new IllegalStateException( "Valid occupied table required");
            }
        }

        order.closeOrder();

        if (order.getOrderType() == Order.OrderType.DINE_IN) {
            table.markForCleaning();
        }
    }

    public Bill[] getBills() { return java.util.Arrays.copyOf(bills,billCount); }
    public void discount(int id, BigDecimal rate) { requireBill(id).setDiscountRate(rate); }
    public Payment pay(int id, Payment.Method method, BigDecimal amount) {
        restuarant.auth.StaffAccount a=restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.CASHIER);
        return recordPayment(id,method,amount,new Cashier(a.getEmployeeId(),a.getName()));
    }
    public void reconnect() {
        int max=0;
        for(int i=0;i<billCount;i++) {
            Bill b=bills[i]; b.getOrder().attachBill(b); if(b.getPaymentCount()>0) b.getOrder().lockForPayment();
            for(int j=0;j<b.getPaymentCount();j++) max=Math.max(max,b.getPayment(j).getPaymentId());
            if(b.getBalance().signum()<0) throw new IllegalStateException("Saved bill is overpaid");
        }
        nextPaymentId=Math.max(nextPaymentId,max+1);
    }
    public boolean hasPayments(int orderId) {
        Bill bill = findBill(orderId);
        return bill != null && bill.getPaymentCount() > 0;
    }

    private Bill requireBill(int orderId) {
        Bill bill = findBill(orderId);

        if (bill == null) {
            throw new IllegalArgumentException( "Bill not generated" );
        }

        return bill;
    }
}

