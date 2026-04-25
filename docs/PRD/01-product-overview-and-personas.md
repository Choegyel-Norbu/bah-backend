# Bhutan Artisan Hub — Product Requirements Document

> **Version**: 1.0  
> **Date**: 2026-04-20  
> **Status**: Draft  
> **Owner**: Product & Engineering  

---

## Table of Contents (Full PRD)

| Part | File | Contents |
|------|------|----------|
| 1 | `01-product-overview-and-personas.md` | Vision, Business Goals, Differentiators, User Personas |
| 2 | `02-functional-requirements.md` | All 12 module specs with features, workflows, validation |
| 3 | `03-user-flows.md` | Detailed user journey diagrams and step-by-step flows |
| 4 | `04-commission-and-referral.md` | Commission engine, referral tracking, attribution design |
| 5 | `05-inventory-model.md` | Owned vs consignment, stock reservation, failure scenarios |
| 6 | `06-edge-cases-and-failures.md` | Exhaustive failure handling across all domains |
| 7 | `07-nonfunctional-metrics-risks.md` | NFRs, KPIs, dependencies, risks, future scope |

---

# Part 1: Product Overview & User Personas

---

## 1. Product Overview

### 1.1 Vision

Bhutan Artisan Hub is a **curated e-commerce platform** that connects tourists visiting Bhutan with authentic, handcrafted Bhutanese artisan products. Unlike open marketplaces where any vendor can list products, BAH operates as a **platform-controlled storefront** — every product is vetted, photographed, priced, and listed by the platform team. This guarantees quality, authenticity, and trust for time-constrained tourists who cannot evaluate artisan goods independently.

The platform bridges the **offline-to-online gap** using a partner referral network of hotels and tour guides who physically interact with tourists and direct them to the platform via QR codes and referral links. This creates a unique acquisition flywheel: tourists discover BAH through trusted in-country partners, not through ads or SEO.

### 1.2 Business Goals

| # | Goal | Success Metric | Target (Year 1) |
|---|------|----------------|------------------|
| BG-1 | Generate revenue through curated artisan product sales | Gross Merchandise Value (GMV) | BTN 15M (~$180K USD) |
| BG-2 | Build a sustainable partner referral network | Active referring partners | 50 hotels, 100 guides |
| BG-3 | Achieve positive unit economics on consignment model | Blended commission margin | ≥ 25% platform take-rate |
| BG-4 | Ensure repeat and post-trip purchases | Repeat purchase rate | ≥ 12% within 6 months |
| BG-5 | Establish BAH as the trusted source for authentic Bhutanese crafts | NPS score | ≥ 60 |
| BG-6 | Minimize operational overhead via automation | Manual intervention rate per order | < 5% |

### 1.3 Key Differentiators

| # | Differentiator | Why It Matters |
|---|---------------|----------------|
| D-1 | **Platform-curated catalog** — Vendors cannot self-list products. Every item is vetted for authenticity, quality, and cultural significance before listing. | Eliminates counterfeit/low-quality goods. Tourists trust curated selections over open marketplaces. |
| D-2 | **Dual inventory model** — Platform owns some inventory (retail) and takes others on consignment from artisans. | Flexibility to stock best-sellers while giving artisans zero-risk access to tourists. |
| D-3 | **Offline-to-online referral network** — Hotels and tour guides physically hand tourists QR codes/referral links, earning commission on resulting purchases. | Solves the discovery problem. Tourists in Bhutan have limited connectivity and rely on trusted local intermediaries. |
| D-4 | **Tourist-optimized UX** — Designed for 3–7 day trip windows: fast checkout, international shipping, multi-currency display, guest checkout. | Tourists won't create accounts or browse for hours. Every friction point loses a sale. |
| D-5 | **Transparent artisan stories** — Each product page features the artisan's story, craft tradition, and region of origin. | Emotional connection drives purchase decisions for handcrafted goods. |
| D-6 | **Post-trip purchasing** — Tourists can bookmark items during their trip and purchase after returning home with international shipping. | Extends the conversion window beyond the physical trip. |

### 1.4 Business Model Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                     BHUTAN ARTISAN HUB                          │
│                                                                 │
│  Revenue Sources:                                               │
│  ┌──────────────────────┐  ┌──────────────────────────────┐     │
│  │  OWNED INVENTORY     │  │  CONSIGNMENT INVENTORY       │     │
│  │  (Retail Model)      │  │  (Commission Model)          │     │
│  │                      │  │                              │     │
│  │  Platform buys at    │  │  Vendor supplies product     │     │
│  │  wholesale price     │  │  Platform sells at agreed    │     │
│  │  →  Sells at retail  │  │  retail price                │     │
│  │  →  Keeps margin     │  │  →  Platform takes 25-35%    │     │
│  │  (typically 40-60%)  │  │  →  Vendor gets 65-75%       │     │
│  └──────────────────────┘  └──────────────────────────────┘     │
│                                                                 │
│  Acquisition Channel:                                           │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PARTNER REFERRAL NETWORK                                │   │
│  │                                                          │   │
│  │  Hotel places QR code    →  Tourist scans  →  Browses   │   │
│  │  Guide shares ref link   →  Tourist clicks →  Purchases │   │
│  │                                                          │   │
│  │  Partner earns 3-8% commission on attributed orders      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. User Personas

### 2.1 Tourist (Primary Customer)

| Attribute | Detail |
|-----------|--------|
| **Name archetype** | "Sarah, the Cultural Explorer" |
| **Demographics** | Age 28–55, international traveler (US, EU, AU, JP, SG), mid-to-high income |
| **Trip context** | Visiting Bhutan for 5–14 days on a guided cultural tour. Limited free time. Spotty mobile connectivity outside Thimphu/Paro. |
| **Goals** | (1) Find authentic, high-quality Bhutanese handicrafts as gifts or personal keepsakes. (2) Purchase with confidence — know that items are genuine. (3) Have items shipped home rather than carrying in luggage. (4) Discover products quickly without extensive browsing. |
| **Frustrations** | (1) Cannot tell authentic from mass-produced crafts in local markets. (2) Doesn't have time to visit multiple shops during a packed itinerary. (3) Worried about international shipping reliability. (4) Language barriers with local vendors. (5) Uncertain about pricing fairness. |
| **Usage context** | Scans QR code at hotel lobby or receives link from tour guide. Browses on mobile phone (often iPhone). May begin browsing during the trip but complete purchase after returning home. Expects USD/EUR pricing alongside BTN. |
| **Key behaviors** | (1) 70% browse on mobile. (2) Average session: 4–8 minutes. (3) Decision factors: product story > price > shipping time. (4) Willing to pay premium for verified authenticity. |
| **Purchase psychology** | Buying artisan goods is an emotional, story-driven decision. Tourists want to feel connected to the artisan and the culture. They are not price-comparison shopping. |

### 2.2 Hotel Partner

| Attribute | Detail |
|-----------|--------|
| **Name archetype** | "Karma, Front Desk Manager at Zhiwa Ling Heritage" |
| **Demographics** | Hotel front desk staff or concierge at 3–5 star Bhutanese hotels |
| **Goals** | (1) Provide value-added service to guests by recommending quality shopping. (2) Earn referral commission for the hotel. (3) Simple process — scan or hand a card; no complex tech. |
| **Frustrations** | (1) Don't want to manage inventory or handle products. (2) Can't track whether guests actually purchased. (3) Commission must be transparent and reliably paid. (4) Don't want to explain complex signup processes to guests. |
| **Usage context** | Places BAH-branded QR stand cards at front desk, in-room, and at restaurant tables. When guests ask about shopping, hands them a card or shares the hotel's unique referral link. Checks a simple dashboard monthly to see referral earnings. |
| **Interaction frequency** | Daily (passive — QR codes are always visible). Active recommendation 2–5 times per day during tourist season (March–May, September–November). |

### 2.3 Tour Guide

| Attribute | Detail |
|-----------|--------|
| **Name archetype** | "Tshering, Licensed Cultural Guide" |
| **Demographics** | Licensed Bhutanese tour guide, age 25–45, tech-comfortable (uses WhatsApp, basic smartphone) |
| **Goals** | (1) Recommend authentic products to tourists during cultural site visits. (2) Earn personal commission on resulting purchases. (3) Share links via WhatsApp (most common communication channel with tourists). |
| **Frustrations** | (1) Doesn't want a complex app — needs to share a link in 10 seconds. (2) Wants real-time or near-real-time visibility into earned commissions. (3) Internet connectivity is unreliable at many tour sites. (4) Settlement/payout process must be simple and regular. |
| **Usage context** | During a tour, when visiting a dzong or textile workshop, the guide says: "If you'd like to purchase authentic Bhutanese textiles, I recommend Bhutan Artisan Hub" and shares their personal referral link via WhatsApp or shows a QR code from their phone. |
| **Interaction frequency** | 3–8 referral shares per week during season. Checks earnings dashboard weekly. |

### 2.4 Admin (Platform Operator)

| Attribute | Detail |
|-----------|--------|
| **Name archetype** | "Dorji, BAH Operations Lead" |
| **Demographics** | BAH internal team member. Small team (3–5 people initially). Handles product curation, vendor relations, order management, and partner payouts. |
| **Goals** | (1) Onboard new products from vetted vendors quickly and accurately. (2) Monitor and fulfill orders with minimal manual steps. (3) Calculate and settle commissions for vendors, hotels, and guides accurately. (4) Manage inventory across owned and consignment stock. (5) Track platform performance metrics. |
| **Frustrations** | (1) Manual commission calculation is error-prone and time-consuming. (2) Coordinating with vendors for consignment stock availability is unpredictable. (3) International shipping logistics is complex (customs, duties, tracking). (4) Fraud detection (fake referrals, order manipulation) is difficult without tooling. |
| **Usage context** | Uses admin panel (web) daily for 4–6 hours. Needs bulk actions (import products, update prices, process settlements). Wants dashboards with real-time order and revenue data. |
| **Technical comfort** | Moderate. Can navigate web applications, read dashboards, use bulk import templates. Cannot write SQL or code. |

### 2.5 Vendor (Artisan / Artisan Cooperative)

| Attribute | Detail |
|-----------|--------|
| **Name archetype** | "Pema, Master Weaver from Bumthang" |
| **Demographics** | Individual artisan or small cooperative (2–10 artisans). Often rural. Limited tech access — basic smartphone, intermittent internet. |
| **Goals** | (1) Get products sold to international tourists without managing an online store. (2) Receive fair, transparent, and timely payment for consignment sales. (3) Maintain visibility into which products sold and current stock levels. |
| **Frustrations** | (1) Cannot manage complex e-commerce tools. (2) Language barrier (most communication in Dzongkha). (3) Doesn't set prices or control listings — relies on BAH team. (4) Concerned about product quality being misrepresented. (5) Irregular income makes long settlement cycles painful. |
| **Usage context** | Receives SMS/WhatsApp notifications when products sell. Logs into a simple vendor portal (mobile-optimized) to view sales history and pending payouts. Does NOT manage product listings, pricing, or shipping — all handled by BAH operations team. |
| **Access level** | **Read-only** for own sales data and payout history. **Cannot** create/edit products, set prices, or access other vendors' data. |
| **Interaction frequency** | Views portal 1–2 times per week. Receives push/SMS notifications on sales. Coordinates product supply with BAH ops team via phone/WhatsApp (outside the platform). |

---

### Persona Access Matrix

| Capability | Tourist | Hotel | Guide | Admin | Vendor |
|-----------|---------|-------|-------|-------|--------|
| Browse catalog | ✅ | ❌ | ❌ | ✅ | ❌ |
| Purchase products | ✅ | ❌ | ❌ | ❌ | ❌ |
| Manage cart / wishlist | ✅ | ❌ | ❌ | ❌ | ❌ |
| View own orders | ✅ | ❌ | ❌ | ✅ (all) | ❌ |
| Share referral link/QR | ❌ | ✅ | ✅ | ❌ | ❌ |
| View referral dashboard | ❌ | ✅ | ✅ | ✅ (all) | ❌ |
| Manage products | ❌ | ❌ | ❌ | ✅ | ❌ |
| Manage orders | ❌ | ❌ | ❌ | ✅ | ❌ |
| View own sales / payouts | ❌ | ❌ | ❌ | ✅ (all) | ✅ (own) |
| Manage vendors | ❌ | ❌ | ❌ | ✅ | ❌ |
| Manage partners | ❌ | ❌ | ❌ | ✅ | ❌ |
| View analytics | ❌ | ❌ | ❌ | ✅ | ❌ |
| Settle commissions | ❌ | ❌ | ❌ | ✅ | ❌ |
