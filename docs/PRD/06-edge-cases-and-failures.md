# Part 6: Edge Cases & Failure Handling

---

> Every section below describes a specific failure scenario, its root cause, detection mechanism, impact, and prescribed resolution. This is the section that prevents "it works on my machine" from reaching production.

---

## 1. Payment Edge Cases

### 1.1 Payment Succeeds but Order Not Created

```
Root cause: Network failure between Stripe confirming payment and BAH backend creating order.
Detection:  Stripe webhook `payment_intent.succeeded` arrives but no matching order in DB.
Impact:     Customer charged but sees no order. Critical trust issue.

Resolution:
  1. Webhook handler checks: does an Order with this PaymentIntent ID exist?
  2. If NO:
     a. Log CRITICAL alert: "Orphaned payment: {paymentIntentId}"
     b. Attempt recovery:
        - Read PaymentIntent metadata (contains cart details, customer info, referral code)
        - Reconstruct order from metadata
        - Create Order with status CONFIRMED (skipping PENDING_PAYMENT)
        - Reserve stock (if available)
        - If stock unavailable: create order in FULFILLMENT_ISSUE state,
          admin manually resolves
     c. If auto-recovery fails: alert admin within 5 minutes (PagerDuty/email)
     d. Admin manually creates order or processes refund
  
  Prevention:
    - Store full cart + customer details in Stripe PaymentIntent metadata  
      (limited to 500 chars per key, use multiple keys if needed)
    - Implement outbox pattern: write order to local DB first, then confirm with Stripe
    - Webhook retry: Stripe retries failed webhooks for up to 72 hours
    - Idempotency check: webhook handler is idempotent (processing same event twice is safe)
```

### 1.2 Duplicate Charges

```
Root cause: Customer double-clicks "Pay" or retry without proper idempotency.
Detection:  Two PaymentIntents for the same order.
Impact:     Customer charged twice for one order.

Prevention:
  1. Use order UUID as Stripe idempotency key when creating PaymentIntent.
     → Stripe returns the same PaymentIntent on retry, not a new one.
  2. Frontend: disable "Pay" button on first click, show loading spinner.
  3. Backend: before creating PaymentIntent, check if one already exists for this order.
     If yes, return existing client_secret.

Resolution (if prevention fails):
  1. Detect via webhook: two `payment_intent.succeeded` events for same order email.
  2. Auto-refund the second charge.
  3. Log incident for engineering review.
```

### 1.3 Webhook Delays

```
Root cause: Stripe webhook delivery delayed (Stripe infrastructure issue, BAH server down).
Detection:  Order stuck in PENDING_PAYMENT for > 5 minutes despite customer seeing success.
Impact:     Customer sees "Payment processing..." indefinitely. Order not confirmed.

Resolution:
  1. Frontend polling: after Stripe confirms payment on client-side, frontend polls
     GET /api/v1/orders/{orderId}/status every 5 seconds for up to 2 minutes.
  2. If status still PENDING_PAYMENT after 2 minutes:
     - Show: "Your payment was received! We're confirming your order. 
              You'll receive a confirmation email shortly."
     - Backend log: flag order for manual webhook reconciliation.
  3. Backend reconciliation job (runs every 10 minutes):
     - Find orders PENDING_PAYMENT for > 10 minutes.
     - Check Stripe API directly: GET /v1/payment_intents/{id}
     - If Stripe says `succeeded`: update order to CONFIRMED, create commission entries.
     - If Stripe says `requires_payment_method`: still waiting on customer, no action.
     - If Stripe says `canceled`: cancel BAH order, release stock.
  4. Webhook retry handling:
     - BAH must return 2xx to Stripe within 20 seconds.
     - If BAH returns 5xx or timeout, Stripe retries with exponential backoff.
     - Webhook handler MUST be idempotent: check if order already CONFIRMED before processing.
```

### 1.4 Currency Mismatch

```
Root cause: Exchange rate changes between order creation and payment confirmation.
Detection:  Charged amount (in foreign currency) doesn't match expected amount.
Impact:     Platform receives slightly more or less than expected.

Resolution:
  - Exchange rate is LOCKED at order creation time (stored in Order.exchangeRateUsed).
  - Stripe charges the exact amount specified in the PaymentIntent (in customer's currency).
  - Platform absorbs minor exchange rate fluctuations (typically < 1%).
  - For large orders: admin can review if variance > 2% and adjust if needed.
  - This is a known trade-off: locking the rate provides price certainty to the customer.
```

---

## 2. Inventory Edge Cases

### 2.1 Overselling

```
Root cause: Race condition where two orders reserve the last unit simultaneously.
Prevention: SELECT ... FOR UPDATE with row-level lock (see Part 5).

If it still happens (bug, manual stock adjustment error):
  Detection: available_stock < 0 for any variant (monitoring alert).
  
  Resolution:
    1. Identify which orders caused the oversell (stock audit log).
    2. Prioritize by timestamp: first order to commit keeps the item.
    3. Contact second customer:
       - Option A: "Item available in [X days] — would you like to wait?"
       - Option B: "Full refund + 10% discount code for future purchase."
    4. Adjust stock to 0 (not negative).
    5. Root cause analysis: fix the code path that allowed negative stock.
```

### 2.2 Vendor Out-of-Stock After Order (Consignment)

```
Root cause: System shows stock available, but physical item is damaged/missing at warehouse.
Detection:  Admin discovers during picking/packing.

Resolution: (detailed in Part 5, Section 3.2)
  - Admin marks FULFILLMENT_ISSUE on order item.
  - Options: substitute, backorder, cancel item, cancel order.
  - Customer notified proactively.
  - Stock reconciliation triggered.
```

### 2.3 Stock Count Drift

```
Root cause: System stock count diverges from physical count over time due to unlogged 
            events (theft, damage, miscounts).
Detection:  Monthly physical stock count reveals discrepancies.

Prevention:
  1. Every stock mutation MUST go through the stock audit log (no direct DB updates).
  2. Barcode/QR scanning for receiving and picking (Phase 2).
  3. Monthly reconciliation process:
     a. Admin performs physical count.
     b. Admin enters physical counts in reconciliation UI.
     c. System compares: expected vs actual.
     d. Discrepancies flagged for review.
     e. Admin creates ADJUSTMENT entries with reason (DAMAGE, THEFT, MISCOUNT, etc.).
     f. Stock levels corrected.
```

---

## 3. Referral Edge Cases

### 3.1 Multiple Referral Conflicts

```
Scenario: Tourist scans Hotel A QR on day 1, Guide B QR on day 3, purchases on day 5.
Resolution: Guide B gets attribution (last-touch policy).

Scenario: Tourist has Hotel A cookie, visits BAH.com directly (no ref param), purchases.
Resolution: Hotel A gets attribution (cookie persists for 30 days).

Scenario: Tourist has Guide A cookie on phone. Switches to laptop to purchase. No cookie on laptop.
Resolution: No attribution. Order is organic. This is an accepted loss.

Scenario: Tourist enters someone else's referral code manually in URL bar.
Resolution: Accepted. The system treats this as a valid referral. Fraud detection (see 
            Part 4) monitors for anomalies. This scenario is rare and low-risk.
```

### 3.2 Missing Referral Data

```
Scenario: Cookie blocked by browser privacy settings (e.g., Safari's ITP).
Detection:  No bah_ref cookie present at checkout.

Resolution:
  1. Primary: cookie
  2. Fallback 1: localStorage (not affected by ITP for same-origin)
  3. Fallback 2: sessionStorage (shortest-lived, covers single session)
  4. If ALL storage mechanisms blocked:
     → Order created without referral attribution.
     → Logged as "referral_lost" event for analytics.
     → Admin can manually attribute within 48 hours if partner contacts them with evidence.

Manual attribution rules:
  - Admin can add referral attribution to an order within 48 hours of creation.
  - Requires documented evidence (e.g., partner confirms they shared link with the tourist).
  - Logged in audit trail with admin's justification.
  - This should be RARE (< 1% of orders).
```

### 3.3 Partner Account Issues

```
Scenario: Partner's referral code used but partner has been SUSPENDED.
Resolution:
  - Click tracking: logged with SUSPENDED flag. No error shown to tourist.
  - At checkout: referral NOT attributed (partner suspended). Order created without referral.
  - Previously attributed orders (before suspension) remain attributed.

Scenario: Partner's bank account changed between earning and settlement.
Resolution:
  - Partner can update bank details in their portal at any time.
  - Settlement process uses the bank details AT SETTLEMENT TIME, not at earning time.
  - Admin verifies updated details before approving payout.
```

---

## 4. Order Edge Cases

### 4.1 Cancellation After Shipping

```
Scenario: Customer requests cancellation but order is already SHIPPED.
Rules:
  - Customer CANNOT cancel after SHIPPED status.
  - Customer CAN request RETURN after delivery (within return window).
  
Resolution:
  1. Inform customer: "Your order has already shipped. Tracking: [link]. 
     Once delivered, you can request a return within 30 days."
  2. If customer insists (e.g., moving to different country):
     a. Admin can attempt to recall shipment (often not possible with Bhutan Post).
     b. If recall successful: process as cancellation + refund.
     c. If recall fails: customer must receive and return.
     d. Exception: admin discretion for VIP customers or exceptional circumstances.
```

### 4.2 Partial Refunds

```
Scenario: Multi-item order. Customer wants to return only 1 of 3 items.

Resolution:
  1. Admin processes partial refund via Stripe for the specific item amount.
  2. OrderItem for returned item → RETURNED.
  3. Other items' status unchanged.
  4. Order overall status remains DELIVERED (not fully refunded).
  5. Commission ledger: only the returned item's commissions reversed.
     Vendor, partner, and platform commissions for UNRETURNED items unaffected.
  6. Shipping cost: NOT refunded for partial returns (unless BAH error).
  
Edge case within partial refund:
  - Item A: CONSIGNMENT (vendor Pema). Refunded.
  - Item B: OWNED. NOT refunded.
  - Only Pema's vendor commission is reversed. No impact on Item B's financials.
```

### 4.3 Order Stuck in Processing

```
Scenario: Admin forgets to update order status. Order stays in PROCESSING for days.
Detection: Scheduled job flags orders in PROCESSING for > 48 hours.

Resolution:
  1. Admin notification: "Order BAH-XXXX has been in PROCESSING for 3 days. Please update."
  2. Repeated notifications every 24 hours.
  3. After 7 days: escalate to SUPER_ADMIN.
  4. Customer notification after 72 hours: 
     "Your order is being prepared. We'll update you with tracking information soon."
```

---

## 5. Logistics Edge Cases

### 5.1 Shipping Failure

```
Scenario: Shipping provider reports package lost or returned to sender.
Detection: Tracking status shows "RETURNED_TO_SENDER" or no update for > 30 days.

Resolution:
  1. Admin contacts shipping provider for investigation.
  2. If confirmed lost:
     a. Reship if item is in stock.
     b. If out of stock: full refund.
     c. File insurance claim if shipping insurance was purchased.
  3. If returned to sender (customs rejection, wrong address):
     a. Contact customer to verify/update address.
     b. If address issue: reship to corrected address (one retry).
     c. If customs rejection: refund minus shipping cost. Advise customer on customs rules.
```

### 5.2 International Delays

```
Scenario: International shipment delayed beyond estimated window (common for Bhutan Post).
Detection: Estimated delivery date passed, tracking shows "IN TRANSIT" for > 7 days past ETA.

Resolution:
  1. Proactive notification to customer after ETA + 5 days:
     "Your package is still in transit. International shipments from Bhutan occasionally 
     experience customs delays. Current tracking: [link]. We'll continue monitoring."
  2. After ETA + 14 days with no tracking update:
     a. Admin contacts shipping provider.
     b. If unresolved after 30 days total:
        Customer offered: reship (if in stock) or full refund.
  3. Set customer expectations upfront:
     - Product pages and checkout clearly state estimated delivery ranges (not dates).
     - Confirmation email includes: "International delivery estimates are approximate. 
       Customs processing may add additional time."
```

### 5.3 Customs Duties

```
Scenario: Customer must pay customs duties on arrival — not aware, refuses package.
Prevention:
  1. Checkout page disclaimer: "Import duties and taxes may apply upon arrival in your 
     country. These are the buyer's responsibility."
  2. Order confirmation email repeats this notice.
  3. Product pages mention: "Shipped from Bhutan. Customs fees may apply."

Resolution (if package refused):
  - Package returned to sender (BAH warehouse).
  - Admin attempts to contact customer.
  - If customer wants redelivery: reship with DDP (Delivered Duty Paid) at customer's additional cost.
  - If customer refuses: refund minus original + return shipping costs.
```

---

## 6. User Edge Cases

### 6.1 Guest Checkout

```
Design decision: Guest checkout is MANDATORY to support (not just "nice to have").
Rationale: Tourists are time-constrained and will abandon if forced to create an account.

Implementation:
  - Guest provides: email, name, shipping address, phone.
  - Order linked to guest email (not a User account).
  - Guest can track order via: /track?order={orderNumber}&email={guestEmail}
  - If guest later creates account with same email:
    → System associates past guest orders with the new account.
    → Merge is automatic, based on email match.
    → No duplicate orders.
  
Edge cases:
  - Guest uses different email for each order: no consolidation possible. Accepted trade-off.
  - Guest typos in email: no confirmation email received. /track page still works if they 
    remember the order number. Support can look up by shipping address.
  - Guest vs registered user with same email purchasing simultaneously:
    → If logged in: order linked to User account.
    → If guest: linked to email. On merge, both orders visible.
```

### 6.2 Fraud Behavior

```
Detection signals:
  - Multiple orders to different addresses from same IP in short timeframe.
  - Stolen credit card: Stripe's Radar flags high-risk charges.
  - Referral fraud: partner placing orders through own referral link.
  - High-value order from new email with express shipping (common fraud pattern).

Prevention:
  1. Stripe Radar: enabled for all charges. Auto-block high-risk.
  2. Velocity check: max 3 orders from same IP per hour.
  3. Order review queue: orders > $500 USD auto-flagged for admin review before fulfillment.
  4. Self-referral detection: match purchaser email against all partner emails.
  5. Address blacklist: admin can blacklist known fraudulent shipping addresses.

Response to confirmed fraud:
  - Refund via Stripe.
  - Block email / IP (admin action).
  - Report to Stripe for chargeback prevention.
  - Partner deactivation if referral fraud.
```

### 6.3 Account Security

```
Scenario: Customer's account compromised (password leaked, unauthorized order placed).
Detection: Customer reports unauthorized order. Admin sees order from unusual location.

Resolution:
  1. Admin suspends account immediately.
  2. Cancel any unshipped orders.
  3. Refund unauthorized charges.
  4. Force password reset via email.
  5. Review: was any personal data accessed? If so, notify customer per privacy policy.
  6. Log incident in security audit trail.
```
