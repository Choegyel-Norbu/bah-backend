# Frontend Changes: Variant-Only Images

Images are stored **on color variant groups** (no product-level image). Each **color group** can have **multiple images**. Uploads are grouped by **group index**.

---

## 1. Request body (create / update product)

**Content-Type:** `multipart/form-data`

| Part     | Type | Required | Description |
|----------|------|----------|-------------|
| `product` | JSON blob (`application/json`) | Yes | Product + variants. **Do not** send product-level image fields. |
| `images[i]` | File (repeatable)          | No  | **Multi-image per color group by index:** append 1..N files under the same key `images[0]` for group 0, `images[1]` for group 1, etc. |

### Create product – `product` JSON (no product-level image)

```json
{
  "name": "Classic Tee",
  "slug": "classic-tee",
  "description": "A comfortable cotton tee.",
  "basePrice": 29.99,
  "categoryId": 1,
  "brand": "AttireHub",
  "material": "Cotton",
  "isActive": true,
  "isFeatured": false,
  "isNewArrival": false,
  "isTrending": false,
  "variants": [
    { "size": "S", "color": "White", "price": 29.99, "stockQuantity": 10, "isActive": true },
    { "size": "M", "color": "White", "price": 29.99, "stockQuantity": 15, "isActive": true },
    { "size": "L", "color": "Black", "price": 29.99, "stockQuantity": 8, "isActive": true }
  ]
}
```

**Multipart:** Append the above as a single part named `product` with `type: 'application/json'`.
Then append **color group images grouped by index**:
- `images[0]`: 1..N files for group 0 (e.g. Yellow)
- `images[1]`: 1..N files for group 1 (e.g. Black)
- ...

```javascript
// groupImageFilesByIndex[i] = File[] for group i (possibly empty)
groupImageFilesByIndex.forEach((files, i) => {
  (files || []).forEach((file) => {
    if (file && file.size > 0) formData.append(`images[${i}]`, file);
  });
});
```

---

## 2. Response body (create / update product)

Product has **no** product-level images. Each **variant group** has `images[]` (ordered), with `primary` + `sortOrder`.

### Create product – full response

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 11,
    "name": "Classic Tee",
    "slug": "classic-tee",
    "description": "A comfortable cotton tee.",
    "basePrice": 29.99,
    "categoryName": "Women",
    "categorySlug": "women",
    "brand": "AttireHub",
    "material": "Cotton",
    "featured": false,
    "trending": false,
    "newArrival": false,
    "averageRating": null,
    "reviewCount": 0,
    "variants": [
      {
        "id": 23,
        "sku": "classic-tee-s-white",
        "size": "S",
        "color": "White",
        "price": 29.99,
        "discount": 0,
        "stockQuantity": 10,
        "active": true,
        "images": [
          { "id": 101, "imageUrl": "https://utfs.io/f/abc123...", "primary": true, "sortOrder": 0 },
          { "id": 102, "imageUrl": "https://utfs.io/f/xyz999...", "primary": false, "sortOrder": 1 }
        ]
      },
      {
        "id": 24,
        "sku": "classic-tee-m-white",
        "size": "M",
        "color": "White",
        "price": 29.99,
        "discount": 0,
        "stockQuantity": 15,
        "imageUrl": "https://utfs.io/f/def456...",
        "active": true
      },
      {
        "id": 25,
        "sku": "classic-tee-l-black",
        "size": "L",
        "color": "Black",
        "price": 29.99,
        "discount": 0,
        "stockQuantity": 8,
        "imageUrl": null,
        "active": true
      }
    ]
  },
  "timestamp": "2026-03-15T10:00:00"
}
```

### Get product by slug – same shape (no `product.images`)

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 11,
    "name": "Classic Tee",
    "slug": "classic-tee",
    "description": "A comfortable cotton tee.",
    "basePrice": 29.99,
    "categoryName": "Women",
    "categorySlug": "women",
    "brand": "AttireHub",
    "material": "Cotton",
    "featured": false,
    "trending": false,
    "newArrival": false,
    "averageRating": null,
    "reviewCount": 0,
    "variants": [
      { "id": 23, "sku": "...", "size": "S", "color": "White", "price": 29.99, "discount": 0, "stockQuantity": 10, "active": true, "images": [ { "id": 1, "imageUrl": "https://utfs.io/f/...", "primary": true, "sortOrder": 0 } ] },
      { "id": 24, "sku": "...", "size": "M", "color": "White", "price": 29.99, "discount": 0, "stockQuantity": 15, "active": true, "images": [ { "id": 2, "imageUrl": "https://utfs.io/f/...", "primary": true, "sortOrder": 0 } ] },
      { "id": 25, "sku": "...", "size": "L", "color": "Black", "price": 29.99, "discount": 0, "stockQuantity": 8, "active": true, "images": [] }
    ]
  },
  "timestamp": "2026-03-15T10:00:00"
}
```

---

## 3. Logic changes on the frontend

### 3.1 Removed / changed

| Before (product-level images) | After (variant-level images) |
|-------------------------------|-------------------------------|
| `product.imageUrl` or `product.images[]` in API | **Removed.** Product has no image fields. |
| Single “product images” list (any number of files) | **Images grouped per color group;** keys are `images[i]` where `i` = group index. |
| `DELETE /api/v1/admin/products/:productId/images/:imageId` | **Removed.** To change a variant’s image, send a new image at that variant’s index on update. |

### 3.2 Admin form: create product

- You have a list of **color groups** (e.g. Yellow, Black).
- For **each color group**, add a **multi-image** input (or multiple slots in state).
- When submitting:
  - Build `product` JSON with **no** `imageUrl`. Include `variants` in a **stable order** (e.g. form order).
  - Build images so that **all files for `variantGroups[i]` are appended under `images[i]`**.
- Send `FormData`: part `product` (JSON blob) + file parts `images[i]` (repeatable).

### 3.3 Admin form: update product

- Load product; show existing **variantGroups** with their `group.images[]`.
- For each group, allow adding/removing images.
- When submitting:
  - Build `product` JSON with only the fields you want to update (again **no** `imageUrl` on product). Include **all** variants in the **same order** as the API (e.g. by variant `id` or current list order).
  - Build images: for each group index `i`, append any newly selected files under `images[i]`.
- Send `FormData` as for create.

### 3.4 Building the group images (create and update)

- Keep **variant order** stable (e.g. same as in `product.variants`).
- Per variant, store either a `File` or “no file”.
- When building `FormData`:

```javascript
function buildProductFormData(product, variantImageFiles) {
  const formData = new FormData();
  formData.append('product', new Blob([JSON.stringify(product)], { type: 'application/json' }));

  // groupImageFilesByIndex[i] = File[] for group i (possibly empty)
  (groupImageFilesByIndex || []).forEach((files, i) => {
    (files || []).forEach((file) => {
      if (file && file.size > 0) formData.append(`images[${i}]`, file);
    });
  });
  return formData;
}
```

### 3.5 Customer-facing: product detail / listing

- **Product** no longer has `imageUrl` or `images`. Use **variants** for images.
- **Main image:** use the selected **color group’s** primary image (or first image), or a placeholder.
- **Gallery:** use `group.images[]`.
- **Listing card:** use the first group’s primary image (or first available).

Example (product detail – main image from selected variant):

```javascript
const selectedVariant = variants.find((v) => v.id === selectedVariantId) || variants[0];
const mainImageUrl = selectedVariant?.images?.find((img) => img.primary)?.imageUrl
  ?? selectedVariant?.images?.[0]?.imageUrl
  ?? null;
```

Example (all variant images for a simple gallery):

```javascript
const galleryUrls = (variants || [])
  .flatMap((v) => (v.images || []).map((img) => img.imageUrl))
  .filter(Boolean);
```

---

## 4. API service layer (minimal change)

- **Remove** any `deleteProductImage(productId, imageId)` call and the endpoint.
- **Keep** `createProduct(product, images)` and `updateProduct(id, product, images)` but ensure:
  - `product` never includes `imageUrl`.
  - `images` is an array of `File` (or empty `File`) **in variant order**, length = `product.variants.length`.

---

## 5. Summary

| Topic | Change |
|-------|--------|
| **Request** | `product` has no product-level images. Upload files under `images[i]` (repeatable) where `i` is variant index. |
| **Response** | No product-level images. Each `data.variants[i].images[]` contains URLs + primary/sortOrder. |
| **Admin form** | One image input per variant row; submit order = variant order. |
| **Customer UI** | Use `variant.imageUrl` (e.g. selected variant or first with image) for main image and gallery. |
| **Delete image** | No dedicated endpoint; send update with empty file at that variant index to clear, or leave as-is and only replace when user picks a new image. |
