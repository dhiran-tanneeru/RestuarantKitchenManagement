# Review 1 demonstration guide

Use a fresh data folder for a predictable presentation. Build, then launch:

```text
java -Drkm.dataDir=demo-data -Drkm.millisPerMinute=1000 -cp build restuarant.Main
```

Choose your own temporary demonstration passwords. Do not reuse real personal passwords. The example numbers below assume a fresh dataset; follow the displayed IDs if continuing existing data.

## 1. Admin preparation

1. Complete first-time Admin setup: ID **1**, your name, a confirmed password. Log in with ID 1.
2. Open **5 Employees → 3 Add** four times: Manager 2, Waiter 3, Chef 4, Cashier 5. Add Waiter 6 and Chef 7 to demonstrate workload balancing and concurrency. Roles are entered as `MANAGER`, `WAITER`, `CHEF`, `CASHIER`.
3. **Employees → 9 Chef categories**: set chefs 4 and 7 to `Main,Drinks`, or leave blank to accept all categories. Show **1 List / workloads**. Return with 0.
4. **Dashboard → 6 Configure menu**: dish 10, `Meal`, category `Main`, price `100.00`, minutes `1`, available `yes`. Add dish 11, `Lime Soda`, category `Drinks`, price `20.00`, minutes `1`, available `yes`.
5. **7 Configure tables**: table 1 capacity 2, table 2 capacity 4, table 3 capacity 6.
6. Log out with 0. Explain that the Employee abstraction and role subclasses are preserved, while the account registry is the authority for operational eligibility.

## 2. Dine-in workflow across roles

1. Log in as Waiter 3. **8 Automatic seating → 2 guests**. Expect the smallest table, table 1, and order **1001**. Observe the assigned waiter ID. A second seating request balances to waiter 6 when loads differ; it does not necessarily assign the requesting waiter.
2. **9 Add dishes**: order 1001, dish 10, quantity 1, instructions `Less spicy`. Add another line if showing concurrent chefs. **3 Orders** and **4 Kitchen** show the shared state.
3. Log out, then log in as Chef 4. Open **4 Kitchen**: show order/item IDs, assigned chef and preparation status. The real workers run automatically; **25 Run automatic kitchen** is also available. Pending work stays pending if no qualified active chef exists.
4. Log in as the assigned waiter. **10 Serve dish → order 1001 → item 1** after READY. Serve all noncancelled ready lines. **11 Request/view bill → 1001**.
5. For one Meal: subtotal **100.00**, tax **5.00**, total **105.00**.
6. Log in as Manager 2. **17 Discount → 1001 → 10**. The total becomes **94.50**: discount 10.00; tax 4.50. The permission check lives in the billing service.
7. Log in as Cashier 5. **21 Payment → 1001 → CASH → 40.00**. Balance is **54.50**. Attempt **22 Close paid dine-in order**: the partial-payment restriction should reject it.
8. **21 Payment → 1001 → UPI → 54.50**. Balance becomes zero. **22 Close paid dine-in order → 1001** changes the order to CLOSED and table 1 to CLEANING.
9. Log in as the assigned waiter. **13 Finish cleaning → 1**. **2 Tables** shows AVAILABLE. A new seating request can now reuse it with a new unique order ID.
10. Log in as Admin/Manager. **18 Reports** shows receipts, method breakdown, outstanding balances, closed sales, best sellers and transactions. With just this sale, receipts are **94.50**, CASH **40.00**, UPI **54.50**.
11. Choose **28 Save**, log out and exit. Restart with the **same dataDir option**. Log in and show the closed order, table status, staff, payments and report again.

If you added more meal lines for concurrency, use the displayed total rather than the single-meal payment figures above.

## 3. Takeaway workflow

1. Log in as Cashier 5 and open **23 Takeaway workspace**.
2. **1 Create** returns a unique order ID/pickup token. It has no table.
3. **2 Add dishes**: use that order ID, dish 10, quantity 1. **3 Track** shows preparation and, once all dishes are ready, `Ready for collection: true`.
4. **4 Bill**: for one undiscounted Meal the total is **105.00**.
5. **5 Pay → CARD → 105.00**. **6 Collect and close** succeeds only when every noncancelled dish is ready and payment is complete.
6. **3 Track** shows the CLOSED order with COLLECTED dishes. Return with 0; **24 → 0** shows reports. After the earlier single-meal dine-in sale and this takeaway, cumulative receipts are **199.50**.
7. Exit and restart to verify takeaway history and payments persisted in the same snapshot.

## 4. Cancellation demonstration

For a reliable demonstration, prevent the item from starting before the request:

1. Admin temporarily deactivate chefs in **Employees → 7**, or create a dish in a category none of the active chefs can cook.
2. Seat a party and add two item lines. As the assigned waiter, use **12 Request cancellation** on the second pending line with a clear reason. A duplicate pending request is rejected.
3. Manager uses **14 Review cancellations**, selects the request ID, answers `yes`, and provides an approval reason. The actual dish becomes CANCELLED and the bill excludes it.
4. Request a cancellation on another pending line and reject it with a reason; the kitchen hold is released.
5. Reactivate/configure chefs and run the kitchen. Already preparing, ready, served or paid dishes cannot be cancelled in this Review 1 policy. History shows requester, reviewer, status and reasons.

## 5. Manager exceptions and negative cases

- **15 Transfer** an open unpaid dine-in order to an adequate available table; supply a reason. The old table goes to CLEANING. A smaller or occupied destination is rejected.
- **16 Reassign waiter** changes both the order and table assignment to another active waiter, with an audit reason.
- **29 Abandon empty order** handles guests leaving before they order. The table still needs cleaning; this cannot erase an order containing dishes.
- Try a duplicate employee ID, invalid waiter ID, unrelated waiter order edit, unauthorized discount/payment, payment larger than balance, and addition after payment begins. Each must fail with a validation/permission message.
- Try deactivating/demoting the last active Admin: blocked. Add a second Admin before demonstrating legitimate Admin-role changes.
- Use **19 Operational history** for transfers, overrides, staff actions, kitchen transitions and cancellation decisions. Use the automated test logs for concurrency, corrupted files and interruption cases; do not corrupt your demonstration data live.

## Talking points for the review

- OOP: `Employee` is abstract; `Chef`, `Waiter`, `Manager` and `Cashier` extend it and override role/duty methods. Encapsulated models own state checks, and services coordinate workflows.
- Money: `BigDecimal` prevents binary floating-point totals; partial and split payments share one balance.
- Threads: `ChefWorker implements Runnable`; synchronized claims avoid duplicate assignment. Saving and kitchen transitions share a state monitor.
- Files: Java serialization persists the whole graph in one snapshot; temporary-file replacement and a backup protect normal save operations.
- Security: PBKDF2 hashes with random salts; authenticated sessions; service-level role checks; disabled accounts cannot perform new actions.
- Review 2: Swing can call these services later. This delivery intentionally remains console-based.
