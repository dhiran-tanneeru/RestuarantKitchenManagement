package restuarant.service;
import restuarant.model.*;
/** One real Java thread per active chef; a failed/interrupted task is recoverable. */
public class ChefWorker implements Runnable {
    private final Chef chef;
    private final KitchenService kitchenService;
    public ChefWorker(Chef chef,KitchenService kitchenService) {
        if(chef==null || kitchenService==null) throw new IllegalArgumentException("Chef and kitchen required");
        this.chef=chef;this.kitchenService=kitchenService;
    }
    public void run() {
        OrderItem item=null;
        try {
            while(!Thread.currentThread().isInterrupted()) {
                item=kitchenService.workerClaim(chef.getEmployeeId());
                if(item==null) {
                    // Another worker may need to claim the lower-ID tie first. Do not exit
                    // while eligible pending work exists, or thread start order defeats balancing.
                    if(!kitchenService.hasPendingFor(chef.getEmployeeId())) break;
                    Thread.sleep(10); continue;
                }
                Thread.sleep(item.getMenuItem().getPreparationTime()*kitchenService.getMillisPerMinute());
                kitchenService.complete(chef.getEmployeeId(),item); item=null;
            }
        } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
        catch(RuntimeException ex) { System.err.println("Kitchen stopped: "+ex.getMessage()); }
        finally { try { kitchenService.recover(chef.getEmployeeId(),item); } finally { kitchenService.finished(chef.getEmployeeId()); } }
    }
}
