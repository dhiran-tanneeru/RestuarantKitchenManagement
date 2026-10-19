
package restuarant.service;

import restuarant.model.*;

public class ChefWorker implements Runnable {

    private final Chef chef;
    private final KitchenService kitchenService;

    public ChefWorker(Chef chef, KitchenService kitchenService) {

        if (chef == null || kitchenService == null) {
            throw new IllegalArgumentException( "Chef and kitchen service are required");
        }

        this.chef = chef;
        this.kitchenService = kitchenService;
    }

    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            OrderItem item = kitchenService.claimNextTask( chef.getEmployeeId());

            if (item == null) {
                break;
            }

            String dishName = item.getMenuItem().getName();

            System.out.println( chef.getName() + " started preparing " + dishName);

            try {
                long preparationTime = item.getMenuItem().getPreparationTime() * 100L;

                Thread.sleep(preparationTime);

                item.markReady();

                System.out.println( chef.getName() + " finished " + dishName + " | Status: " + item.getStatus());

            } catch (InterruptedException e) {

                System.out.println( chef.getName() + " was interrupted while preparing " + dishName);

                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
