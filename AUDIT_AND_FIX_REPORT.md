# Audit and fix report

Source: supplied `rkm(2).zip`; requirements: supplied `Pasted text.txt`. The uploaded archive is the authoritative baseline. This report describes verified fixes, not a claim that testing proves the absence of every possible defect.

## Continuation and data preservation

All 28 original Java source paths remain, with six new classes for integration, authorization, cancellation, assignment and atomic snapshots. The package remains `restuarant`. Original order/menu/table/billing models, employee hierarchy, PBKDF2 format, service names, login input/password helpers and useful allocation logic are retained. Most domain/service changes extend existing implementations. Fixed arrays grow without changing their serialized field types.

The original unrestricted `RestaurantConsole` is now an authentication adapter; its working operations are available through integrated role menus. Placeholder role menus in `LoginDemo` were replaced because they contained no operational implementation to preserve. `Main` and both old demo entry points use the same application lifetime. `RestaurantApplication` coordinates the existing services rather than creating an unrelated restaurant implementation.

Legacy data was tested using copies of the actual uploaded `restaurant.dat` and `staff-accounts.dat`. Their bytes remained unchanged. Existing account hashes were loaded, not reset. The actual old passwords are unknown and were not guessed; password compatibility is established by retaining the original hash/salt format and testing new credentials through save/restart. The unified migrated snapshot was also saved and reopened successfully.

## Known issues investigated and resolved

Paths below are under `src/restuarant/` unless otherwise stated. Test names refer to executable checks in `tests/restuarant/Review1Tests.java` and `tests/console_smoke.py`.

| # | Original problem / root cause | Affected files | Implemented change | Executed verification |
|---|---|---|---|---|
| 1 | Main started the unrestricted console without a login | Main, RestaurantConsole, LoginDemo | All public entry points route to one login/application lifecycle | Actual Main console role login/workflow test; logged-out service rejection |
| 2 | Manager, Chef and Cashier menus only displayed placeholders | LoginDemo | Working role menus for oversight/cancellation/discount, cooking and settlement | All five roles exercised through actual Main; Java permission tests |
| 3 | Admin restaurant-management option printed a future-stage message | LoginDemo, MenuService, OrderServices | Staff/menu/table configuration, workloads, reports, operations/history | Console staff/menu/table setup; menu edit and capacity tests |
| 4 | Payment guard was only in console; service callers could add dishes | Order, OrderServices, Bill, BillingService | First payment locks the order; model and service mutations reject further additions; discount/cancellation checks use the same lifecycle | Direct service and direct model add after payment; concurrent payment/addition |
| 5 | Public table rollback could release a table with an active order | RestuarantTable, OrderServices, DiningSessionService | Link active order to table; rollback and cleaning reject active orders; links rebuilt on migration | Active-order rollback and cleaning rejected |
| 6 | No reachable cleaning completion workflow | OrderServices, LoginDemo | Explicit assigned-waiter/Manager/Admin cleaning action; checkout goes to CLEANING first | Checkout, cleaning, table reuse in Java and console workflows |
| 7 | Hardcoded Employee records and login-account status were separate authorities | RestaurantState, RestaurantApplication, KitchenService, Access | Single account registry controls current role and eligibility; old employee records remain historical compatibility data | Deactivation/reactivation, role change, previously authenticated disabled user rejection |
| 8 | Manual seating validated only a positive waiter number | RestuarantTable, RestaurantApplication, OrderServices | Canonical active WAITER validation; Manager/Admin manual seating; coordinated order creation | Unknown/inactive/wrong-role waiter cases; failed seating leaves table unchanged |
| 9 | No specialization/workload assignment integrated with workers | ChefAssignmentService, KitchenService, StaffAccount | Category qualification, lowest active workload, deterministic tie-break, real worker threads | Overlapping preparation by two chefs; category and inactive-chef tests |
| 10 | Takeaway was only an order type without a complete role workspace | LoginDemo, RestaurantApplication, OrderServices | Dedicated authorized console workspace sharing existing menu/kitchen/billing and unique order IDs | Table-free order → kitchen → payment → collection → CLOSED through Java and Main |
| 11 | Manager approval method only printed a success message | CancellationService, CancellationRequest, OrderItem, Manager | Persistent request/review records; real state mutation, validation, kitchen hold/release. Ambiguous legacy order-only approval rejects instead of lying | Approval changes actual dish; rejection releases hold; repeated/unauthorized review rejected |
| 12 | No complete revenue/transaction/outstanding reports | RestaurantApplication, BillingService | Separate receipt/settled-sale/outstanding/unbilled metrics, method/date totals, transactions and best sellers | Exact expected receipt totals and method totals; persisted console report after restart |
| 13 | Operations did not consistently save | LoginDemo, RestaurantApplication, SnapshotStore, KitchenService | Save after successful UI actions and kitchen transitions; full shared snapshot; reported save failure blocks UI mutation until retry | Repeated save, forced failure, retry, console exit/restart |
| 14 | Interrupted chef job could remain PREPARING forever | ChefWorker, KitchenService, RestaurantState | Requeue interrupted tasks; recover serialized PREPARING jobs on startup; undo claim if persistence fails | Interrupt worker; crash-style saved PREPARING reload; resumed cooking; injected failed claim save |
| 15 | Staff/menu/order/line/bill/payment arrays had hard caps | StaffService, MenuService, OrderServices, Order, Bill, BillingService | Capacity doubles when needed; serialized array field types retained | Staff/menu beyond 100, order lines beyond 100, orders beyond 500, payments beyond 20 |
| 16 | Service calls trusted provided employee objects rather than authenticated identity | Access, StaffService, Order, BillingService, CancellationService, KitchenService | Thread-local authenticated canonical account, per-action active/role checks, ownership validation | Forged Cashier cannot authorize payment; waiter ownership and logged-out access rejected |
| 17 | Disconnected lifecycles/demos could construct different operational instances | Main, LoginDemo, RestaurantConsole, TableAllocationDemo, RestaurantApplication | One loaded state for all dashboards; compatibility entry adapters; process lock | Actual role-to-role shared workflow; second process instance rejected |
| 18 | Unreadable account/state handling could invite resets; serialization evolution risk | StaffAccount, RestaurantState, SnapshotStore, AccountStore, FileManager, employee subclasses | Fail closed, retain legacy files, single snapshot/backup, restricted deserialization, original subclass serial IDs pinned explicitly | Actual legacy import and reopen; corrupt accounts/unified state rejected without fallback/reset |
| 19 | ZIP included generated class files, live credentials/data and Git internals | Packaging, .gitignore | Deliver source/tests/docs/scripts only; leave original archive/data intact | ZIP manifest/content checks; changes-only overlay compared with full project |
| 20 | No adequate automated regression suite | tests, build/test scripts | Executable Java suite plus actual console process driver; raw output retained | See TEST_REPORT.md and logs for individual outcomes |

## Additional issues fixed during independent inspection

| Problem and root cause | Files / fix | Verification |
|---|---|---|
| Eight whitespace characters were accepted as a new password | StaffAccount rejects blank passwords while retaining existing salted hash format | Blank password rejection; reset old/new password checks; no plaintext passwords in snapshot |
| Last Admin could be deactivated via service/model setter | StaffService and attached StaffAccount validate remaining active Admin count | Last Admin deactivation and demotion rejected |
| Old sessions remained valid after role/reset/status changes | StaffAccount credential version captured in Access session; checked on every protected action | Previously logged-in inactive user rejected; login uses new role/reset password |
| Menu edits could retroactively alter dish category/name/preparation details | OrderItem takes a menu snapshot and exact price when added | Price and special-instruction assertions; live menu edit tests |
| Binary floating-point monetary subtotal and fixed precision mixing | Exact BigDecimal price/line/subtotal paths; compatibility double fields retained for serialization/API | 300.30 subtotal, 10% discount and 283.78 rounded tax-inclusive total |
| Fabricated Order with an existing ID could be associated with another bill | BillingService binds canonical order registry and validates object identity | Duplicate-ID fabricated order rejected |
| Add/payment race lacked a shared lock | Order mutations and Bill payment/discount use the order monitor; payment IDs and order creation synchronized | Competing payment/addition test leaves consistent locked order |
| Null order lookup could fail before meaningful validation | BillingService validates null before permission/ownership checks | Null bill order rejected with validation exception |
| Empty seating sessions had no exit path | Explicit Manager/Admin abandon-empty operation with reason and cleaning transition | Empty-session abandonment test |
| Unsafe source-table release during transfers | Validate destination first, move order, then release original table into cleaning | Transfer preserves order association and cleaning requirements |
| Password/staff changes and operations lived in separate save files | New snapshot contains both; original split files are migration inputs only | Restart retains staff, account states, orders, bills, payments and requests together |
| Multiple processes could overwrite the same data directory | FileChannel lock held for whole application lifetime | Second instance rejected |
| Thread startup order could make a higher-ID chef exit before the lower-ID chef claimed work | ChefWorker waits briefly while eligible pending work remains; KitchenService distinguishes no-work from not-yet-selected | Reversed chef insertion/start order and overlapping preparation timestamps |
| Save failure while claiming a kitchen item could strand in-memory PREPARING | Recover the item to PENDING before propagating a failed claim save | Injected claim persistence failure followed by PENDING assertion |

## Design policies and remaining scope limits

- Pending-only, unpaid cancellation is intentional. Post-cooking cancellations, refunds, voided payments and chargebacks are outside this academic Review 1 scope.
- Manager/Admin may discount; Admin/Cashier may record payments. A Manager may close an already settled order. These are explicit permission choices, not silent bypasses.
- Existing personal saves are intentionally absent from distribution ZIPs. Follow README to copy the original data pair before first startup; no hidden default credentials or reset is supplied.
- Low-level model objects remain ordinary Java teaching objects. This is a local trusted-process application, not an isolation sandbox for hostile code with JVM access. UI compound operations and kitchen saves coordinate on the state monitor; extensions must follow that convention.
- Atomic rename falls back when unsupported. A reported save failure requires retry; abrupt termination before a successful snapshot can lose unsaved changes.
- Runtime tested on OpenJDK 17/Linux. Java 8 API/source compatibility was checked with `--release 8`; Windows batch launchers and an actual Java 8 runtime were not executed here.
- No real payment-gateway integration, GUI, stock/procurement module, multi-branch architecture or database is represented as implemented. These were not required for this console Review 1.
- Original compatibility Employee duty methods remain classroom illustrations. The obsolete Manager order-only cancellation method now refuses the operation; real approvals require a persistent cancellation request through CancellationService.

The supplied archive contained no additional AGENTS.md and no official course PDF in this request. The attached master prompt governed the implementation and verification scope.
