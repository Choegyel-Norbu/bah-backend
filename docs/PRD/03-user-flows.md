# Part 3: Detailed User Flows

---

## Flow 1: Tourist Purchase Journey (QR → Checkout → Delivery)

### Trigger
Tourist scans QR code at hotel lobby or receives WhatsApp link from tour guide.

### Steps

```
Step 1: REFERRAL CAPTURE
────────────────────────
Tourist scans QR code OR clicks referral link.
URL: https://bhutanartisanhub.com/?ref=HOTEL_ZHIWA_001

System actions:
  1. Parse `ref` parameter → validate against Partner table
  2. If valid:
     a. Set cookie: `bah_ref=HOTEL_ZHIWA_001` (HttpOnly, SameSite=Lax, 30-day expiry)
     b. Set localStorage: `bah_referral=HOTEL_ZHIWA_001`
     c. Log referral click: {partnerId, timestamp, userAgent, IP (hashed)}
  3. If invalid/expired referral code:
     a. Silently ignore (do not block user)
     b. Log warning for admin review
  4. Redirect to homepage (strip ref param from URL for clean UX)

Step 2: BROWSING
────────────────
Tourist browses the catalog.
  - Default view: featured/curated collections
  - Can filter by: category, price range, craft tradition
  - Can search by: product name, artisan name, material
  
  During browsing, referral cookie persists across all pages.

Step 3: PRODUCT DETAIL PAGE
───────────────────────────
Tourist views a product page.
  - Displayed: images (swipeable gallery), name, price (in selected currency),
    artisan story, materials, dimensions, craft tradition, region of origin
  - Variant selection (if applicable): color, size
  - "Add to Cart" button (prominent)
  - "Save for Later" (bookmark for post-trip purchase)
  - Shipping estimate: "Ships to [detected country] from BTN X (~$Y USD)"

Step 4: ADD TO CART
───────────────────
Tourist clicks "Add to Cart".
  System actions:
    1. Validate variant is in stock (check available > 0)
    2. If in stock → add to cart (stored in session/localStorage for guests, DB for logged-in)
    3. If out of stock → show "Sold Out" badge, disable button, suggest similar products
    4. Cart does NOT reserve stock (stock reserved only at checkout)
  
  Cart persists across sessions via localStorage (guest) or user account.

Step 5: CART REVIEW
───────────────────
Tourist reviews cart.
  Displayed:
    - Item list (image, name, variant, quantity, unit price, line total)
    - Currency selector (change display currency)
    - Subtotal, estimated shipping (based on selected country), total
    - Referral code display: "Referred by: Zhiwa Ling Heritage Hotel" (if referral active)
    - "Apply promo code" field (if applicable)
    - "Proceed to Checkout" button

Step 6: CHECKOUT — SHIPPING INFO
─────────────────────────────────
Tourist enters shipping details.
  
  For guest checkout:
    - Email address (required — for order updates)
    - Full name
    - Shipping address (street, city, state/province, country dropdown, postal code)
    - Phone number (for shipping courier contact)
  
  For logged-in users:
    - Pre-filled from profile. Option to use saved address or enter new.
  
  Validation:
    - Email: valid format, not disposable domain
    - Country: from supported shipping zones list
    - Phone: E.164 format with country code
    - All fields required (except state for countries that don't use it)
  
  System shows final shipping cost based on destination + total weight.

Step 7: CHECKOUT — ORDER REVIEW
────────────────────────────────
Final review before payment.
  Displayed:
    - Complete item list with prices (in selected currency)
    - Shipping address summary
    - Shipping method + cost
    - Order total (items + shipping + tax if applicable)
    - Referral attribution (displayed subtly, e.g., "Recommended by Zhiwa Ling Heritage")
    - Exchange rate disclosure: "Charged in USD at rate 1 USD = 84.5 BTN (locked at checkout)"
  
  System actions on "Place Order":
    1. BEGIN TRANSACTION
    2. Validate all cart items still in stock (re-check available quantities)
    3. If any item out of stock:
       a. ROLLBACK
       b. Show clear message: "Sorry, [item name] is no longer available. Please update your cart."
       c. Return to cart with out-of-stock items flagged
    4. If all in stock:
       a. Reserve stock (decrement available, increment reserved) for each item
       b. Create Order record (status: PENDING_PAYMENT)
       c. Create OrderItem records (price snapshots locked)
       d. Lock referral attribution from cookie → Order.referralCode, Order.referralPartner
       e. Lock exchange rate → Order.exchangeRateUsed
       f. Create Payment record (status: CREATED)
       g. Create Stripe PaymentIntent with order ID as idempotency key
       h. COMMIT TRANSACTION
    5. Return Stripe client_secret to frontend for payment confirmation

Step 8: PAYMENT
───────────────
Tourist confirms payment in Stripe payment element (card form embedded on page).
  
  - Stripe.js handles card input (PCI-compliant, no card data touches BAH server)
  - Tourist clicks "Pay $X.XX USD"
  - Stripe processes payment
  - On success:
    a. Stripe sends `payment_intent.succeeded` webhook to BAH backend
    b. Backend (idempotently) updates:
       - Payment.status → SUCCEEDED
       - Order.status → CONFIRMED
       - Commission ledger entries created for this order
    c. Frontend redirected to order confirmation page
    d. Confirmation email sent with order number and estimated delivery
  
  - On failure:
    a. Stripe returns error to frontend
    b. Tourist can retry with different card
    c. After 30 minutes without successful payment:
       - Scheduled job auto-cancels order
       - Stock reservation released
       - Order.status → CANCELLED (reason: PAYMENT_TIMEOUT)

Step 9: POST-PURCHASE
─────────────────────
  - Tourist sees order confirmation page with:
    - Order number (e.g., "BAH-20260420-0001")
    - Items ordered
    - Estimated delivery date range
    - "Track your order" link (order number + email lookup)
    - "Create an account" prompt (pre-filled from checkout data)
  
  - Email sent: order confirmation + receipt
  - Tourist can track order at any time via /track?order=BAH-20260420-0001&email=sarah@example.com

Step 10: FULFILLMENT & DELIVERY
────────────────────────────────
  Admin fulfills order:
    1. Admin sees order in dashboard (status: CONFIRMED)
    2. Admin picks, packs, photographs package (proof of shipment)
    3. Admin marks order as PROCESSING → then SHIPPED
    4. Admin enters tracking number from shipping provider
    5. System emails customer with tracking number and estimated delivery
    6. Customer tracks package via shipping provider's website
    7. When delivered (confirmed by tracking or manually by admin):
       - Order.status → DELIVERED
       - Delivery confirmation email sent
       - After 14 days with no issues → Order.status → COMPLETED
       - Commissions become eligible for settlement
```

---

## Flow 2: Referral Attribution Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                     REFERRAL ATTRIBUTION FLOW                       │
└─────────────────────────────────────────────────────────────────────┘

1. CLICK EVENT
   Partner shares link: https://bhutanartisanhub.com/?ref=GUIDE_TSHERING_42

2. CAPTURE (Frontend)
   ├── Extract `ref` param from URL
   ├── Validate format: /^[A-Z]+_[A-Z0-9_]+$/
   ├── Store in cookie: bah_ref (30-day, HttpOnly, SameSite=Lax)
   ├── Store in localStorage: bah_referral (backup if cookies blocked)
   └── POST /api/v1/referrals/click
       Body: { referralCode: "GUIDE_TSHERING_42" }

3. BACKEND CLICK TRACKING
   ├── Validate referral code exists in Partner table
   ├── Validate partner status = ACTIVE
   ├── Log to ReferralClick table:
   │   { partnerId, code, timestamp, userAgentHash, ipHash, sessionId }
   ├── Increment partner.totalClicks counter
   └── Return 200 (silent — no error shown to user even if invalid)

4. BROWSE SESSION (referral persists)
   ├── Cookie attached to every request automatically
   ├── If user clears cookies but localStorage intact → restore cookie on next page load
   └── If BOTH cleared → attribution lost (acceptable — fraudprevention trade-off)

5. NEW REFERRAL CLICK (attribution override policy)
   Policy: LAST-TOUCH ATTRIBUTION (see justification below)
   ├── If user clicks new referral link while existing cookie active:
   │   ├── Overwrite cookie with new referral code
   │   └── Log overwrite event: { oldCode, newCode, timestamp }
   └── The partner whose link was clicked MOST RECENTLY gets attribution

6. CHECKOUT — ATTRIBUTION LOCK
   ├── Read referral code from cookie (primary) or localStorage (fallback)
   ├── If referral code present:
   │   ├── Validate partner still ACTIVE
   │   ├── Write to Order.referralCode, Order.referralPartner, Order.referralType
   │   └── This attribution is PERMANENT — cannot be changed after order creation
   ├── If no referral code:
   │   └── Order.referralType = NONE (organic/direct traffic)
   └── Clear referral from cookie/localStorage? NO — persists for future orders within 30-day window

7. POST-ORDER
   ├── Commission calculated for referring partner on this order
   ├── Partner dashboard updated with new attributed order
   └── If order is later cancelled/refunded → commission reversed in ledger
```

### Last-Touch Attribution Justification

| Factor | Last-Touch | First-Touch |
|--------|-----------|-------------|
| **Simplicity** | ✅ Simple to implement and explain | ❌ Requires tracking first-ever touch per visitor |
| **Fairness for this model** | ✅ The last person to recommend BAH to the tourist is most likely the immediate trigger for the purchase | ❌ First touch may have been weeks ago, tourist may have forgotten |
| **Typical tourist journey** | Tourist visits 2–3 hotels during trip. The hotel they're staying at when they purchase is the most relevant partner. | First hotel may have planted the seed, but the purchase decision happens later. |
| **Fraud resistance** | ✅ Harder to game — requires actual proximity to tourist at purchase time | ❌ First-touch can be gamed by mass-distributing codes early |
| **Industry standard** | ✅ Most affiliate programs use last-touch | |

**Decision**: Use **last-touch attribution** with a **30-day cookie window**. If a tourist is referred by Hotel A on day 1 and Guide B on day 3, and purchases on day 5, Guide B receives the referral commission.

---

## Flow 3: Admin Product Onboarding Flow

```
Step 1: VENDOR COORDINATION (offline)
  Admin contacts vendor (phone/WhatsApp), agrees on:
    - Products to list
    - Sourcing type (OWNED: BAH purchases; CONSIGNMENT: vendor supplies)
    - For OWNED: wholesale price BAH pays
    - For CONSIGNMENT: vendor's payout percentage (e.g., 70%)
    - Quantity to supply initially
    - Ongoing replenishment expectations

Step 2: PRODUCT INTAKE
  Admin receives physical products at BAH warehouse (Thimphu).
  For CONSIGNMENT:
    - Create/update ConsignmentReceipt record
    - Update vendor stock count

Step 3: PRODUCT PHOTOGRAPHY
  BAH photography team captures:
    - 3–8 product photos (white background, styled, detail shots)
    - Artisan portrait (if new artisan)
    - Upload to media storage (S3/CDN)

Step 4: CATALOG ENTRY (Admin Panel)
  Admin creates product in system:
    a. Status: DRAFT (not visible to customers)
    b. Fill: name, description, short description
    c. Select: category (from existing taxonomy)
    d. Select/create: artisan profile
    e. Link: vendor
    f. Set: sourcing type (OWNED or CONSIGNMENT)
    g. Set pricing:
       - basePriceBTN (retail price customers see)
       - costPriceBTN (what BAH paid for OWNED, or vendor payout for CONSIGNMENT)
       - commissionRate (for CONSIGNMENT: platform's take, e.g., 0.30)
    h. Write: artisan story (who made this, how, cultural significance)
    i. Specify: materials, weight, dimensions, region, craft tradition
    j. Upload: images (reorder via drag-and-drop)
    k. Add: tags for filtering

Step 5: VARIANT CREATION (if applicable)
  For each variant:
    - Variant name (e.g., "Natural White - Large")
    - SKU (auto-generated)
    - Attributes (color, size, material)
    - Price override (if variant has different price)
    - Initial stock quantity
    - Variant-specific images (if applicable)

Step 6: REVIEW & VALIDATION
  System validates all required fields for ACTIVE status:
    ✓ At least 1 image uploaded
    ✓ Price > 0
    ✓ Weight > 0
    ✓ Category assigned
    ✓ At least 1 variant with stock > 0
    ✓ Description ≥ 50 characters
    ✓ Artisan linked
  
  Admin previews product page as it will appear to tourists.

Step 7: PUBLISH
  Admin transitions status: DRAFT → ACTIVE
  Product immediately visible in storefront.
  Internal notification: "New product published: [name]"
```

---

## Flow 4: Order Lifecycle Flow

```
┌────────────────┐
│ PENDING_PAYMENT │ ← Order created, stock reserved, Stripe PaymentIntent created
└───────┬────────┘
        │
   ┌────┴────────────────────────┐
   │                             │
   ▼                             ▼
┌──────────┐           ┌──────────────┐
│ CONFIRMED │           │  CANCELLED   │ ← Payment timeout (30 min) or customer cancel
└────┬─────┘           │  (auto/manual)│
     │                  └──────┬───────┘
     │                         │
     ▼                         ▼
┌────────────┐          Stock released
│ PROCESSING │          Commission entries voided
│ (picking & │          Customer notified
│  packing)  │
└────┬───────┘
     │
     ▼
┌──────────┐
│  SHIPPED  │ ← Tracking number entered by admin
└────┬─────┘
     │
     ▼
┌───────────┐
│ DELIVERED  │ ← Confirmed via tracking or manual admin update
└────┬──────┘
     │
     ├─── (14 days, no issues) ──→ ┌───────────┐
     │                              │ COMPLETED  │ ← Commissions eligible for settlement
     │                              └───────────┘
     │
     └─── (customer reports issue) ──→ ┌──────────────────┐
                                        │ RETURN_REQUESTED  │
                                        └───────┬──────────┘
                                                │
                                           Admin reviews
                                                │
                                        ┌───────┴──────────┐
                                        │                  │
                                        ▼                  ▼
                                 ┌──────────────┐  ┌───────────┐
                                 │RETURN_APPROVED│  │  DENIED   │
                                 └──────┬───────┘  │(close case)│
                                        │          └───────────┘
                                        ▼
                                 ┌──────────┐
                                 │ RETURNED  │ ← Item received back at warehouse
                                 └────┬─────┘
                                      │
                                      ▼
                                ┌────────────────┐
                                │ REFUND_INITIATED│
                                └──────┬─────────┘
                                       │
                                       ▼
                                 ┌──────────┐
                                 │ REFUNDED  │ ← Stripe refund processed, commissions reversed
                                 └──────────┘
```

---

## Flow 5: Refund Flow

```
TRIGGER: Customer contacts BAH support about an issue with a delivered order.

Step 1: RETURN REQUEST
  Customer submits return/refund request via:
    - Order tracking page → "Report Issue" button
    - Contact form with order number
  
  Required info:
    - Order number
    - Which item(s)
    - Reason: DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED, CHANGED_MIND, OTHER
    - Photos (optional but encouraged for damage claims)

Step 2: ADMIN REVIEW
  Admin reviews request in dashboard:
    - View order details, original photos, customer complaint
    - Decide: APPROVE or DENY
    - If DENY: must provide reason. Customer notified.
    - If APPROVE: system sets order status to RETURN_APPROVED

Step 3: RETURN SHIPPING (if physical return required)
  For DAMAGED / WRONG_ITEM:
    - BAH provides return shipping label (BAH pays)
    - Customer ships item back to BAH warehouse, Thimphu
  For CHANGED_MIND:
    - Customer pays return shipping
    - Must be in original condition, unused
  For low-value items (< BTN 2,000):
    - Admin may approve refund WITHOUT requiring return ("keep the item" policy)

Step 4: ITEM RECEIPT & INSPECTION
  When returned item arrives at warehouse:
    - Admin inspects condition
    - Updates order status to RETURNED
    - Stock adjustment:
      - If resalable: available += quantity
      - If damaged: log as DAMAGE adjustment, no stock increase

Step 5: REFUND PROCESSING
  Admin initiates refund:
    - Full refund: entire item cost refunded
    - Partial refund: admin enters amount (e.g., 50% for wear-and-tear)
    - Shipping cost: refunded only for BAH errors (wrong item, damage in transit)
  
  System actions:
    1. Call Stripe Refund API (amount in original charged currency)
    2. Update Payment.refundedAmount
    3. Update Order status to REFUND_INITIATED → REFUNDED
    4. REVERSE commission ledger entries:
       - Vendor commission: reversed proportionally
       - Partner (hotel/guide) commission: reversed proportionally
       - Platform commission: reversed proportionally
    5. Email customer: "Your refund of $X.XX has been processed. Allow 5–10 business days."

REFUND RULES:
  - Full refund within 30 days of delivery: no questions (unless Changed Mind → return required)
  - Partial refund within 90 days: admin discretion
  - After 90 days: case-by-case, generally denied
  - Refund amount can NEVER exceed original charge amount
  - Shipping cost refunded only if BAH error
```
