# Part 5: Inventory Model

---

## 1. Owned vs Consignment — Detailed Logic

### 1.1 Lifecycle Comparison

```
OWNED INVENTORY LIFECYCLE:
──────────────────────────
  1. Admin identifies product → negotiates wholesale price with artisan/supplier
  2. Platform purchases N units at wholesale → pays artisan upfront
  3. Stock received at BAH warehouse → admin logs receipt in system
  4. Admin creates product listing (sourcingType = OWNED, costPriceBTN = wholesale cost)
  5. Platform bears ALL risk: if unsold, it's platform's loss
  6. On sale: revenue = sale price. COGS = cost price. Margin = sale - cost - partner commission.
  7. Restocking: platform re-purchases when stock runs low

  Financial treatment:
    - Inventory appears as platform ASSET on balance sheet
    - Revenue = full sale price
    - COGS = wholesale cost
    - Gross margin = revenue - COGS
    - Net margin = gross margin - partner commission (if referral)


CONSIGNMENT INVENTORY LIFECYCLE:
─────────────────────────────────
  1. Admin identifies product → negotiates consignment terms with vendor
  2. Terms: vendor supplies product, platform sells, platform keeps X% commission
  3. Typical split: vendor 65–75%, platform 25–35%
  4. Vendor delivers N units to BAH warehouse (vendor retains ownership)
  5. Admin creates product listing (sourcingType = CONSIGNMENT, commissionRate = 0.30)
  6. VENDOR bears unsold risk (but platform must handle/store responsibly)
  7. On sale: platform collects full price, owes vendor their share
  8. Restocking: admin requests vendor to supply more units
  9. Unsold stock: vendor can request return after agreed period (e.g., 6 months)
  
  Financial treatment:
    - Inventory is NOT a platform asset (vendor ownership)
    - Revenue = commission amount only (platform's share)
    - No COGS (platform didn't purchase)
    - Platform margin = commission - partner referral commission
    - Vendor payout = sale price × (1 - commissionRate)
```

### 1.2 Stock Management Rules

| Rule | Owned | Consignment |
|------|-------|-------------|
| **Initial stock entry** | Admin logs receipt after platform purchases | Admin logs receipt when vendor delivers to warehouse |
| **Stock location** | Always at BAH warehouse | BAH warehouse (transferred by vendor) |
| **Restock trigger** | Admin monitors low stock, initiates purchase | Admin sends ConsignmentRequest to vendor |
| **Restock flow** | Purchase order → vendor delivers → admin receives → stock updated | ConsignmentRequest → vendor ships → admin receives → stock updated |
| **Unsold stock** | Platform's problem (markdown, donate, or absorb loss) | Vendor can request return; platform returns unsold units after agreed holding period |
| **Damage/loss at warehouse** | Platform bears cost | Platform bears cost (insurance recommended for high-value items) |
| **Price changes** | Platform can change freely (it owns the inventory) | Price changes require admin decision; vendor's share changes proportionally |
| **Removal from catalog** | Admin archives product | Admin archives product; notifies vendor; returns remaining stock |

### 1.3 Consignment Request Flow

```
ConsignmentRequest (admin requests vendor to supply more stock):

  ┌──────────────┐
  │   REQUESTED   │ ← Admin creates request: vendor X, product Y, quantity N
  └──────┬───────┘
         │
    Vendor notified (SMS/WhatsApp)
         │
         ▼
  ┌───────────────────┐
  │ SHIPPED_BY_VENDOR  │ ← Vendor confirms shipment (or admin updates on vendor's behalf)
  └──────┬────────────┘
         │
    Vendor ships to BAH warehouse
         │
         ▼
  ┌──────────┐
  │ RECEIVED  │ ← Admin inspects delivery, confirms quantity and quality
  └────┬─────┘
       │
  Quality check:
       │
  ┌────┴──────────────────────────────┐
  │                                   │
  ▼                                   ▼
All units OK                     Some rejected
  │                                   │
  ▼                                   ▼
┌───────────┐                  ┌─────────────────┐
│ AVAILABLE  │                  │ PARTIALLY_RECEIVED│
│ stock += N │                  │ stock += accepted │
└───────────┘                  │ rejected logged   │
                                │ vendor notified   │
                                └─────────────────┘

ConsignmentRequest Data Model:
├── id (UUID)
├── vendor (FK → Vendor)
├── product (FK → Product)
├── variant (FK → ProductVariant, nullable)
├── requestedQuantity (int)
├── receivedQuantity (int, default 0)
├── rejectedQuantity (int, default 0)
├── rejectionReason (string, nullable)
├── status (ENUM: REQUESTED, SHIPPED_BY_VENDOR, RECEIVED, PARTIALLY_RECEIVED, CANCELLED)
├── requestedAt (timestamp)
├── shippedAt (timestamp, nullable)
├── receivedAt (timestamp, nullable)
├── notes (string — admin notes)
└── createdBy
```

---

## 2. Stock Reservation Mechanics

### 2.1 Reservation Flow (Atomic)

```
When a tourist clicks "Place Order" (before payment):

  BEGIN TRANSACTION (SERIALIZABLE isolation for stock operations)
  
  FOR EACH item in cart:
    1. SELECT available_stock FROM product_variants 
       WHERE id = :variantId 
       FOR UPDATE  -- row-level lock prevents concurrent modification
    
    2. IF available_stock < requested_quantity:
         ROLLBACK entire transaction
         RETURN error: "Insufficient stock for [product name] — [variant name]. 
                        Only [available] remaining."
    
    3. UPDATE product_variants 
       SET available_stock = available_stock - :quantity,
           reserved_stock = reserved_stock + :quantity
       WHERE id = :variantId
    
    4. INSERT INTO stock_audit_log:
         { variantId, changeType: RESERVATION, quantity: -quantity, 
           reason: "Order BAH-XXXX", orderId, timestamp }
  
  CREATE Order (status: PENDING_PAYMENT)
  CREATE OrderItems with price snapshots
  CREATE Payment record
  
  COMMIT TRANSACTION

Key design decisions:
  - SELECT ... FOR UPDATE: pessimistic row lock prevents two concurrent orders from 
    overselling the same variant
  - SERIALIZABLE isolation: ensures consistent reads within the transaction
  - All-or-nothing: if ANY item is out of stock, ENTIRE order is rejected (no partial orders 
    at creation time)
  - Cart does NOT reserve stock: only at checkout ("Place Order" click)
  - This means it's possible to add to cart, go to checkout, and find an item out of stock —
    this is intentional to avoid phantom reservations from abandoned carts
```

### 2.2 Reservation Expiry

```
Problem: Tourist creates order (stock reserved) but never completes payment.
Solution: Scheduled job releases expired reservations.

  RESERVATION_TIMEOUT = 30 minutes (configurable)

  Scheduled job (runs every 5 minutes):
    SELECT * FROM orders 
    WHERE status = 'PENDING_PAYMENT' 
    AND created_at < NOW() - INTERVAL 30 MINUTE
    FOR UPDATE
    
    FOR EACH expired order:
      BEGIN TRANSACTION
        1. Update order.status → CANCELLED (reason: PAYMENT_TIMEOUT)
        2. FOR EACH order item:
           UPDATE product_variants
           SET available_stock = available_stock + item.quantity,
               reserved_stock = reserved_stock - item.quantity
           WHERE id = item.variantId
        3. Log stock audit entry: { changeType: RESERVATION_RELEASE, reason: "Payment timeout" }
        4. Cancel Stripe PaymentIntent (if created)
      COMMIT TRANSACTION
    
    Log: "Released reservations for N expired orders"
```

---

## 3. Failure Scenarios

### 3.1 Overselling Prevention

```
Scenario: Two tourists try to buy the last unit of the same product at the same time.

Prevention mechanism:
  - SELECT ... FOR UPDATE creates a row-level lock
  - First transaction locks the row, reads available_stock = 1, reserves it
  - Second transaction waits for lock release, reads available_stock = 0, fails
  - Result: only one tourist gets the product, no overselling

If somehow overselling occurs (bug, data inconsistency):
  1. Stock audit log will show available_stock went negative
  2. Monitoring alert triggers when any variant has available_stock < 0
  3. Admin manually resolves:
     a. Contact customer with the later order: apologize, offer refund + discount on next order
     b. Adjust stock to 0
     c. Log incident for postmortem
```

### 3.2 Vendor Out-of-Stock After Order (Consignment)

```
Scenario: Order placed and paid for a consignment product, but upon picking from warehouse 
shelf, admin discovers the item is damaged/missing (physical stock doesn't match system stock).

Resolution flow:
  1. Admin marks order item status: FULFILLMENT_ISSUE
  2. Admin selects reason: DAMAGED_IN_WAREHOUSE, MISSING_STOCK, QUALITY_ISSUE
  3. System options presented to admin:
     a. SUBSTITUTE: replace with equivalent variant/product (requires customer approval)
     b. BACKORDER: request vendor to supply replacement (update estimated delivery)
     c. CANCEL_ITEM: cancel this item from the order (partial cancellation)
     d. CANCEL_ORDER: cancel entire order
  
  For option (a) — Substitute:
     - Admin contacts customer via email: "We'd like to offer [substitute]. Is that acceptable?"
     - If accepted: admin updates order item, adjusts stock
     - If declined: proceed to option (c) or (d)
  
  For option (c) — Cancel Item:
     - Item status → CANCELLED
     - Partial refund for this item via Stripe
     - Commission ledger entries for this item reversed
     - If last item in order cancelled → entire order → CANCELLED
  
  For all scenarios:
     - Stock adjustment logged with reason
     - If consignment: vendor notified of stock discrepancy
     - Incident logged for stock reconciliation
```

### 3.3 Consignment Return-to-Vendor

```
Scenario: Vendor requests return of unsold consignment stock.

Rules:
  - Vendor can request return after minimum holding period (default: 90 days from receipt)
  - Vendor must give 30-day notice (platform may have orders in pipeline)
  - Platform ships back at platform's cost (part of consignment agreement)
  
Flow:
  1. Vendor requests return (via portal or phone)
  2. Admin creates ConsignmentReturn record
  3. Admin verifies no pending/active orders for these variants
  4. If active orders exist: hold return until orders fulfilled
  5. Admin removes stock from system (available_stock -= return quantity)
  6. Admin archives product if no remaining stock
  7. Admin ships items back to vendor
  8. Vendor confirms receipt → ConsignmentReturn.status = COMPLETED
```
