# Part 4: Commission System & Referral Tracking Design

---

## 1. Commission System Design

### 1.1 Commission Types

There are **four commission stakeholders** on every order:

| Stakeholder | When Applicable | Typical Rate | Paid From |
|-------------|----------------|-------------|-----------|
| **Platform** | Every order | Remainder after all other commissions | Sale revenue |
| **Vendor** | Consignment orders only | 65–75% of item sale price | Platform pays vendor |
| **Hotel Partner** | Order attributed to hotel referral | 3–5% of order subtotal | Platform's share |
| **Tour Guide** | Order attributed to guide referral | 5–8% of order subtotal | Platform's share |

### 1.2 Commission Calculation Formulas

#### Scenario A: OWNED Inventory, No Referral

```
Item sale price:          BTN 5,000
Cost to platform (COGS):  BTN 2,000
Shipping revenue:          BTN 1,200  (charged to customer)
Shipping cost (actual):    BTN   800

Commission breakdown:
  Vendor payout:           BTN     0  (platform owns inventory — vendor already paid at purchase)
  Partner commission:      BTN     0  (no referral)
  Platform gross margin:   BTN 3,000  (sale price - COGS)
  Platform shipping margin:BTN   400  (shipping revenue - shipping cost)
  Platform total margin:   BTN 3,400
```

#### Scenario B: OWNED Inventory, Hotel Referral (5%)

```
Item sale price:          BTN 5,000
Cost to platform (COGS):  BTN 2,000
Hotel commission rate:     5%

Commission breakdown:
  Hotel commission:        BTN   250  (5% × BTN 5,000)
  Vendor payout:           BTN     0  (owned — already paid)
  Platform gross margin:   BTN 2,750  (5,000 - 2,000 - 250)
```

#### Scenario C: CONSIGNMENT Inventory, Guide Referral (7%)

```
Item sale price:          BTN 5,000
Vendor commission rate:    70% (vendor gets 70%, platform keeps 30%)
Guide commission rate:     7%

Commission breakdown:
  Vendor payout:           BTN 3,500  (70% × BTN 5,000)
  Platform commission:     BTN 1,500  (30% × BTN 5,000)
  Guide commission:        BTN   350  (7% × BTN 5,000 — paid from platform's 1,500)
  Net platform revenue:    BTN 1,150  (1,500 - 350)
```

#### Scenario D: CONSIGNMENT + Hotel Referral (Multi-item Order)

```
Order with 2 items:
  Item 1: Consignment textile, BTN 5,000, vendor rate 70%
  Item 2: Owned wooden mask,  BTN 3,000, cost BTN 1,200
  Hotel referral rate: 4%

Per-item calculation:
  Item 1 (consignment):
    Vendor payout:    BTN 3,500  (70% × 5,000)
    Platform share:   BTN 1,500  (30% × 5,000)
    Hotel commission: BTN   200  (4% × 5,000) — from platform's share
    Net platform:     BTN 1,300

  Item 2 (owned):
    Vendor payout:    BTN     0
    Platform margin:  BTN 1,800  (3,000 - 1,200)
    Hotel commission: BTN   120  (4% × 3,000) — from platform margin
    Net platform:     BTN 1,680

  Order totals:
    Vendor payout:    BTN 3,500
    Hotel commission: BTN   320
    Platform revenue: BTN 2,980
    Subtotal:         BTN 8,000  ✓ (3,500 + 320 + 2,980 + 1,200 COGS = 8,000)
```

### 1.3 Commission Calculation Rules

| Rule ID | Rule | Detail |
|---------|------|--------|
| CR-01 | Per-item calculation | Commissions calculated at the **order item** level, not order level. Each item has its own sourcing type and vendor. |
| CR-02 | Vendor commission applies only to CONSIGNMENT | OWNED products: vendor payout = 0 (platform already purchased the goods). |
| CR-03 | Partner commission applies to all items in attributed order | If an order has referral attribution, the partner earns commission on **every item** in the order, regardless of sourcing type. |
| CR-04 | Partner commission base = item sale price | Commission is calculated on the sale price (what the customer pays for the item), NOT on the platform's margin. |
| CR-05 | Platform commission = remainder | Platform keeps whatever is left after vendor and partner payouts. Platform never has a fixed rate — it's the residual. |
| CR-06 | Maximum one referral partner per order | An order can only be attributed to ONE partner (last-touch). No split attribution. |
| CR-07 | Commission rates snapshot at order time | The commission rate used is the rate configured at the **moment the order is created**. Subsequent rate changes do not affect existing orders. |
| CR-08 | Commissions become settleable after COMPLETED | An order must reach COMPLETED status (14 days after delivery with no issues) before commissions are eligible for settlement payout. |
| CR-09 | Refund → proportional commission reversal | If an order item is refunded (full or partial), all commission entries for that item are reversed proportionally. |
| CR-10 | Minimum commission payout: BTN 500 | Commission payouts accumulate until they reach BTN 500 minimum threshold. Below this, they carry forward to the next settlement cycle. |

### 1.4 Priority Rules (Multiple Referral Conflicts)

| Scenario | Resolution |
|----------|-----------|
| Tourist scans Hotel A QR, then Guide B link, then purchases | **Guide B** gets attribution (last-touch) |
| Tourist scans Guide A link on day 1, Guide A link again on day 5, purchases day 7 | **Guide A** (same partner, no conflict) |
| Tourist scans Hotel A QR, clears cookies, scans Hotel B QR, purchases | **Hotel B** (last-touch; clearing cookies is equivalent to new visitor) |
| Tourist scans Guide QR on phone, purchases on laptop (different device) | **No attribution** (cookie doesn't transfer across devices). Edge case — accepted loss. Mitigation: encourage guide to share link via WhatsApp so tourist opens on purchase device. |
| Two tourists share a device (rare) | Last referral code wins. Accepted trade-off. |
| Partner is deactivated between referral click and purchase | **Attribution honored** — the commission is still recorded but flagged as `PARTNER_DEACTIVATED`. Admin decides whether to pay out. (Default: honor if deactivation was non-fraud.) |

### 1.5 Commission Ledger Design

The commission system uses a **ledger-based model** — an append-only log of financial entries that can be summed to determine balances. This is inspired by double-entry accounting and is chosen for auditability, accuracy, and easy reconciliation.

```
CommissionLedgerEntry
├── id (UUID)
├── orderId (FK → Order)
├── orderItemId (FK → OrderItem)
├── stakeholder (ENUM: PLATFORM, VENDOR, HOTEL_PARTNER, GUIDE_PARTNER)
├── stakeholderId (UUID — FK to Vendor or Partner table depending on type)
├── entryType (ENUM: EARNED, REVERSAL, ADJUSTMENT, PAYOUT)
├── amountBTN (decimal — positive for EARNED, negative for REVERSAL/PAYOUT)
├── description (string — human-readable, e.g., "Commission for order BAH-20260420-0001, item Yathra Scarf")
├── commissionRate (decimal — the rate used, for audit)
├── basePriceBTN (decimal — the item price the commission was calculated on)
├── status (ENUM: PENDING, SETTLED, CANCELLED)
├── settlementBatchId (FK → SettlementBatch, nullable — populated when paid out)
├── createdAt
└── createdBy (system or admin user)
```

**Ledger entry examples for one consignment item with guide referral:**

```
On ORDER COMPLETED:
  Entry 1: { stakeholder: VENDOR, type: EARNED, amount: +3,500, status: PENDING }
  Entry 2: { stakeholder: GUIDE_PARTNER, type: EARNED, amount: +350, status: PENDING }
  Entry 3: { stakeholder: PLATFORM, type: EARNED, amount: +1,150, status: PENDING }

On REFUND (full):
  Entry 4: { stakeholder: VENDOR, type: REVERSAL, amount: -3,500, status: PENDING }
  Entry 5: { stakeholder: GUIDE_PARTNER, type: REVERSAL, amount: -350, status: PENDING }
  Entry 6: { stakeholder: PLATFORM, type: REVERSAL, amount: -1,150, status: PENDING }

On SETTLEMENT PAYOUT (vendor):
  Entry 7: { stakeholder: VENDOR, type: PAYOUT, amount: -3,500, status: SETTLED, settlementBatchId: "SB-001" }
```

**Balance calculation:**
```
Vendor balance = SUM(amount) WHERE stakeholder = VENDOR AND stakeholderId = X AND status IN (PENDING)
  → If positive: platform owes vendor
  → If zero: settled
  → If negative: vendor owes platform (e.g., excess refunds — handled manually)
```

### 1.6 Settlement Process

```
┌──────────────────────────────────────────────────────────────────┐
│                     SETTLEMENT WORKFLOW                          │
└──────────────────────────────────────────────────────────────────┘

Frequency: Bi-weekly (1st and 15th of each month)

Step 1: ADMIN INITIATES SETTLEMENT
  Admin selects settlement period (e.g., April 1–15, 2026)
  Admin selects stakeholder type: VENDOR, HOTEL_PARTNER, or GUIDE_PARTNER
  
Step 2: SYSTEM GENERATES SETTLEMENT REPORT
  Query all CommissionLedgerEntries WHERE:
    - status = PENDING
    - entryType IN (EARNED, REVERSAL, ADJUSTMENT)
    - createdAt within settlement period
    - associated order status = COMPLETED (commissions only settleable after 14-day hold)
  
  Group by stakeholder:
    For each vendor/partner:
      Total earned = SUM(EARNED entries)
      Total reversed = SUM(REVERSAL entries)
      Net payable = Total earned + Total reversed (reversals are negative)
      
      If net payable < BTN 500 (minimum threshold):
        Carry forward to next period (do not include in this batch)
      
  Generate SettlementBatch:
    ├── id (UUID)
    ├── batchNumber (e.g., "SB-2026-04-15-V")
    ├── periodStart, periodEnd
    ├── stakeholderType (VENDOR, HOTEL_PARTNER, GUIDE_PARTNER)
    ├── totalAmount (sum of all payables in batch)
    ├── lineItemCount
    ├── status (DRAFT → APPROVED → PAID)
    ├── generatedAt, generatedBy
    ├── approvedAt, approvedBy
    └── paidAt, paidBy

Step 3: ADMIN REVIEWS SETTLEMENT REPORT
  Admin reviews:
    - List of all payees with amounts
    - Drill-down per payee to see individual order items
    - Flag any anomalies (unusually large payouts, high reversal ratios)
  
  Admin can:
    - ADJUST: manually add/remove entries with documented reason
    - APPROVE: marks batch status as APPROVED

Step 4: PAYOUT EXECUTION
  Admin processes payments:
    - For vendors: bank transfer or mobile payment (out-of-system, manual initially)
    - For hotel partners: bank transfer to hotel's account
    - For tour guides: mobile wallet transfer (mBoB popular in Bhutan)
  
  Admin marks each payee as PAID in the system.
  System creates PAYOUT ledger entry for each payee (negative amount, reducing balance to zero).
  
Step 5: CONFIRMATION
  System sends settlement notification to each payee:
    - Vendors: "You've been paid BTN X,XXX for sales from [period]. View details in your portal."
    - Partners: "Your referral earnings of BTN X,XXX for [period] have been paid."
  
  Settlement batch status → PAID
```

---

## 2. Referral Tracking Design

### 2.1 QR Code Structure

```
QR Code Content (URL):
  https://bhutanartisanhub.com/?ref={REFERRAL_CODE}

REFERRAL_CODE Format:
  {PARTNER_TYPE}_{IDENTIFIER}_{SEQUENCE}
  
  Examples:
    HOTEL_ZHIWA_001      → Zhiwa Ling Heritage Hotel, first code
    HOTEL_ZHIWA_002      → Same hotel, second code (e.g., for different location in hotel)
    GUIDE_TSHERING_001   → Tour guide Tshering, first code
    GUIDE_KARMA_001      → Tour guide Karma, first code

  Rules:
    - Uppercase alphanumeric + underscores only
    - 5–30 characters
    - Globally unique
    - Partner type prefix for easy identification
    - Auto-generated by system on partner onboarding (admin can customize suffix)

QR Code Generation:
  - Format: PNG (300×300px for print) and SVG (scalable)
  - Error correction: Level M (15% correction — survives minor damage)
  - Embedded: BAH logo watermark in center (optional)
  - Generated: server-side using ZXing library
  - Downloadable: from partner dashboard and admin panel
  - Printable: BAH provides branded table cards and stickers with QR code for hotels

Physical Distribution:
  - Hotels: acrylic table stands for lobby + in-room cards + restaurant table tents
  - Guides: laminated pocket cards + digital image in phone gallery
  - Each physical item has partner's name and code printed below QR
```

### 2.2 Session Tracking Logic

```
┌────────────────────────────────────────────────────────────────────┐
│ FRONTEND REFERRAL SESSION MANAGEMENT                              │
└────────────────────────────────────────────────────────────────────┘

On Page Load (every page):
  1. Check URL for `ref` parameter
     IF present:
       a. Validate format: /^[A-Z]+_[A-Z0-9_]{2,25}$/ (client-side)
       b. Store in cookie: 
            name: bah_ref
            value: {REFERRAL_CODE}
            maxAge: 30 days
            path: /
            httpOnly: false  (needs JS access for sending in API calls)
            secure: true     (HTTPS only)
            sameSite: Lax
       c. Store in localStorage:
            key: bah_referral
            value: JSON.stringify({ code: {REFERRAL_CODE}, capturedAt: Date.now() })
       d. POST /api/v1/referrals/click (async, non-blocking)
       e. Clean URL: window.history.replaceState(null, '', window.location.pathname)
            (removes ?ref= from visible URL for cleaner UX)
  
  2. Check cookie for bah_ref
     IF present: referral active, code available for checkout
     IF missing:
       a. Check localStorage for bah_referral
       b. IF present AND captured < 30 days ago:
            Restore cookie from localStorage value
       c. IF missing or expired:
            No referral attribution for this session

On Checkout (order creation):
  1. Read referral code from cookie (primary)
  2. Fallback: read from localStorage
  3. Include in order creation request body: { referralCode: "HOTEL_ZHIWA_001" }
  4. Backend validates code, resolves to partner, stores in order
  5. DO NOT clear cookie after purchase (allows attribution for future orders within 30-day window)
```

### 2.3 Attribution Rules

| Rule ID | Rule | Detail |
|---------|------|--------|
| AR-01 | Last-touch wins | If multiple referral codes are captured in the 30-day window, the most recent one is used for attribution. |
| AR-02 | 30-day attribution window | Referral cookie expires after 30 days. After that, user is treated as organic traffic. |
| AR-03 | Cross-device: not supported | Referral is device-bound (cookie/localStorage). If tourist scans QR on phone but purchases on laptop, attribution is lost. This is an accepted trade-off. |
| AR-04 | One partner per order | An order can only be attributed to one referral partner. No split attribution. |
| AR-05 | Self-referral blocked | A partner cannot earn commission on their own purchases. System checks if the purchaser's email matches the partner's registered email. |
| AR-06 | Deactivated partner: honored | If partner is deactivated between click and purchase, the attribution is recorded but flagged. Admin decides on payout. |
| AR-07 | Invalid code: silent ignore | If a referral code doesn't match any active partner, the user proceeds normally with no error. Code is logged for admin review. |
| AR-08 | Attribution is immutable after order creation | Once an order is created with a referral code, the attribution cannot be changed. |
| AR-09 | Referral persists post-purchase | The referral cookie is NOT cleared after purchase. The tourist may make additional purchases during their trip, all attributed to the same partner. |

### 2.4 Fraud Prevention

| Threat | Detection | Prevention |
|--------|-----------|-----------|
| **Click fraud**: Partner generates thousands of fake clicks to inflate metrics | Monitor click-to-conversion ratio. Flag partners with > 1,000 clicks but < 1% conversion. | Rate limit clicks: max 100 per referral code per IP per day. Hash IP, don't store raw. |
| **Self-referral**: Partner buys through own link | Check purchaser email against partner registered email. | Block commission if email matches. Log for admin review. |
| **Collusion**: Partner and tourist collude for fake purchase + refund | Monitor refund rate per partner. Flag if > 15% of referred orders are refunded. | Commission only settleable after 14-day hold (order COMPLETED). Refunded orders reverse all commissions. |
| **Code sharing**: Unauthorized distribution of referral codes | Monitor geographic spread of clicks per code. Hotel code used from 20 countries = suspicious. | Admin investigation. Deactivate code if confirmed abuse. |
| **Duplicate accounts**: Tourist creates multiple accounts to exploit promotions | Check by email, phone, shipping address fingerprint. | Deduplicate orders by email. Flag identical shipping addresses across accounts. |
| **Expired partner**: Using deactivated partner's code | Backend validates partner status on click and on checkout. | Click tracking logs it but does not attribute. Checkout rejects attribution (order still proceeds without referral). |

### 2.5 Referral Data Model

```
Partner
├── id (UUID)
├── partnerCode (unique, e.g., "HOTEL_ZHIWA_001")
├── partnerType (ENUM: HOTEL, GUIDE)
├── businessName (string — hotel name or guide's full name)
├── contactName (string)
├── email (unique)
├── phone (string)
├── region (string — where the partner operates)
├── commissionRate (decimal — e.g., 0.05 for 5%)
├── tier (ENUM: BRONZE, SILVER, GOLD — future use)
├── status (ENUM: ACTIVE, SUSPENDED, DEACTIVATED)
├── bankName (nullable)
├── bankAccountNumber (encrypted)
├── mobilePaymentNumber (nullable)
├── totalClicks (int — denormalized counter)
├── totalOrders (int — denormalized counter)
├── totalEarnings (decimal — denormalized, BTN)
├── qrCodeUrl (string — URL to generated QR code image)
├── notes (string — admin notes)
├── createdAt, updatedAt, createdBy
└── version

ReferralClick
├── id (UUID)
├── partnerId (FK → Partner)
├── referralCode (string)
├── sessionId (string — anonymous session tracking)
├── ipHash (string — SHA-256 hashed IP for fraud detection, not raw IP)
├── userAgentHash (string)
├── country (string — derived from IP geolocation at click time)
├── clickedAt (timestamp)
├── convertedToOrder (boolean — set to true if this session resulted in an order)
└── orderId (FK → Order, nullable — populated on conversion)
```
