
package restuarant.service;

import restuarant.model.*;

public class KitchenService {

    private final OrderItem[] queue;
    private int taskCount;
    private int nextTask;

    public KitchenService() {
        queue = new OrderItem[500];
        taskCount = 0;
        nextTask = 0;
    }

    public void loadPendingOrders(OrderServices orderService) {

        if (orderService == null) {
            throw new IllegalArgumentException( "Order service cannot be null");
        }

        taskCount = 0;
        nextTask = 0;

        for (int i = 0; i < orderService.getOrderCount(); i++) {

            Order order = orderService.getOrderAt(i);

            if (order.getStatus() != Order.OrderStatus.OPEN) {
                continue;
            }

            for (int j = 0; j < order.getItemCount(); j++) {

                OrderItem item = order.getItem(j);

                if (item.getStatus() == OrderItem.Status.PENDING) {

                    if (taskCount >= queue.length) {
                        throw new IllegalStateException( "Kitchen queue capacity exceeded");
                    }

                    queue[taskCount++] = item;
                }
            }
        }
    }

    public synchronized OrderItem claimNextTask(int chefId) {

        if (chefId <= 0) {
            throw new IllegalArgumentException( "Invalid chef ID");
        }

        while (nextTask < taskCount) {

            OrderItem item = queue[nextTask++];

            if (item.getStatus() != OrderItem.Status.PENDING) {
                continue;
            }

            item.startPreparing(chefId);
            return item;
        }

        return null;
    }

    public void displayQueue() {

        System.out.println("\n====== KITCHEN QUEUE ======");

        if (taskCount == 0) {
            System.out.println("No pending dishes.");
            return;
        }

        for (int i = 0; i < taskCount; i++) {
            OrderItem item = queue[i];

            System.out.println(
                (i + 1) + ". " + item
                + " | Chef ID: " + item.getAssignedChefId()
            );
        }
    }

    public int getTaskCount() {
        return taskCount;
    }
}
