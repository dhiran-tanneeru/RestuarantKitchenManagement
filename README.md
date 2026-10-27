# Restaurant & Kitchen Management — Review 1 continuation

This is a continuation of the supplied `rkm(2).zip`. The `restuarant` spelling and original domain/service classes are retained. Review 1 is a Java console application. No database, external Java library or GUI is required.

## Start on Windows / VS Code

1. Extract **RKM_Review1_Complete.zip** into a new folder. Keep your original ZIP and `data` folder as backups.
2. Install a JDK (Java 8 or later), and check `java -version` and `javac -version` in the VS Code terminal.
3. Run `build.bat`, then `run.bat` from that folder. In PowerShell use `./build.bat` and `./run.bat`.
4. The entry point is `restuarant.Main`. `LoginDemo`, `RestaurantConsole` and `TableAllocationDemo` also route through the same authenticated application.

On Linux/macOS run `bash run.sh`. The build scripts compile both application and dependency-free Java tests. Main can also be run after building with `java -cp build restuarant.Main`.

The code was compiled on OpenJDK 17 with `--release 8 -Xlint:all -Werror`. This checks Java 8 language/API compatibility; execution on an actual Java 8 VM and Windows was not available in the test environment.

## Existing data versus a fresh demonstration

- **Continue your data:** copy your existing `data/restaurant.dat` and `data/staff-accounts.dat` together into this project's `data` directory **before the first run**. Your existing employee IDs and passwords still apply. The uploaded files were successfully imported in tests. This does not reset or reveal their passwords.
- **Fresh demonstration:** leave `data` absent. Create Admin ID **1** with your own name and a nonblank password of at least eight characters. There are no default credentials, employees, dishes or tables.
- The application creates `data/review1.dat`, containing accounts, menu, tables, orders, bills, payments, cancellations and history in one snapshot. Once this file exists, it is authoritative; changes to legacy files are not silently re-imported.
- A previous snapshot is retained as `data/review1.dat.bak`. `data/review1.lock` prevents two processes from opening the same data directory.
- Legacy files are read but never overwritten by the new entry point. Historical hardcoded `Chef`/`Manager`/`Cashier` objects remain serialized for compatibility; the staff-account registry alone determines current roles, eligibility and permissions.
- If an imported occupied table names an employee who has no active waiter account, an Admin must create/reactivate the correct account or a Manager must reassign the order. The program does not invent employees or credentials.

A corrupt snapshot/account file stops startup and is not replaced with an empty restaurant. Stop all instances, retain a copy of the corrupt file for diagnosis, and restore a known good **combined** `review1.dat.bak` as `review1.dat`. For legacy recovery restore the matching restaurant/account pair into a separate test directory. Never mix snapshots casually. Saves are local serialized data: use your own trusted files and protect the data directory with OS permissions.

## Role menus and permissions

| Role | Main responsibilities |
|---|---|
| Admin | Staff CRUD/status/passwords, chef categories, menu/table configuration, all restaurant management, discounts, payment, reports |
| Manager | Seating, orders, cancellations, table transfers, waiter reassignment, discounts, closing fully paid orders, reports; no taking payments or staff administration |
| Waiter | Automatic seating requests, own orders/dishes, serving, bill requests, cancellation requests and cleaning own assigned tables |
| Chef | Shared queue, own task claim/ready actions, automatic kitchen; no billing or customer-order edits |
| Cashier | Billing/payment/checkout, reports, dedicated takeaway orders and collection; no dine-in item edits or discounts |

Authorization is checked in service operations, not just in displayed menus. Caller-supplied `Cashier`/`Manager` objects do not grant authority. Login creates a thread-confined session; logout clears it. Deactivation, role change and password reset invalidate existing sessions. The last active Admin cannot be demoted or deactivated. Admins can add another Admin before changing their own role.

Automatic seating chooses the smallest adequate available table, then the least-loaded active waiter (ties use ID). The requesting waiter may therefore create a session assigned to a colleague, but can edit only their own assigned orders. Transfer/reassignment requires a Manager/Admin and a reason. Occupied tables are never released by rollback while their order is active. Checkout moves a table to CLEANING; a separate cleaning action makes it AVAILABLE. Managers can abandon an empty order with a reason; its table still goes through cleaning.

## Staff and chef setup

Admin → **5 Employees** → **3 Add**. Supply a unique positive ID, name, one of `ADMIN/MANAGER/WAITER/CHEF/CASHIER`, and a confirmed password. Option 9 sets a chef's comma-separated menu categories. Matching ignores case and outer whitespace; blank means qualified for every category. Option 1 shows waiter and chef workloads. Admin options 6 and 7 add/update dishes and tables.

The kitchen uses real Java threads and a shared synchronization monitor. Eligible pending dishes are claimed once, based on chef workload and specialization. Dish snapshots preserve original price/category/preparation details when the menu changes. Pending dishes with cancellation requests are held until the request is reviewed. No eligible chef means the dish stays PENDING. Adding dishes, logging in or completing menu operations dispatches available workers; option 25 explicitly dispatches as well. An inactive chef receives no new tasks; a task already cooking may finish. An interrupted task returns to PENDING, including after restart.

Default demo timing is **100 ms per menu preparation minute**. Override after building:

```text
java -Drkm.millisPerMinute=1000 -cp build restuarant.Main
java -Drkm.millisPerMinute=60000 -cp build restuarant.Main
java -Drkm.dataDir=demo-data -cp build restuarant.Main
```

Valid timing is 1–60000. A line with quantity >1 is treated as one batch task. Chef ready/claim controls are available for teaching the state transitions; automatic processing usually claims tasks first.

## Billing and cancellations

Money totals, discounts, taxes, balances and payments use `BigDecimal`, rounded HALF_UP to two decimal places. Existing double getters are retained only for source/data compatibility. Default tax is 5% on the discounted subtotal; this is an academic demo configuration, not a determination of applicable tax law.

Generate a bill before payment. A bill remains live while no payment has been received. Discounts (0–100%) require Manager/Admin. Cash, UPI and card are recorded as ledger entries; repeat payment for split or partial settlement. Amounts must be positive, at most two decimal places, and no more than the outstanding balance. The first payment freezes additions, cancellation and discounts even through direct service calls. An order closes only after full settlement and completion of all noncancelled dishes.

Cancellations require a reason, and a Manager/Admin approves or rejects the request with a decision reason. Only PENDING, unpaid dishes can be cancelled; no post-cooking refunds or payment reversals are implemented. Approved requests update the dish and bill totals; rejected requests release the kitchen hold. Whole-order cancellation is performed by cancelling every pending line; a zero-total bill can then close it. Empty sessions use the explicit abandonment action.

Takeaway workspace (23) is restricted to Admin/Manager/Cashier and shares the same services. The unique order ID is its pickup token. All noncancelled dishes must be READY and the bill fully paid before collection closes the order. Managers may prepare a takeaway bill but a Cashier/Admin records payment.

Reports distinguish cash receipts (including partial payments and tax), today's receipts, closed settled sales, outstanding billed balances and unbilled order subtotals. Best sellers count quantities in closed orders. Payment-method totals and transaction entries are displayed. Dates use the JVM/local operating-system date.

## Structure

```text
src/restuarant/Main.java                 integrated entry
src/restuarant/LoginDemo.java            retained input/password helpers, role menus
src/restuarant/RestaurantApplication.java shared state, lifecycle, reports, transfers
src/restuarant/auth/                     original accounts/hash/store + Access
src/restuarant/model/                    original models + CancellationRequest
src/restuarant/service/                  original services + cancellation/chef assignment
src/restuarant/billing/                  Bill and Payment
src/restuarant/persistence/              original legacy readers + SnapshotStore
 tests/restuarant/Review1Tests.java       executable Java integration/edge tests
 tests/console_smoke.py                  actual Main process workflow/restart test
```

No generated classes, credentials, live saves or `.git` repository are included in either deliverable ZIP. The unmodified upload remains your original backup. The changes-only ZIP contains changed/added files at their original relative paths; extract it into your existing repository root after backing up local work. Rebuild and launch `Main`, not old compiled classes from `out`.

## Tests

Linux/macOS: `bash test.sh`. Windows: `test.bat`. Python 3 is needed only for the console-driver test; the application and Java suite have no Python dependency. The optional argument to the Java suite is the directory containing an original legacy restaurant/account pair:

```text
java -cp build restuarant.Review1Tests
java -cp build restuarant.Review1Tests /path/to/legacy-data
python tests/console_smoke.py
```

All tests use newly created temporary directories; they do not edit your actual data. See TEST_REPORT.md and the raw output logs for executed checks, AUDIT_AND_FIX_REPORT.md for changes, and DEMO_GUIDE.md for the presentation sequence.

## Scope and operational limits

- Review 1 local console only; Swing remains Review 2. Payment entries do not contact UPI/card gateways or verify bank settlement.
- The application supports one process per data folder. UI mutations and kitchen persistence use the shared state monitor. Embedded callers extending this program must likewise coordinate compound transactions under `app.state`; domain objects are not a sandbox against arbitrary malicious code executing inside the JVM.
- Automatic saving follows successful console operations and kitchen transitions. A reported save failure keeps changes in memory and blocks UI changes until Save is retried. A crash before a successful snapshot may lose those unsaved changes. Atomic replacement has a non-atomic fallback on filesystems without atomic moves.
- The built-in hash format stays PBKDF2-HMAC-SHA256 with the original 150,000 iterations and independent random salts, preserving existing credentials. Secure terminals hide password input; IDE input fallback warns that typing is visible. No hardcoded recovery password exists.
- Serialization is version-compatible with the supplied upload and was tested, but unrelated historical formats are not guaranteed. Keep backups. Model subclasses retain their original computed serialization IDs explicitly.
- No external course-guidelines file was supplied in this request; academic scope follows the attached master prompt. The demonstration uses inheritance/abstract Employee subclasses, constructors, overriding/overloading, packages, exception handling, serialization, `Runnable`/`AutoCloseable`, real threads and synchronization.
