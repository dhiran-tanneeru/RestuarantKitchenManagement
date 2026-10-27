
package restuarant.service;

import restuarant.auth.StaffAccount;
import restuarant.auth.StaffService;
import restuarant.model.*;

public class TableAllocationService {

    private final RestuarantTable[] tables;
    private final StaffService staff;

    public TableAllocationService(RestuarantTable[] tables,
                                  StaffService staff) {

        if (tables == null || staff == null) {
            throw new IllegalArgumentException(
                "Tables and staff are required"
            );
        }

        this.tables = tables;
        this.staff = staff;
    }

    public synchronized TableAssignment assignTable(
            int guestCount) {

        restuarant.auth.Access.require(StaffAccount.Role.ADMIN,StaffAccount.Role.MANAGER,StaffAccount.Role.WAITER);
        if (guestCount <= 0) {
            throw new IllegalArgumentException(
                "Guest count must be positive"
            );
        }

        RestuarantTable bestTable = findBestTable(
            guestCount
        );

        if (bestTable == null) {
            throw new IllegalStateException(
                "No suitable table is available"
            );
        }

        StaffAccount waiter = findBestWaiter();

        if (waiter == null) {
            throw new IllegalStateException(
                "No active waiter is available"
            );
        }

        // Existing model validates capacity and status.
        bestTable.seatGuests(
            guestCount, waiter.getEmployeeId()
        );

        return new TableAssignment(
            bestTable.getTableNumber(),
            waiter.getEmployeeId(),
            waiter.getName(),
            guestCount
        );
    }

    private RestuarantTable findBestTable(int guestCount) {

        RestuarantTable best = null;

        for (RestuarantTable table : tables) {

            if (table == null) {
                continue;
            }

            if (table.getStatus()
                    != RestuarantTable.Status.AVAILABLE) {
                continue;
            }

            if (table.getCapacity() < guestCount) {
                continue;
            }

            if (best == null
                    || table.getCapacity() < best.getCapacity()
                    || (table.getCapacity() == best.getCapacity()
                    && table.getTableNumber()
                       < best.getTableNumber())) {

                best = table;
            }
        }

        return best;
    }

    private StaffAccount findBestWaiter() {

        StaffAccount best = null;
        int lowestWorkload = Integer.MAX_VALUE;

        for (int i = 0; i < staff.getCount(); i++) {

            StaffAccount account = staff.getAccountAt(i);

            if (!account.isActive()
                    || account.getRole()
                       != StaffAccount.Role.WAITER) {
                continue;
            }

            int workload = countAssignedTables(
                account.getEmployeeId()
            );

            if (best == null
                    || workload < lowestWorkload
                    || (workload == lowestWorkload
                    && account.getEmployeeId()
                       < best.getEmployeeId())) {

                best = account;
                lowestWorkload = workload;
            }
        }

        return best;
    }

    private int countAssignedTables(int waiterId) {

        int count = 0;

        for (RestuarantTable table : tables) {

            if (table != null
                    && table.getStatus()
                       == RestuarantTable.Status.OCCUPIED
                    && table.getWaiterId() == waiterId) {

                count++;
            }
        }

        return count;
    }

    public synchronized void displayWaiterWorkload() {

        System.out.println(
            "\n===== WAITER WORKLOAD ====="
        );

        for (int i = 0; i < staff.getCount(); i++) {

            StaffAccount account = staff.getAccountAt(i);

            if (account.getRole()
                    != StaffAccount.Role.WAITER) {
                continue;
            }

            System.out.println(
                account.getEmployeeId()
                + " | " + account.getName()
                + " | Active: " + account.isActive()
                + " | Occupied Tables: "
                + countAssignedTables(
                    account.getEmployeeId()
                )
            );
        }
    }

    public synchronized void displayAssignedTables(
            int waiterId) {

        System.out.println(
            "\n===== MY ASSIGNED TABLES ====="
        );

        boolean found = false;

        for (RestuarantTable table : tables) {

            if (table != null
                    && table.getStatus()
                       == RestuarantTable.Status.OCCUPIED
                    && table.getWaiterId() == waiterId) {

                System.out.println(table);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No tables assigned.");
        }
    }
}
