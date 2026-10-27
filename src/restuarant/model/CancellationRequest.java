package restuarant.model;
import java.io.Serializable;
import java.time.LocalDateTime;
public final class CancellationRequest implements Serializable {
    private static final long serialVersionUID=1L;
    public enum Status { PENDING, APPROVED, REJECTED }
    public final int id, orderId, itemIndex, requestedBy;
    public final String reason;
    public final LocalDateTime requestedAt=LocalDateTime.now();
    private Status status=Status.PENDING;
    private int reviewedBy;
    private LocalDateTime reviewedAt;
    public boolean isWholeOrder(){return itemIndex==-1;}
    public int getReviewedBy(){return reviewedBy;}
    public String getDecisionReason(){return decisionReason;}
    public LocalDateTime getReviewedAt(){return reviewedAt;}
    private String decisionReason="";
    public CancellationRequest(int id,int order,int item,int actor,String reason) {
        if(reason==null || reason.trim().isEmpty()) throw new IllegalArgumentException("Cancellation reason required");
        this.id=id;orderId=order;itemIndex=item;requestedBy=actor;this.reason=reason.trim();
    }
    public Status getStatus() { return status; }
    public void decide(boolean approve,int actor,String note) {
        if(status!=Status.PENDING) throw new IllegalStateException("Request already decided");
        status=approve?Status.APPROVED:Status.REJECTED;reviewedBy=actor;decisionReason=note;reviewedAt=LocalDateTime.now();
    }
    public String toString() { return "Request "+id+" | Order "+orderId+(isWholeOrder()?" whole order":" item "+(itemIndex+1))+" | "+status+" | requested by "+requestedBy+" | "+reason+" | reviewed by "+reviewedBy+" | "+decisionReason; }
}
