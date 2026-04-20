# Product & Catalog API — Browsing products and categories

Base URL: **`/api/v1`**  
All responses use **JSON** and the standard `ApiResponse<T>` wrapper.  
Product and category endpoints are **public** (no `Authorization` header required).

---

## Table of contents

1. [Overview](#1-overview)
2. [Products API](#2-products-api)
3. [Categories API](#3-categories-api)
4. [Response wrappers](#4-response-wrappers)
5. [Errors](#5-errors)
6. [Data model reference](#6-data-model-reference)
7. [Summary](#7-summary)

---

## 1. Overview

The **Product & Catalog** module exposes read-only APIs for:

- **Products** — Paginated list with filters (category, size, color, price, search, featured, trending), product detail by slug, variants, related products, and trending list.
- **Categories** — Tree of categories and products per category.

| Resource    | Base path              | Auth   |
|------------|-------------------------|--------|
| Products   | `/api/v1/products`      | Public |
| Categories | `/api/v1/categories`    | Public |

All endpoints return `200 OK` with `ApiResponse.success(data)` unless stated otherwise. Not-found cases return `404` with an error payload.

---

## 2. Products API

Base path: **`/api/v1/products`**

### 2.1 List products (with filters and pagination)

**Endpoint:** `GET /api/v1/products`  
**Auth:** None (public)

#### Query parameters

| Parameter       | Type    | Default | Description |
|----------------|---------|---------|-------------|
| `page`         | int     | `0`     | Page index (0-based). |
| `size`         | int     | `20`    | Page size. |
| `sort`         | string  | —       | Sort field and direction. Format: `field,direction` (e.g. `basePrice,asc`, `createdAt,desc`). If omitted, sorts by `createdAt` descending. |
| `category`     | string  | —       | Filter by category **slug** (e.g. `mens-shirts`). |
| `size_filter`  | string  | —       | Filter by variant size (e.g. `M`, `L`). |
| `color`        | string  | —       | Filter by variant color (case-insensitive). |
| `minPrice`     | decimal | —       | Minimum product `basePrice` (inclusive). |
| `maxPrice`     | decimal | —       | Maximum product `basePrice` (inclusive). |
| `search`       | string  | —       | Search in product **name** (case-insensitive, partial match). |
| `featured`     | boolean | —       | If `true`, only featured products. |
| `trending`     | boolean | —       | If `true`, only trending products (shop owner curated). |
| `onSale`       | boolean | —       | If `true`, only products with at least one variant where discount &gt; 0. |
| `newArrivalsOnly` | boolean | —   | If `true`, only products marked as **new arrival** (admin-set flag). |

Only **active** products are returned. Combining multiple query params applies AND logic. Each product in the list includes **`newArrival`** (admin-set; true when product is marked as new arrival) and **`isTrending`** (true if marked trending by shop owner).

#### Example request

```http
GET /api/v1/products?page=0&size=10&category=womens-dresses&minPrice=29&maxPrice=99&sort=basePrice,asc
```

#### Success response

**Status:** `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Summer Floral Dress",
        "slug": "summer-floral-dress",
        "basePrice": 49.99,
        "categoryName": "Dresses",
        "categorySlug": "womens-dresses",
        "brand": "AttireHub",
        "imageUrl": "https://example.com/img/1.jpg",
        "isFeatured": false,
        "newArrival": true,
        "variants": []
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "last": false
  },
  "timestamp": "2026-02-14T10:00:00"
}
```

#### Product list item fields

| Field           | Type    | Description |
|----------------|---------|-------------|
| `id`           | long    | Product ID. |
| `name`        | string  | Product name. |
| `slug`        | string  | URL-friendly unique identifier. |
| `basePrice`   | number  | Base price. |
| `categoryName`| string  | Category display name. |
| `categorySlug` | string  | Category slug. |
| `brand`       | string  | Brand name (nullable). |
| `imageUrl`    | string  | Main image URL (nullable). |
| `isFeatured`  | boolean | Whether the product is featured. |
| `isTrending`  | boolean | Whether the product is marked trending by shop owner. |
| `newArrival`  | boolean | `true` if product is marked as new arrival (admin-set). |
| `variants`    | array   | Active variants (id, sku, size, color, price, etc.). |

---

### 2.2 Get product by slug (detail)

**Endpoint:** `GET /api/v1/products/{slug}`  
**Auth:** None (public)

Returns full product detail including **all active variants** (size, color, price, stock, etc.).

#### Example request

```http
GET /api/v1/products/summer-floral-dress
```

#### Success response

**Status:** `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "Summer Floral Dress",
    "slug": "summer-floral-dress",
    "description": "Light cotton dress with floral print.",
    "basePrice": 49.99,
    "categoryName": "Dresses",
    "categorySlug": "womens-dresses",
    "brand": "AttireHub",
    "material": "Cotton",
    "imageUrl": "https://example.com/img/1.jpg",
    "isFeatured": false,
    "isTrending": false,
    "newArrival": false,
    "variants": [
      {
        "id": 101,
        "sku": "SFD-S-RED",
        "size": "S",
        "color": "Red",
        "price": 49.99,
        "stockQuantity": 10,
        "imageUrl": null,
        "isActive": true
      },
      {
        "id": 102,
        "sku": "SFD-M-RED",
        "size": "M",
        "color": "Red",
        "price": 49.99,
        "stockQuantity": 5,
        "imageUrl": null,
        "isActive": true
      }
    ]
  },
  "timestamp": "2026-02-14T10:00:00"
}
```

#### Product detail fields

| Field          | Type   | Description |
|----------------|--------|-------------|
| `id`           | long   | Product ID. |
| `name`         | string | Product name. |
| `slug`         | string | Product slug. |
| `description`  | string | Full description (nullable). |
| `basePrice`    | number | Base price. |
| `categoryName` | string | Category name. |
| `categorySlug` | string | Category slug. |
| `brand`        | string | Brand (nullable). |
| `material`     | string | Material (nullable). |
| `imageUrl`     | string | Main image (nullable). |
| `isFeatured`   | boolean| Featured flag. |
| `isTrending`   | boolean| Trending flag (shop owner curated). |
| `newArrival`   | boolean| New arrival flag (admin-set). |
| `variants`     | array  | List of [variant objects](#variant-fields). |

#### Variant fields

| Field           | Type    | Description |
|-----------------|---------|-------------|
| `id`            | long    | Variant ID (use for cart/order line items). |
| `sku`           | string  | Unique SKU. |
| `size`          | string  | Size (e.g. S, M, L). |
| `color`         | string  | Color. |
| `price`         | number  | Price for this variant. |
| `stockQuantity` | int     | Available stock. |
| `imageUrl`      | string  | Variant image (nullable). |
| `isActive`      | boolean | Whether variant is sellable. |

#### Error response

**Status:** `404 Not Found` — when no product exists with the given `slug`.

```json
{
  "success": false,
  "message": "Product not found with slug: invalid-slug",
  "data": null,
  "timestamp": "2026-02-14T10:00:00"
}
```

---

### 2.3 Get product variants by product ID

**Endpoint:** `GET /api/v1/products/{id}/variants`  
**Auth:** None (public)

Returns only the **variants** for the product (useful when you already have product detail and need to refresh stock/SKU list).  
`{id}` is the **product ID** (numeric), not the slug. Only **active** variants are returned.

#### Example request

```http
GET /api/v1/products/1/variants
```

#### Success response

**Status:** `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 101,
      "sku": "SFD-S-RED",
      "size": "S",
      "color": "Red",
      "price": 49.99,
      "stockQuantity": 10,
      "imageUrl": null,
      "isActive": true
    }
  ],
  "timestamp": "2026-02-14T10:00:00"
}
```

#### Error response

**Status:** `404 Not Found` — when product `id` does not exist (behavior depends on your global exception handler; typically same structure as above).

---

### 2.4 Get related products

**Endpoint:** `GET /api/v1/products/{id}/related`  
**Auth:** None (public)

Returns up to **4 related products** from the same category (excluding the current product).  
`{id}` is the **product ID** (numeric). Items have the same shape as [product list items](#product-list-item-fields).

#### Example request

```http
GET /api/v1/products/1/related
```

#### Success response

**Status:** `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 2,
      "name": "Another Dress",
      "slug": "another-dress",
      "basePrice": 59.99,
      "categoryName": "Dresses",
      "categorySlug": "womens-dresses",
      "brand": "AttireHub",
      "imageUrl": "https://example.com/img/2.jpg",
      "isFeatured": false
    }
  ],
  "timestamp": "2026-02-14T10:00:00"
}
```

If the product has no category or no other products in that category, `data` is an empty array.

---

### 2.5 Trending products

**Endpoint:** `GET /api/v1/products/trending`  
**Auth:** None (public)

Returns products marked as **trending** by shop owners (via admin product create/update). Sorted by most recently updated first.

#### Query parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit`   | int  | `10`    | Max number of products to return. |
| `days`    | int  | `7`     | Reserved for future use; currently ignored. |

#### Example request

```http
GET /api/v1/products/trending?limit=8
```

#### Success response

**Status:** `200 OK` — Same shape as [product list items](#product-list-item-fields). Each item includes `isTrending: true`. Empty array if no products are marked trending.

---

## 3. Categories API

Base path: **`/api/v1/categories`**

### 3.1 List all categories (tree)

**Endpoint:** `GET /api/v1/categories`  
**Auth:** None (public)

Returns **root categories** only. Each category can have a nested `children` array (tree structure). Only **active** categories are included (implementation may filter by `isActive` at repository level).

#### Example request

```http
GET /api/v1/categories
```

#### Success response

**Status:** `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "name": "Women",
      "slug": "women",
      "description": "Women's clothing",
      "displayOrder": 0,
      "imageUrl": "https://example.com/cat/women.jpg",
      "parentId": null,
      "children": [
        {
          "id": 2,
          "name": "Dresses",
          "slug": "womens-dresses",
          "description": null,
          "displayOrder": 0,
          "imageUrl": null,
          "parentId": 1,
          "children": []
        }
      ]
    }
  ],
  "timestamp": "2026-02-14T10:00:00"
}
```

#### Category fields

| Field          | Type   | Description |
|----------------|--------|-------------|
| `id`           | long   | Category ID. |
| `name`         | string | Display name. |
| `slug`         | string | URL-friendly unique slug (use for filtering products). |
| `description`  | string | Optional description. |
| `displayOrder` | int    | Order for display. |
| `imageUrl`     | string | Optional image URL. |
| `parentId`     | long   | Parent category ID; `null` for roots. |
| `children`     | array  | Nested categories (same structure). |

---

### 3.2 Get category by slug

**Endpoint:** `GET /api/v1/categories/{slug}`  
**Auth:** None (public)

Returns a single category by slug (including its `children` if any).

#### Example request

```http
GET /api/v1/categories/womens-dresses
```

#### Success response

**Status:** `200 OK` — same `CategoryResponse` shape as one element in the list above.

#### Error response

**Status:** `404 Not Found` — when no category exists with the given slug.

---

### 3.3 Get products by category slug

**Endpoint:** `GET /api/v1/categories/{slug}/products`  
**Auth:** None (public)

Returns **paginated products** that belong to the given category (by slug). Same response shape as [List products](#21-list-products-with-filters-and-pagination) (`PagedResponse<ProductListResponse>`).

#### Query parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page`    | int  | `0`     | Page index. |
| `size`    | int  | `20`    | Page size. |

#### Example request

```http
GET /api/v1/categories/womens-dresses/products?page=0&size=10
```

#### Success response

**Status:** `200 OK` — `data` is a `PagedResponse` with `content` array of product list items, plus `page`, `size`, `totalElements`, `totalPages`, `last`.

---

## 4. Response wrappers

All endpoints use the shared wrapper:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2026-02-14T10:00:00"
}
```

- **Paginated** endpoints put pagination metadata and list in `data`:
  - `content`: array of items
  - `page`, `size`, `totalElements`, `totalPages`, `last`

---

## 5. Errors

| Status | Meaning |
|--------|--------|
| `400` | Bad request (e.g. invalid query params). |
| `404` | Resource not found (e.g. product or category slug/id not found). |
| `500` | Internal server error. |

Error body uses the same `ApiResponse` structure with `success: false` and `message` describing the error.

---

## 6. Data model reference

- **Product**: `name`, `slug`, `description`, `basePrice`, `category`, `brand`, `material`, `isActive`, `isFeatured`, `isNewArrival`, `isTrending`, `imageUrl`. One-to-many **ProductVariant**.
- **ProductVariant**: `product`, `sku`, `size`, `color`, `price`, `stockQuantity`, `imageUrl`, `isActive`; used for cart/order line items.
- **Category**: `name`, `slug`, `description`, `parent` (self-reference), `displayOrder`, `isActive`, `imageUrl`. Tree built via `children`.

Product list and detail APIs only expose **active** products; variant endpoints only expose **active** variants.

---

## 7. Summary

| Action              | Method | Path                                | Auth   |
|---------------------|--------|-------------------------------------|--------|
| List products       | GET    | `/api/v1/products`                  | Public |
| Product by slug     | GET    | `/api/v1/products/{slug}`          | Public |
| Product variants    | GET    | `/api/v1/products/{id}/variants`   | Public |
| Related products    | GET    | `/api/v1/products/{id}/related`    | Public |
| Trending products   | GET    | `/api/v1/products/trending`        | Public |
| List categories     | GET    | `/api/v1/categories`                | Public |
| Category by slug    | GET    | `/api/v1/categories/{slug}`        | Public |
| Products by category| GET    | `/api/v1/categories/{slug}/products`| Public |

Base URL when running locally (default port 8081): **`http://localhost:8081`**

---

**Next steps:** After integrating the catalog in your frontend, you can implement the **Cart** module (add/update/remove items by `productVariantId`) and then **Checkout / Orders** using the same product and variant IDs.
