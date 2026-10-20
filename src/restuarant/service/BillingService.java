
package restuarant.service;
import java.io.Serializable;
import restuarant.billing.*;
import restuarant.model.*;

import java.math.BigDecimal;

public class BillingService implements Serializable{
    private static final long serialVersionUID = 1L;

    private final Bill[] bills = new Bill[500];
    private int billCount = 0;
    private int nextPaymentId = 1;

    private final BigDecimal taxRate;

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

    public Bill generateBill(Order order) {

        if (order == null) {
            throw new IllegalArgumentException( "Order not found");
        }

        Bill existing = findBill(order.getOrderId());

        if (existing != null) {
            return existing;
        }

        if (order.getStatus()!= Order.OrderStatus.OPEN) {
            throw new IllegalStateException( "Order already closed" );
        }

        if (order.getItemCount() == 0) {
            throw new IllegalStateException( "Cannot bill an empty order");
        }

        if (billCount >= bills.length) {
            throw new IllegalStateException( "Bill storage full" );
        }

        Bill bill = new Bill(order, taxRate);
        bills[billCount++] = bill;

        return bill;
    }

    public void applyPredefinedDiscount(
            int orderId, Cashier cashier) {

        if (cashier == null || !cashier.isActive()) {
            throw new IllegalStateException( "Active cashier required");
        }

        Bill bill = requireBill(orderId);

        bill.setDiscountRate( new BigDecimal("10") );
    }

    public void applySpecialDiscount( int orderId, BigDecimal rate, Manager manager) {

        if (manager == null || !manager.isActive()) {
            throw new IllegalStateException("Manager approval required");
        }

        Bill bill = requireBill(orderId);
        bill.setDiscountRate(rate);

        System.out.println("Special discount approved by " + manager.getName());
    }

    public Payment recordPayment( int orderId, Payment.Method method, BigDecimal amount, Cashier cashier) {

        if (cashier == null || !cashier.isActive()) {
            throw new IllegalStateException( "Active cashier required");
        }

        Bill bill = requireBill(orderId);

        if (bill.isFullyPaid()) {
            throw new IllegalStateException("Bill already fully paid");
        }

        Payment payment = new Payment(nextPaymentId, orderId, method, amount);

        bill.addPayment(payment);
        nextPaymentId++;

        return payment;
    }

    public void closePaidOrder( Order order, RestuarantTable table) {

        if (order == null) {
            throw new IllegalArgumentException( "Order required");
        }

        Bill bill = requireBill(order.getOrderId());

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

