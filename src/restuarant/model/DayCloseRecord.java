package restuarant.model;
import java.io.Serializable;
import java.time.*;
import java.util.*;
/** An immutable close-of-business snapshot; later settlements do not rewrite it. */
public final class DayCloseRecord implements Serializable {
    private static final long serialVersionUID=1L;
    private final LocalDate businessDate;
    private final LocalDateTime closedAt;
    private final int managerId;
    private final String explanation, report;
    private final List<String> exceptions;
    public DayCloseRecord(LocalDate date,int manager,String explanation,String report,List<String> exceptions) {
        businessDate=date;closedAt=LocalDateTime.now();managerId=manager;
        this.explanation=explanation;this.report=report;this.exceptions=new ArrayList<>(exceptions);
    }
    public LocalDate getBusinessDate(){return businessDate;}
    public List<String> getExceptions(){return Collections.unmodifiableList(exceptions);}
    public String getReport(){return report;}
    public String toString(){return "BUSINESS CLOSE "+businessDate+" | "+closedAt+" | manager "+managerId+"\nExplanation: "+explanation+"\nUnresolved exceptions: "+exceptions+"\n"+report;}
}
