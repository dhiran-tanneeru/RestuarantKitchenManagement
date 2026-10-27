# Review 1 test report

## Final executed result

- **Compilation: PASS.** All 34 application classes plus the Java regression suite compiled together on OpenJDK 17.0.20 with Java 8 API/language targeting, all lint checks and warnings treated as errors: `--release 8 -Xlint:all -Werror`.
- **Java regression suite: 126 PASS, 0 FAIL in the final run.** Tests call the actual models/services and shared application, including real threads, file writes and reloads. These are individual checks, not 126 separate test classes.
- **Actual console integration: PASS.** A Python stdlib driver ran `restuarant.Main` as a separate process, performed first-time setup and all five role logins, configured staff/menu/tables, seated customers, prepared/served dishes, discounted/billed/partially paid/settled/closed an order, cleaned its table, completed a takeaway, exited, started a new process and verified persistent totals. A second assertion found no `Operation failed:` messages in that transcript.
- **Original uploaded legacy pair: PASS.** Imported copies of the supplied restaurant/account files, saved a unified snapshot, reopened it and confirmed both source files remained byte-for-byte unchanged. Existing user passwords were not known or guessed; account hashes/records were preserved.

Raw evidence: `test-output.txt`, `console-test-output.txt` and `console-transcript.txt`. The transcript is captured output, not simulated successful responses; the driver does not echo its input passwords. Test credentials in the source are synthetic and belong only to temporary test datasets.

## Failure investigated before the final run

An early test expected two very short jobs to use different chefs. The first diagnosis was a timing-dependent assertion; longer tasks and overlapping timestamp checks revealed a genuine startup-order issue: a higher-ID chef could see another chef selected and exit before that chef claimed its task. ChefWorker now waits briefly while eligible pending work remains. The final suite inserts chefs in reverse ID order and verifies overlapping preparation on two distinct real threads. The suite was rerun after that fix and after the final compatibility-discount change; final results above include those changes.

Independent review also found that a save exception during claim could strand a PREPARING dish before the worker received its reference. The claim now recovers the dish before propagating the exception; the injected-failure test passes.

## Scope and limits of verification

- Java 8 compatibility was compiler-checked, but execution was on Java 17/Linux. An actual Java 8 VM, Windows batch execution and VS Code terminal behavior were not available for runtime testing here.
- Existing legacy account passwords were not tested because they were not supplied. Newly created and reset credentials were tested before and after persisted reload.
- UPI/card/cash tests validate the local payment ledger, not bank/gateway transactions. No gateway integration is claimed.
- Concurrency tests exercise overlapping chefs, competing claims, payment/add contention, interruption and recovery. They are finite tests, not proof against every possible scheduling interleaving.
- Corruption and save-failure tests operate on isolated temporary files. They do not simulate every disk/controller/power-loss failure mode. Atomic-move fallback behavior depends on the user's filesystem.
- There are no known failing assertions in the delivered suite. Product-scope limits (pending-only cancellation, no refunds, one process per data folder, no Swing) are documented in README and the audit.

## Reproduce

Run `bash test.sh` on Linux/macOS, or `test.bat` on Windows with a JDK and Python 3. To include the actual legacy-data checks, pass a directory holding `restaurant.dat` and `staff-accounts.dat` to the Java test main. These files are deliberately not bundled in the clean source ZIP.

```text
bash build.sh
java -cp build restuarant.Review1Tests /path/to/original/data
python tests/console_smoke.py
```

## Individual checks from final execution

| # | Result | Executed check |
|---|---|---|
| 1 | PASS | First-time Admin creation |
| 2 | PASS | Incorrect password |
| 3 | PASS | Invalid employee login |
| 4 | PASS | Blank password even with eight spaces |
| 5 | PASS | Login role ID 1 |
| 6 | PASS | Login role ID 2 |
| 7 | PASS | Login role ID 3 |
| 8 | PASS | Login role ID 4 |
| 9 | PASS | Login role ID 5 |
| 10 | PASS | Login role ID 6 |
| 11 | PASS | Login role ID 7 |
| 12 | PASS | Duplicate account ID |
| 13 | PASS | Last active Admin deactivation |
| 14 | PASS | Last active Admin demotion |
| 15 | PASS | Inactive account login |
| 16 | PASS | Account reactivation |
| 17 | PASS | Employee rename |
| 18 | PASS | Chef specialization stored |
| 19 | PASS | Duplicate table ID |
| 20 | PASS | Duplicate menu ID |
| 21 | PASS | Menu edit and availability |
| 22 | PASS | Smallest suitable table and least-loaded waiter |
| 23 | PASS | Waiter workload balancing |
| 24 | PASS | Unrelated waiter order mutation |
| 25 | PASS | Waiter cannot create takeaway |
| 26 | PASS | Waiter cannot manage menu |
| 27 | PASS | Active-order rollback forbidden |
| 28 | PASS | Active-order cleaning forbidden |
| 29 | PASS | Occupied table reuse |
| 30 | PASS | Invalid quantity |
| 31 | PASS | Invalid menu item |
| 32 | PASS | Unavailable dish |
| 33 | PASS | Invalid manual waiter ID |
| 34 | PASS | Manual seating wrong staff role |
| 35 | PASS | No suitable table |
| 36 | PASS | No active waiter |
| 37 | PASS | Failed seating does not occupy table |
| 38 | PASS | Unsafe capacity edit |
| 39 | PASS | Table transfer preserves order and cleans old table |
| 40 | PASS | Waiter reassignment synchronized |
| 41 | PASS | Transfer source reusable after cleaning |
| 42 | PASS | Duplicate pending cancellation |
| 43 | PASS | Waiter cannot approve cancellation |
| 44 | PASS | Cancellation approval changes actual dish |
| 45 | PASS | Repeated cancellation decision |
| 46 | PASS | Cancellation rejection releases hold |
| 47 | PASS | Shared automatic kitchen completes eligible dishes |
| 48 | PASS | Two real chef threads overlap preparation |
| 49 | PASS | Preparation timing and special instructions retained |
| 50 | PASS | Cannot cancel prepared dish |
| 51 | PASS | Exact decimal subtotal |
| 52 | PASS | Unauthorized waiter discount |
| 53 | PASS | Unauthorized direct payment service |
| 54 | PASS | Forged Cashier object cannot authorize payment |
| 55 | PASS | Tax after discount with decimal rounding |
| 56 | PASS | Discount above 100 |
| 57 | PASS | Partial payment prevents closure |
| 58 | PASS | Overpayment |
| 59 | PASS | Zero payment |
| 60 | PASS | Payment fraction beyond paise |
| 61 | PASS | Direct service add after payment |
| 62 | PASS | Direct model add after payment |
| 63 | PASS | Discount frozen after partial payment |
| 64 | PASS | Cancellation after payment |
| 65 | PASS | Split payment balance reaches zero |
| 66 | PASS | Repeated payment on fully paid bill |
| 67 | PASS | Paid served order closes to cleaning |
| 68 | PASS | Closed order cannot reopen |
| 69 | PASS | Checkout cleaning and reuse |
| 70 | PASS | Reused table receives unique new order |
| 71 | PASS | Takeaway requires no table and has unique token |
| 72 | PASS | Collect unpaid takeaway |
| 73 | PASS | Takeaway kitchen payment collection complete |
| 74 | PASS | Revenue report reconciles receipts |
| 75 | PASS | No qualified chef leaves task pending |
| 76 | PASS | Inactive chef gets no task |
| 77 | PASS | Chef activation and category dispatch |
| 78 | PASS | Repeated atomic save creates backup |
| 79 | PASS | Cancellation and audit persisted in shared state |
| 80 | PASS | Logged-out service mutation |
| 81 | PASS | Menu grows beyond 100 |
| 82 | PASS | Order grows beyond 100 lines |
| 83 | PASS | Order registry grows beyond 500 |
| 84 | PASS | All generated order IDs unique |
| 85 | PASS | Login after restart |
| 86 | PASS | Paid takeaway survives reload |
| 87 | PASS | Payments survive reload |
| 88 | PASS | Cancellation history survives reload |
| 89 | PASS | Continue operations after loading |
| 90 | PASS | Loaded payment lock still enforced |
| 91 | PASS | Second application cannot concurrently use same data |
| 92 | PASS | Corrupt snapshot fails closed |
| 93 | PASS | Worker actually entered preparing |
| 94 | PASS | Crash reload reconstructs interrupted task |
| 95 | PASS | Interrupted worker requeues task |
| 96 | PASS | Recovered task cooks after restart |
| 97 | PASS | Uploaded legacy saves load without resetting accounts |
| 98 | PASS | Original legacy restaurant bytes unchanged |
| 99 | PASS | Legacy accounts unchanged |
| 100 | PASS | Migrated combined snapshot loads again |
| 101 | PASS | Corrupt unified save does not fall back to stale legacy |
| 102 | PASS | Corrupt accounts do not trigger Admin reset |
| 103 | PASS | Save failure propagated |
| 104 | PASS | Application blocks further changes after save failure |
| 105 | PASS | Explicit save retry recovers |
| 106 | PASS | Empty-session abandonment follows cleaning workflow |
| 107 | PASS | Bill null order rejected |
| 108 | PASS | Legacy predefined discount respects manager/admin policy |
| 109 | PASS | Fabricated duplicate order cannot obtain canonical bill |
| 110 | PASS | Empty order cannot be billed |
| 111 | PASS | Duplicate payment ID |
| 112 | PASS | Payment storage grows beyond 20 entries |
| 113 | PASS | Fully paid unfinished dish still prevents order closure |
| 114 | PASS | Concurrent claims cannot duplicate a dish |
| 115 | PASS | Chef manual ready works for assigned dish |
| 116 | PASS | Chef cannot impersonate another chef |
| 117 | PASS | Chef cannot approve cancellation directly |
| 118 | PASS | Previously logged-in deactivated employee cannot act |
| 119 | PASS | Old password rejected after reset |
| 120 | PASS | New reset password authenticates |
| 121 | PASS | Changed employee role used by login |
| 122 | PASS | Staff registry grows beyond 100 |
| 123 | PASS | Kitchen claim save failure is reported |
| 124 | PASS | Failed claim persistence returns dish to pending |
| 125 | PASS | Snapshot contains no plaintext test passwords |
| 126 | PASS | Concurrent payment/addition leaves locked consistent order |
