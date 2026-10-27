
package restuarant.service;

import restuarant.model.Order;
import restuarant.model.RestuarantTable;

public class DiningSessionService {

    private final RestuarantTable[] tables;
    private final TableAllocationService allocationService;
    private final OrderServices orderService;

    public DiningSessionService(
            RestuarantTable[] tables,
            TableAllocationService allocationService,
            OrderServices orderService) {

        this.tables = tables;
        this.allocationService = allocationService;
        this.orderService = orderService;
    }

    public synchronized Order seatAndCreateOrder(
            int guestCount) {

        TableAssignment assignment =
            allocationService.assignTable(guestCount);

        RestuarantTable table =
            findTable(assignment.getTableNumber());

        try {
            // Keep allocation and order creation in one coordinated operation.
            return orderService.createDineInOrder(
                assignment.getTableNumber()
            );

        } catch (RuntimeException e) {
            table.rollbackSeating();
            throw e;
        }
    }

    private RestuarantTable findTable(int tableNumber) {

        for (RestuarantTable table : tables) {
            if (table != null
                    && table.getTableNumber() == tableNumber) {
                return table;
            }
        }

        throw new IllegalStateException(
            "Assigned table not found"
        );
    }
}
