# Order API

**Base path:** `/api/v1/orders`  
**Auth:** Required (Bearer token). Orders are scoped to the authenticated user.

---

## Create order (place order)

Creates an order from the **current user's cart**. Cart is converted to order items, stock is deducted, and the cart is cleared (or items are consumed). Shipping address must belong to the user.

**URL:** `POST /api/v1/orders`

**Request body:**

```json
{
  "shippingAddressId": 1,
  "couponCode": "SAVE10",
  "notes": "Leave at door"
}
```

| Field              | Type   | Required | Description                                      |
|--------------------|--------|----------|--------------------------------------------------|
| shippingAddressId  | number | yes      | User's address ID (from GET /api/v1/users/me/addresses) |
| couponCode         | string | no       | Optional discount coupon code                    |
| notes              | string | no       | Order notes (e.g. delivery instructions)         |

**Response:** `201 Created`

```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260214-0001",
    "status": "PENDING",
    "subtotal": 5400.00,
    "discount": 0.00,
    "tax": 0.00,
    "shippingCost": 0.00,
    "total": 5400.00,
    "paymentMethod": "CASH_ON_DELIVERY",
    "paymentStatus": "PENDING",
    "couponCode": null,
    "notes": "Leave at door",
    "createdAt": "2026-02-14T15:00:00",
    "items": [
      {
        "id": 1,
        "productName": "Sweater",
        "sku": "sweater-m-red",
        "size": "M",
        "color": "red",
        "quantity": 2,
        "unitPrice": 2000.00,
        "totalPrice": 4000.00
      },
      {
        "id": 2,
        "productName": "Jeans",
        "sku": "jeans-l-blue",
        "size": "L",
        "color": "blue",
        "quantity": 1,
        "unitPrice": 1400.00,
        "totalPrice": 1400.00
      }
    ]
  },
  "timestamp": "2026-02-14T15:00:00"
}
```

**Order response fields:**

| Field          | Type    | Description                          |
|----------------|---------|--------------------------------------|
| id             | number  | Order ID                             |
| orderNumber    | string  | Unique order reference (e.g. ORD-date-seq) |
| status         | string  | PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED |
| subtotal       | number  | Sum of items before discount/tax     |
| discount       | number  | Discount amount (e.g. from coupon)   |
| tax            | number  | Tax amount                           |
| shippingCost   | number  | Shipping fee                         |
| total          | number  | Final total                          |
| paymentMethod  | string  | e.g. CASH_ON_DELIVERY                |
| paymentStatus  | string  | PENDING, PAID, FAILED, REFUNDED      |
| couponCode     | string  | Applied coupon (or null)             |
| notes          | string  | Order notes (or null)                |
| createdAt      | string  | ISO date-time                        |
| items          | array   | Order line items (see below)         |

**Order item fields:**

| Field        | Type   | Description           |
|-------------|--------|-----------------------|
| id          | number | Order item ID         |
| productName | string | Product name          |
| sku         | string | Variant SKU           |
| size        | string | Variant size          |
| color       | string | Variant color         |
| quantity    | number | Quantity ordered      |
| unitPrice   | number | Price per unit        |
| totalPrice  | number | unitPrice × quantity  |

**Errors:**

| Status | Condition |
|--------|-----------|
| 400    | Cart is empty; or insufficient stock for a cart item |
| 404    | Shipping address not found or not owned by user (see [Address API](api-address.md)) |
| 401    | Not authenticated                                     |

---

## Other order endpoints (user)

| Method | URL | Description |
|--------|-----|-------------|
| GET    | `/api/v1/orders` | List current user's orders (paginated). Query: `page`, `size` |
| GET    | `/api/v1/orders/{orderNumber}` | Get one order by order number |
| PUT    | `/api/v1/orders/{orderNumber}/cancel` | Cancel order (if allowed) |

---

## Admin: List all orders

**URL:** `GET /api/v1/admin/orders`  
**Auth:** Bearer token with role **ADMIN**.

**Query:** `page` (default 0), `size` (default 20).

Returns a paginated list of all orders (newest first). Same `data` shape as user's order list (`content[]` of order objects with `items`).

---

## Admin: Update order status

**URL:** `PUT /api/v1/admin/orders/{orderNumber}/status`  
**Auth:** Bearer token with role **ADMIN**.

**Path:** `orderNumber` – e.g. `ORD-20260214-0001`.

**Request body:**
```json
{
  "status": "CONFIRMED",
  "notes": "Payment received"
}
```

| Field   | Type   | Required | Description |
|--------|--------|----------|-------------|
| status | string | yes      | One of: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED |
| notes  | string | no       | Optional note for status history |

**Response:** `200 OK` – full order object (same as order detail) with updated `status`.
