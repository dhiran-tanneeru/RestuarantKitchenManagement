package restuarant.billing;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Method {
        CASH, UPI, CARD
    }

    private final int paymentId;
    private final int orderId;
    private final Method method;
    private final BigDecimal amount;
    private final LocalDateTime paidAt;
    private java.time.LocalDate businessDate;
    public java.time.LocalDate getBusinessDate(){return businessDate==null?paidAt.toLocalDate():businessDate;}
    public Payment(int paymentId,int orderId,Method method,BigDecimal amount,java.time.LocalDate date){
        this(paymentId,orderId,method,amount);if(date==null)throw new IllegalArgumentException("Business date required");businessDate=date;
    }

    public Payment(int paymentId, int orderId, Method method, BigDecimal amount) {
         if (paymentId <= 0 || orderId <= 0) {
            throw new IllegalArgumentException("Invalid ID");
        }

        if (method == null || amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException( "Invalid payment details" );
        }

        this.paymentId = paymentId;
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
        this.paidAt = LocalDateTime.now();
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public Method getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String toString() {
        return "Payment #" + paymentId
            + " | Order #" + orderId
            + " | " + method
            + " | Rs. " + amount.toPlainString()
            + " | " + paidAt;
    }
}

