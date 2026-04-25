# Part 7: Non-Functional Requirements, Metrics, Dependencies, Risks & Future Scope

---

## 1. Non-Functional Requirements

### 1.1 Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Page load time (storefront)** | < 2 seconds on 4G, < 4 seconds on 3G | Core Web Vitals (LCP, FID, CLS) via Lighthouse |
| **API response time (p95)** | < 300ms for read endpoints, < 500ms for write endpoints | Application metrics (Micrometer → Prometheus) |
| **API response time (p99)** | < 1 second for all endpoints | Same |
| **Checkout completion time** | < 60 seconds from cart review to payment confirmation | Client-side timing |
| **Search latency** | < 200ms for catalog search results | Backend timing |
| **Database query time (p95)** | < 50ms | Hibernate metrics, slow query log |
| **Image load time** | < 1 second per image on 4G | CDN metrics |
| **Webhook processing** | < 5 seconds from receipt to order update | Application timing |
| **Stock reservation** | < 200ms including lock acquisition | Transaction timing |

### 1.2 Availability & Reliability

| Metric | Target | Detail |
|--------|--------|--------|
| **Uptime** | 99.5% (≈ 44 hours downtime/year) | Includes planned maintenance windows (scheduled during 2–6 AM BTT) |
| **Recovery Time Objective (RTO)** | < 2 hours | Time to restore service after failure |
| **Recovery Point Objective (RPO)** | < 15 minutes | Maximum data loss in disaster scenario (DB backup frequency) |
| **Error rate** | < 0.5% of requests return 5xx | Application monitoring |
| **Planned maintenance window** | Max 2 hours, max 2×/month | Announced 48 hours in advance |

### 1.3 Scalability

| Dimension | Year 1 Target | Year 3 Target | Strategy |
|-----------|---------------|---------------|----------|
| **Concurrent users** | 100 | 1,000 | Horizontal scaling (add app instances) |
| **Orders per day** | 50 | 500 | Queue-based order processing |
| **Products in catalog** | 500 | 5,000 | Database indexing, CDN caching |
| **Total registered users** | 5,000 | 50,000 | Database partitioning if needed |
| **Partner count** | 150 | 1,000 | Partner service isolation |
| **Image storage** | 50 GB | 500 GB | S3 with CloudFront CDN |

### 1.4 Security Requirements

| Requirement | Standard | Implementation |
|-------------|----------|----------------|
| **Data encryption in transit** | TLS 1.2+ | HTTPS enforcement, HSTS headers |
| **Data encryption at rest** | AES-256 | Sensitive fields (bank accounts, phone numbers) encrypted in DB |
| **Password hashing** | BCrypt (12 rounds) | Spring Security `BCryptPasswordEncoder` |
| **PCI compliance** | SAQ A (no card data touches server) | Stripe Elements / PaymentIntents (card data never touches BAH) |
| **Authentication** | JWT (RS256 or HS256) | Access token (15 min) + refresh token (7 days) |
| **Authorization** | RBAC with method-level security | Spring Security `@PreAuthorize`, role hierarchy |
| **Input validation** | OWASP Top 10 | Jakarta Bean Validation + parameterized queries |
| **API rate limiting** | Per-user, per-IP | Bucket4j or Spring Cloud Gateway rate limiter |
| **CORS** | Whitelist only | Only BAH frontend domains allowed |
| **Secrets management** | No secrets in code | Environment variables / AWS Secrets Manager |
| **Dependency scanning** | Weekly | OWASP Dependency-Check or Snyk |
| **Audit trail** | All admin actions logged | Immutable audit_log table |

### 1.5 Observability

| Layer | Tool | Detail |
|-------|------|--------|
| **Logging** | SLF4J + Logback | Structured JSON logs. Fields: timestamp, level, traceId, userId, action, duration. |
| **Metrics** | Micrometer → Prometheus → Grafana | JVM metrics, HTTP metrics, custom business metrics (orders/min, GMV). |
| **Tracing** | OpenTelemetry → Jaeger | Distributed tracing across service calls and DB queries. |
| **Alerting** | Grafana Alerts / PagerDuty | Critical: payment failures, stock negative, 5xx spike. Warning: slow queries, high error rate. |
| **Health checks** | Spring Boot Actuator | `/actuator/health` checks: DB, Redis, disk, Stripe connectivity. |
| **Uptime monitoring** | UptimeRobot or Pingdom | External monitoring every 60 seconds. |

### 1.6 Internationalization

| Aspect | Implementation |
|--------|---------------|
| **Language** | English only (Phase 1). Dzongkha (Phase 2 — for vendor portal). |
| **Currency** | Display: USD, EUR, GBP, JPY, SGD, AUD, THB, BTN. Charge: customer's selected currency via Stripe. Internal accounting: BTN. |
| **Timezone** | All timestamps stored in UTC. Display in user's local timezone (detected via browser). Admin dashboard: BTT (UTC+6). |
| **Date format** | ISO 8601 in API responses. Localized display in frontend. |
| **Number format** | Use Intl.NumberFormat in frontend for locale-aware formatting. |

---

## 2. Metrics & KPIs

### 2.1 Business Metrics

| KPI | Definition | Target (Year 1) | Measurement Frequency |
|-----|-----------|------------------|----------------------|
| **Gross Merchandise Value (GMV)** | Total value of all orders (before refunds) | BTN 15M | Weekly |
| **Net Revenue** | GMV minus refunds, vendor payouts, partner commissions | BTN 5M | Monthly |
| **Average Order Value (AOV)** | Total revenue ÷ number of orders | BTN 4,000 (~$48 USD) | Weekly |
| **Orders per Day** | Total orders placed per day | 15 average, 50 peak | Daily |
| **Conversion Rate** | Orders ÷ unique visitors | ≥ 3% | Weekly |
| **Cart Abandonment Rate** | (Carts created - Orders completed) ÷ Carts created | < 60% | Weekly |
| **Repeat Purchase Rate** | Customers with > 1 order ÷ total customers | ≥ 12% within 6 months | Monthly |

### 2.2 Referral Metrics

| KPI | Definition | Target (Year 1) | Measurement |
|-----|-----------|------------------|-------------|
| **Referral Click Volume** | Total referral link/QR clicks per month | 5,000 | Weekly |
| **Referral Conversion Rate** | Orders with referral ÷ total referral clicks | ≥ 5% | Weekly |
| **Revenue from Referrals** | GMV attributed to referral partners | ≥ 60% of total GMV | Monthly |
| **Partner Engagement Rate** | Active referring partners (≥1 referral/month) ÷ total partners | ≥ 40% | Monthly |
| **Top Partner Revenue** | Revenue from top 10 partners | Should not exceed 30% of total referral revenue (concentration risk) | Monthly |
| **Referral Fraud Rate** | Flagged/blocked referral events ÷ total referral events | < 0.5% | Monthly |

### 2.3 Commission Metrics

| KPI | Definition | Target | Measurement |
|-----|-----------|--------|-------------|
| **Total Commission Payouts** | Sum of all vendor + partner payouts per period | Track trend | Bi-weekly |
| **Average Vendor Commission Rate** | Weighted average of vendor commission rates | 28–32% platform take-rate | Monthly |
| **Average Partner Commission Rate** | Weighted average of hotel/guide commission rates | 4–6% | Monthly |
| **Settlement Cycle Time** | Days from order completion to payout | < 21 days | Monthly |
| **Outstanding Commissions** | Total PENDING commission balance in ledger | Monitor trend | Weekly |
| **Commission Reversal Rate** | Reversed commissions ÷ total commissions | < 5% | Monthly |

### 2.4 Operational Metrics

| KPI | Definition | Target | Measurement |
|-----|-----------|--------|-------------|
| **Order Fulfillment Time** | Time from CONFIRMED to SHIPPED | < 48 hours (domestic), < 72 hours (international) | Weekly |
| **Order Success Rate** | COMPLETED orders ÷ total orders (excluding PENDING_PAYMENT) | ≥ 92% | Weekly |
| **Refund Rate** | Refunded orders ÷ delivered orders | < 5% | Monthly |
| **Stock Accuracy** | System stock matches physical count | ≥ 98% | Monthly (reconciliation) |
| **Customer Support Response Time** | Time from inquiry to first response | < 24 hours | Weekly |
| **Product Onboarding Time** | Time from vendor agreement to product ACTIVE | < 7 days | Monthly |

---

## 3. Dependencies

### 3.1 Payment Gateway

| Dependency | Detail |
|-----------|--------|
| **Primary** | **Stripe** — international card payments (Visa, Mastercard, Amex, JCB). Selected for: global coverage, excellent API, built-in fraud detection (Radar), PCI SAQ-A compliance, multi-currency support. |
| **Risk** | Stripe not officially available in Bhutan. **Mitigation**: register legal entity in Singapore or India as payment processor intermediary. |
| **Fallback** | PayPal Checkout as secondary option for tourists who prefer PayPal. Phase 2. |
| **Domestic** | mBoB (Mobile Banking of Bhutan) and BNB (Bank of Bhutan) for domestic customers. Integration via their API (if available) or manual reconciliation. Phase 2. |

### 3.2 Shipping Providers

| Provider | Use Case | Integration |
|----------|----------|-------------|
| **Bhutan Post** | Domestic + international standard shipping | Manual tracking number entry (Phase 1). API integration (Phase 2 — if API available). |
| **DHL Express** | Express international shipping | DHL API integration for label generation and tracking (Phase 2). |
| **FedEx / UPS** | Fallback for specific regions | Phase 3. |

### 3.3 Infrastructure

| Component | Service | Reasoning |
|-----------|---------|-----------|
| **Application hosting** | AWS EC2 (t3.medium initially) or DigitalOcean Droplet | Regional availability, cost-effective for scale. |
| **Database** | AWS RDS (MySQL 8) or managed MySQL | Automated backups, replication, scaling. |
| **Cache** | AWS ElastiCache (Redis) or self-hosted Redis | Product catalog caching, session management. |
| **File storage** | AWS S3 | Product images, documents, QR codes. |
| **CDN** | CloudFront or Cloudflare | Fast image delivery to global tourists. |
| **Email** | AWS SES or SendGrid | Transactional emails (order confirmation, shipping, etc.). |
| **Monitoring** | Prometheus + Grafana (self-hosted) or Datadog | Application and infrastructure monitoring. |
| **CI/CD** | GitHub Actions | Automated testing, building, deployment. |
| **DNS** | Cloudflare or Route 53 | Domain management, DDoS protection. |
| **Exchange rates** | FreeCurrencyAPI or Open Exchange Rates | Daily exchange rate refresh. |

### 3.4 Third-Party Libraries & Services

| Library/Service | Purpose | License |
|----------------|---------|---------|
| ZXing | QR code generation | Apache 2.0 |
| MapStruct | DTO ↔ Entity mapping | Apache 2.0 |
| Flyway | Database migration | Apache 2.0 |
| Stripe Java SDK | Payment processing | MIT |
| Jackson | JSON serialization | Apache 2.0 |
| Caffeine | Local caching | Apache 2.0 |
| iText or OpenPDF | Invoice PDF generation | AGPL (iText) or LGPL (OpenPDF) |

---

## 4. Risks & Trade-offs

### 4.1 Risk Register

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|-----------|
| R-01 | **Stripe not available in Bhutan** — cannot directly register as Bhutanese entity | High | Critical | Register intermediary entity in Singapore or India. Consult Stripe sales team for cross-border setup. Budget 2–4 weeks for account setup. |
| R-02 | **Bhutan Post unreliable** — international shipments delayed or lost | Medium | High | Set conservative delivery estimates. Offer DHL Express option. Proactive customer communication. Shipping insurance for high-value items. |
| R-03 | **Low internet penetration in Bhutan** — vendors and some partners have limited connectivity | High | Medium | SMS notifications for vendors. WhatsApp as backup communication. Vendor portal optimized for low bandwidth + offline-capable (Phase 2). |
| R-04 | **Partner fraud** — fake referrals, self-purchasing through own links | Medium | Medium | Fraud detection rules (see Part 4/6). Commission hold period (14 days). Manual review for high-value payouts. |
| R-05 | **Tourism seasonality** — 70%+ revenue concentrated in March–May and Sep–Nov | High | Medium | Plan marketing and inventory for peak seasons. Reduce operational costs in off-season. Post-trip purchase feature extends conversion window. |
| R-06 | **Currency volatility** — BTN pegged to INR, but USD/EUR fluctuations affect margins | Medium | Low | Lock exchange rates at order time. Daily rate refresh. Build 2–3% buffer into pricing. |
| R-07 | **Customs complications** — tourists' countries have varying import rules for handicrafts | Medium | Medium | Research major destination countries' tariff rules. Provide HS codes on customs forms. Clear disclaimers about buyer's duty responsibility. |
| R-08 | **Cold start / chicken-and-egg** — need products to attract tourists, need tourists to attract vendors | High | High | BAH team curates initial 50–100 products. Own inventory model reduces vendor dependency. Partner network (hotels) provides built-in distribution. |
| R-09 | **Single point of failure** — small team, single warehouse, limited redundancy | High | High | Document all processes. Cross-train team members. Cloud infrastructure for app reliability. |
| R-10 | **Counterfeiting concerns** — tourists may question product authenticity | Low | High | Each product has artisan story + authentication card (physical). Consider blockchain-based certificates of authenticity (Phase 3). |

### 4.2 Architectural Trade-offs

| Decision | Chosen Approach | Alternative Considered | Why |
|----------|----------------|----------------------|-----|
| **Monolith vs microservices** | Modular monolith (Spring Boot) | Microservices | Small team (3–5 dev). Monolith is faster to develop, deploy, and debug. Microservices overhead unjustified at this scale. Modular structure allows extraction later. |
| **Last-touch vs first-touch attribution** | Last-touch | First-touch, multi-touch | Simpler to implement. More relevant for this business model where the last interaction (hotel/guide) is the purchase trigger. See Part 4 for detailed justification. |
| **Cart reservation vs checkout reservation** | Checkout reservation only | Reserve on add-to-cart | Cart reservation leads to phantom stock locks from abandoned carts. With <50 orders/day and >500 products, stock contention is low. Better UX to not show "reserved by another user" messages. |
| **Guest checkout as primary** | Supported and encouraged | Account required | Tourist-optimized. Forcing account creation kills conversions for time-limited travelers. Post-purchase account creation option preserves long-term benefits. |
| **Commission ledger (append-only) vs balance updates** | Ledger | Direct balance mutation | Ledger provides full audit trail, easy reconciliation, and natural support for refund reversals. Slightly more complex but far more reliable for financial operations. |
| **Server-side vs client-side currency conversion** | Server-side (display only), charge in customer's currency | Convert all to BTN, charge in BTN | Tourists don't want to pay in an unfamiliar currency (BTN). Charging in USD/EUR via Stripe reduces friction and chargeback risk. |
| **Manual shipping integration (Phase 1) vs API** | Manual (admin enters tracking number) | DHL/Bhutan Post API from day 1 | Bhutan Post likely has no API. DHL API adds complexity. At <50 orders/day, manual entry is manageable. Automate when volume justifies. |

---

## 5. Future Scope (Post-MVP)

### Phase 2 (Month 6–12)

| Feature | Description | Business Value |
|---------|-------------|---------------|
| **Mobile app (React Native)** | Native iOS/Android app for tourists. Offline product browsing. Push notifications for order updates. | Higher engagement, better UX for tourists during trip. |
| **Vendor mobile portal** | WhatsApp-integrated notifications. Simple mobile-first dashboard in Dzongkha. | Vendor engagement. Lower support burden. |
| **DHL API integration** | Automated label generation, tracking sync, rate calculation. | Operational efficiency at scale. |
| **Domestic payment (mBoB/BNB)** | Mobile banking payment for Bhutanese customers. | Domestic market access. |
| **Review & rating system** | Verified purchase reviews. Admin moderation. | Social proof → higher conversion. |
| **Wishlist sharing** | Tourists can share wishlists with friends/family (gift idea). | Post-trip viral acquisition. |
| **Multi-language support** | Dzongkha for vendor portal. Japanese for J-market tourists. | Accessibility → market expansion. |

### Phase 3 (Month 12–24)

| Feature | Description | Business Value |
|---------|-------------|---------------|
| **AI product recommendations** | "Tourists who bought this also liked..." ML-based. | Higher AOV via cross-sell. |
| **Dynamic pricing** | Time-based discounts (end of season), demand-based pricing on trending items. | Revenue optimization. Inventory clearance. |
| **Personalization engine** | Browsing history-based homepage, email recommendations. | Higher conversion from returning visitors. |
| **Subscription boxes** | Monthly "Bhutan Craft Box" — curated items shipped internationally. | Recurring revenue. |
| **Artisan video stories** | Short video content on product pages (artisan making the product). | Emotional connection → higher conversion. |
| **Multi-warehouse** | Support regional warehouses (Paro, Bumthang) for faster domestic delivery. | Operational resilience. |
| **Blockchain authenticity** | NFT-based certificate of authenticity for high-value items. | Trust + collectability for premium segment. |
| **Partner tiered rewards** | Automated tier upgrades (Bronze → Silver → Gold) based on referral volume. Higher tiers earn more commission. | Partner motivation + loyalty. |
| **B2B wholesale portal** | Bulk ordering for international retailers who want to stock Bhutanese crafts. | New revenue channel. |

---

## 6. Glossary

| Term | Definition |
|------|-----------|
| **BAH** | Bhutan Artisan Hub (this platform) |
| **BTN** | Bhutanese Ngultrum (national currency, pegged to Indian Rupee) |
| **GMV** | Gross Merchandise Value — total value of goods sold before deductions |
| **AOV** | Average Order Value |
| **COGS** | Cost of Goods Sold — what the platform paid for owned inventory |
| **Owned inventory** | Products purchased and owned by BAH for resale |
| **Consignment inventory** | Products supplied by vendors, owned by vendors, sold by BAH on their behalf |
| **Partner** | Hotels or tour guides in the referral network |
| **Commission rate** | Percentage of sale price earned by a stakeholder |
| **Settlement** | The process of paying out earned commissions to vendors and partners |
| **Ledger entry** | A single financial record in the commission ledger |
| **Last-touch attribution** | Credit given to the most recent referral source before purchase |
| **SKU** | Stock Keeping Unit — unique identifier for each product variant |
| **PDP** | Product Detail Page |
| **ITP** | Intelligent Tracking Prevention (Safari's cookie restriction feature) |
| **DDP** | Delivered Duty Paid (shipping term where seller covers import duties) |
| **HS Code** | Harmonized System code for customs classification of goods |

---

## End of PRD

> **Document Status**: Draft v1.0 — awaiting stakeholder review  
> **Next Steps**: Engineering team review → technical feasibility assessment → Sprint planning  
> **Review Deadline**: 2 weeks from document date
