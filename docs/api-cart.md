# Cart API

**Base path:** `/api/v1/cart`  
**Auth:** Required (Bearer token). All endpoints use the authenticated user's cart.

---

## Add item to cart

**URL:** `POST /api/v1/cart/items`

**Request body:**

```json
{
  "variantId": 3,
  "quantity": 1
}
```

| Field      | Type   | Required | Description                          |
|-----------|--------|----------|--------------------------------------|
| variantId | number | yes      | Product variant ID (from product/variants) |
| quantity  | number | no       | Quantity to add (default: 1, min: 1) |

**Response:** `200 OK`

```json
{
  "success": true,
  "message": "Item added to cart",
  "data": {
    "id": 1,
    "items": [
      {
        "id": 1,
        "variantId": 3,
        "productName": "Sweater",
        "sku": "sweater-m-red",
        "size": "M",
        "color": "red",
        "unitPrice": 2000.00,
        "quantity": 1,
        "totalPrice": 2000.00,
        "imageUrl": null,
        "availableStock": 20
      }
    ],
    "subtotal": 2000.00,
    "totalItems": 1
  },
  "timestamp": "2026-02-14T14:00:00"
}
```

**Cart response fields:**

| Field      | Type    | Description                    |
|-----------|---------|--------------------------------|
| id        | number  | Cart ID                        |
| items     | array   | Cart line items (see below)   |
| subtotal  | number  | Sum of all item totalPrice     |
| totalItems| number  | Sum of all item quantities     |

**Cart item fields:**

| Field          | Type   | Description                |
|----------------|--------|----------------------------|
| id             | number | Cart item ID               |
| variantId      | number | Product variant ID         |
| productName    | string | Product name               |
| sku            | string | Variant SKU                |
| size           | string | Variant size               |
| color          | string | Variant color              |
| unitPrice      | number | Price per unit              |
| quantity       | number | Quantity in cart           |
| totalPrice     | number | unitPrice × quantity       |
| imageUrl       | string | Variant image (or null)   |
| availableStock | number | Current stock for variant  |

---

## Other cart endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET    | `/api/v1/cart` | Get current cart |
| PUT    | `/api/v1/cart/items/{itemId}` | Update item quantity (body: `{ "quantity": 2 }`) |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove one item |
| DELETE | `/api/v1/cart` | Clear entire cart |
