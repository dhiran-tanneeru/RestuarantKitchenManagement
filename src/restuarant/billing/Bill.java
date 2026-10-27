
package restuarant.billing;
import java.io.Serializable;
import restuarant.model.*;

import java.math.*;

public class Bill implements Serializable{
    private static final long serialVersionUID = 1L;

    private final Order order;
    private final BigDecimal taxRate;

    private BigDecimal discountRate;
    private Payment[] payments;
    private int paymentCount;

    public Bill(Order order, BigDecimal taxRate) {

        if (order == null || taxRate == null) {
            throw new IllegalArgumentException( "Order and tax rate required");
        }

        if (taxRate.signum() < 0 || taxRate.compareTo( new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException( "Invalid tax rate" );
        }

        this.order = order;
        this.taxRate = taxRate;
        this.discountRate = BigDecimal.ZERO;

        this.payments = new Payment[20];
        this.paymentCount = 0;
        order.attachBill(this);
    }

    public int getOrderId() {
        return order.getOrderId();
    }

    public BigDecimal getSubtotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < order.getItemCount(); i++) {

            OrderItem item = order.getItem(i);

            if (item.getStatus() != OrderItem.Status.CANCELLED) {

                BigDecimal price = item.priceMoney();

                total = total.add( price.multiply( BigDecimal.valueOf( item.getQuantity() ) ) );
            }
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getDiscountAmount() {
        return getSubtotal().multiply(discountRate).divide( new BigDecimal("100"), 2, RoundingMode.HALF_UP );
    }

    public BigDecimal getTaxAmount() {

        BigDecimal taxable = getSubtotal().subtract(getDiscountAmount());

        return taxable.multiply(taxRate).divide( new BigDecimal("100"), 2, RoundingMode.HALF_UP );
    }

    public BigDecimal getTotal() {
        return getSubtotal().subtract(getDiscountAmount()).add(getTaxAmount());
    }

    public BigDecimal getPaidAmount() {

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < paymentCount; i++) {
            total = total.add(payments[i].getAmount());
        }

        return total;
    }

    public BigDecimal getBalance() {
        return getTotal().subtract(getPaidAmount());
    }

    public boolean isFullyPaid() {
        return getBalance().signum() == 0;
    }

    public void setDiscountRate(BigDecimal rate) {
        synchronized(order) {

        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.MANAGER);
        order.ensureEditable();

        if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException( "Discount must be between 0 and 100" );
        }

        if (paymentCount > 0) {
            throw new IllegalStateException( "Cannot change discount after payment" );
        }

        discountRate = rate;
    
        }
    }

    public void addPayment(Payment payment) {
        synchronized(order) {

        restuarant.auth.Access.require(restuarant.auth.StaffAccount.Role.ADMIN,restuarant.auth.StaffAccount.Role.CASHIER);
        if(order.getStatus()!=Order.OrderStatus.OPEN) throw new IllegalStateException("Order closed");

        if (payment == null || payment.getOrderId() != getOrderId()) {
            throw new IllegalArgumentException( "Payment does not belong to this bill" );
        }

        if (paymentCount >= payments.length) {
            payments=java.util.Arrays.copyOf(payments, Math.max(1,payments.length*2));
        }

        for (int i = 0; i < paymentCount; i++) {
            if (payments[i].getPaymentId() == payment.getPaymentId()) {
                throw new IllegalArgumentException( "Duplicate payment ID");
            }
        }

        if (payment.getAmount().compareTo(getBalance()) > 0) {
            throw new IllegalArgumentException( "Payment exceeds outstanding balance" );
        }

        payments[paymentCount++] = payment;
        order.lockForPayment();
    
        }
    }

    public Order getOrder() { return order; }
    public int getPaymentCount() {
        return paymentCount;
    }

    public Payment getPayment(int index) {
        if (index < 0 || index >= paymentCount) {
            throw new IllegalArgumentException("Invalid payment index");
        }

        return payments[index];
    }

    public String toString() {

        return "\n========== BILL =========="
            + "\nOrder: #" + getOrderId()
            + "\nSubtotal: Rs. " + getSubtotal()
            + "\nDiscount: Rs. " + getDiscountAmount()
            + "\nTax: Rs. " + getTaxAmount()
            + "\n--------------------------"
            + "\nTotal: Rs. " + getTotal()
            + "\nPaid: Rs. " + getPaidAmount()
            + "\nBalance: Rs. " + getBalance()
            + "\nStatus: "
            + (isFullyPaid() ? "PAID" : "UNPAID")
            + "\n==========================";
    }
}

