# Product Reviews API

Base URL: **`/api/v1/products`**  
Responses use the standard **`ApiResponse<T>`** wrapper.  
Reviews are stored at **variant level** (size/color), but can be listed per product or per variant.  
**Only users who have purchased the variant** (order status DELIVERED or SHIPPED) may create or update a review. One review per user per variant.

---

## Overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/{productId}/reviews` | Public | List reviews for a product (all variants) or for a specific variant (paginated) |
| POST | `/{productId}/reviews` | Required | Create or replace own review for a specific variant (verified purchase required) |
| PUT | `/{productId}/reviews/{reviewId}` | Required | Update own review |
| DELETE | `/{productId}/reviews/{reviewId}` | Required | Delete own review |

---

## 1. List reviews (public)

**Endpoint:** `GET /api/v1/products/{productId}/reviews`

**Query parameters**

| Parameter  | Type | Default | Description |
|------------|------|---------|-------------|
| `page`     | int  | 0       | Page index |
| `size`     | int  | 10      | Page size |
| `variantId` | long | —      | If provided, only reviews for this variant are returned. If omitted, reviews for all variants of the product are returned. |

**Success response:** `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "productId": 10,
        "variantId": 101,
        "userId": 5,
        "userDisplayName": "Jane Doe",
        "rating": 5,
        "comment": "Great quality.",
        "verifiedPurchase": true,
        "createdAt": "2025-02-19T12:00:00",
        "updatedAt": "2025-02-19T12:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

## 2. Create review (authenticated, verified purchase only)

**Endpoint:** `POST /api/v1/products/{productId}/reviews?variantId={variantId}`  
**Auth:** Bearer token required.

**Query parameters**

| Parameter   | Type | Required | Description |
|------------|------|----------|-------------|
| `variantId` | long | Yes      | Variant being reviewed. Must belong to the product in the path. |

**Body**

| Field    | Type   | Required | Constraints |
|----------|--------|----------|-------------|
| `rating` | int    | Yes      | 1–5 |
| `comment`| string | No       | Max 2000 characters |

**Business rules**

- User must have purchased the variant (at least one order item in a **DELIVERED** or **SHIPPED** order).
- If the user already has a review for this variant, it is **updated** (same unique constraint).

**Success:** `201 Created` with the created/updated review.  
**Errors:** `400 Bad Request` if the user has not purchased the product.

---

## 3. Update review (authenticated)

**Endpoint:** `PUT /api/v1/products/{productId}/reviews/{reviewId}`  
**Auth:** Bearer token required. User may only update their own review.

**Body:** Same as create (`rating`, `comment`).

**Success:** `200 OK` with updated review.  
**Errors:** `404` if review not found or not owned by the user.

---

## 4. Delete review (authenticated)

**Endpoint:** `DELETE /api/v1/products/{productId}/reviews/{reviewId}`  
**Auth:** Bearer token required. User may only delete their own review.

**Success:** `200 OK`.  
**Errors:** `404` if review not found or not owned by the user.

---

## Product rating aggregates

Product list and detail responses now include:

- **`averageRating`** — Average of all review ratings (1–5), or `null` if no reviews.
- **`reviewCount`** — Total number of reviews.

These are updated automatically when reviews are created, updated, or deleted.
