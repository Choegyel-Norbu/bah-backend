# Product Images — React (JSX) Frontend Implementation Guide

Base URL: **`/api/v1`**
All responses use **JSON** and the standard `ApiResponse` wrapper.
Image files are stored on **UploadThing cloud** — the frontend uses returned URLs directly.

---

## Table of contents

1. [Overview](#1-overview)
2. [API endpoints](#2-api-endpoints)
3. [API service layer](#3-api-service-layer)
4. [Image upload hook](#4-image-upload-hook)
5. [ImageDropzone component](#5-imagedropzone-component)
6. [Admin product form integration](#6-admin-product-form-integration)
7. [Product image gallery (customer)](#7-product-image-gallery-customer)
8. [Key considerations](#8-key-considerations)

---

## 1. Overview

The backend supports **multipart product creation/update** where the `product` JSON and `images` files are sent together. Images are uploaded to UploadThing and the response includes full UploadThing URLs.

### Architecture

```
React App
  ├── FormData: { product: JSON blob, images: File[] }
  │        ↓
  ├── POST /api/v1/admin/products  (create)
  ├── PUT  /api/v1/admin/products/:id  (update + add images)
  ├── DELETE /api/v1/admin/products/:id/images/:imageId  (remove image)
  │        ↓
  └── Response: product.images[].url → https://utfs.io/f/{fileKey}
```

### Constraints

| Rule | Value |
|------|-------|
| Max file size | 10 MB per file |
| Allowed types | `image/jpeg`, `image/png`, `image/gif`, `image/webp` |
| Max images per request | No hard limit (recommend ≤ 10) |
| Auth required | Admin role (`ROLE_ADMIN`) |

---

## 2. API endpoints

### Create product with images

```
POST /api/v1/admin/products
Content-Type: multipart/form-data
Authorization: Bearer <token>

Parts:
  product  →  JSON blob (application/json)
  images   →  File (repeatable, optional)
```

**Response:** `ApiResponse` with `data.images` array populated.

### Update product (add images)

```
PUT /api/v1/admin/products/:id
Content-Type: multipart/form-data
Authorization: Bearer <token>

Parts:
  product  →  JSON blob (application/json)
  images   →  File (repeatable, optional — new images to add)
```

### Delete a product image

```
DELETE /api/v1/admin/products/:productId/images/:imageId
Authorization: Bearer <token>
```

Removes the image from UploadThing cloud and the database.

### Get product detail (includes images)

```
GET /api/v1/products/:slug
(Public — no auth required)
```

Response includes `images` array with UploadThing URLs.

### Standalone upload (optional)

```
POST /api/v1/uploadthing/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

Params: file, field (default "general"), fileType (default "image")
```

### Health check

```
GET /api/v1/uploadthing/health
(Public — no auth)
```

---

## 3. API service layer

The critical detail: the `product` part must be sent as a **JSON Blob** with `type: 'application/json'` so Spring Boot deserializes it correctly via `@RequestPart`.

```jsx
// src/services/api/productImageApi.js

import { apiClient } from './apiClient'; // your axios instance with baseURL + auth interceptor

/**
 * Builds a FormData with the product JSON blob and optional image files.
 * @param {Object} product - Product data object
 * @param {File[]} [images] - Optional array of image files
 * @returns {FormData}
 */
function buildProductFormData(product, images) {
  const formData = new FormData();

  // Product JSON — must be a Blob with application/json content type
  const productBlob = new Blob([JSON.stringify(product)], {
    type: 'application/json',
  });
  formData.append('product', productBlob);

  // Each image appended with the same key "images"
  if (images && images.length > 0) {
    images.forEach((file) => formData.append('images', file));
  }

  return formData;
}

/**
 * Creates a product with optional images.
 * @param {Object} product - Product payload
 * @param {File[]} [images] - Image files to upload
 * @returns {Promise<Object>} Created product detail
 */
export async function createProduct(product, images) {
  const formData = buildProductFormData(product, images);
  const { data } = await apiClient.post('/api/v1/admin/products', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data.data;
}

/**
 * Updates a product and optionally adds new images.
 * @param {number} id - Product ID
 * @param {Object} product - Fields to update
 * @param {File[]} [images] - New image files to add
 * @returns {Promise<Object>} Updated product detail
 */
export async function updateProduct(id, product, images) {
  const formData = buildProductFormData(product, images);
  const { data } = await apiClient.put(
    `/api/v1/admin/products/${id}`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data.data;
}

/**
 * Deletes a product image (removes from UploadThing + database).
 * @param {number} productId
 * @param {number} imageId
 * @returns {Promise<void>}
 */
export async function deleteProductImage(productId, imageId) {
  await apiClient.delete(
    `/api/v1/admin/products/${productId}/images/${imageId}`
  );
}
```

---

## 4. Image upload hook

Manages local file selection, validation, and previews before the form is submitted.

```jsx
// src/hooks/useImageUpload.js

import { useState, useCallback } from 'react';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

/**
 * Hook for managing image file selection with validation and previews.
 * @param {number} [maxImages=10] - Maximum number of images allowed
 */
export function useImageUpload(maxImages = 10) {
  const [files, setFiles] = useState([]);
  // files shape: [{ file: File, preview: string, id: string }]

  const [error, setError] = useState(null);

  const addFiles = useCallback(
    (newFiles) => {
      setError(null);
      const fileArray = Array.from(newFiles);

      for (const file of fileArray) {
        if (!ALLOWED_TYPES.includes(file.type)) {
          setError(`"${file.name}" is not a supported image type`);
          return;
        }
        if (file.size > MAX_FILE_SIZE) {
          setError(`"${file.name}" exceeds 10 MB limit`);
          return;
        }
      }

      setFiles((prev) => {
        const remaining = maxImages - prev.length;
        if (remaining <= 0) {
          setError(`Maximum ${maxImages} images allowed`);
          return prev;
        }
        const toAdd = fileArray.slice(0, remaining).map((file) => ({
          file,
          preview: URL.createObjectURL(file),
          id: `${file.name}-${Date.now()}-${Math.random()}`,
        }));
        return [...prev, ...toAdd];
      });
    },
    [maxImages]
  );

  const removeFile = useCallback((id) => {
    setFiles((prev) => {
      const target = prev.find((f) => f.id === id);
      if (target) URL.revokeObjectURL(target.preview);
      return prev.filter((f) => f.id !== id);
    });
  }, []);

  const clearFiles = useCallback(() => {
    files.forEach((f) => URL.revokeObjectURL(f.preview));
    setFiles([]);
  }, [files]);

  /** Returns raw File objects for FormData. */
  const getFilesForUpload = useCallback(
    () => files.map((f) => f.file),
    [files]
  );

  return { files, error, addFiles, removeFile, clearFiles, getFilesForUpload };
}
```

---

## 5. ImageDropzone component

Drag-and-drop zone that displays both **existing** server images and **new** locally-selected files.

```jsx
// src/components/admin/ImageDropzone.jsx

import { useCallback, useRef } from 'react';
import './ImageDropzone.css';

/**
 * @param {Object} props
 * @param {Array} [props.existingImages] - Images already saved on the server
 * @param {Function} [props.onExistingImageRemove] - Called with imageId when user removes a server image
 * @param {number} [props.maxImages] - Max total images (existing + new)
 * @param {Array} props.files - State from useImageUpload()
 * @param {string|null} props.error - Validation error from useImageUpload()
 * @param {Function} props.addFiles - From useImageUpload()
 * @param {Function} props.removeFile - From useImageUpload()
 */
export function ImageDropzone({
  existingImages = [],
  onExistingImageRemove,
  maxImages = 10,
  files,
  error,
  addFiles,
  removeFile,
}) {
  const inputRef = useRef(null);

  const handleDrop = useCallback(
    (e) => {
      e.preventDefault();
      if (e.dataTransfer.files.length) addFiles(e.dataTransfer.files);
    },
    [addFiles]
  );

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
  }, []);

  const totalImages = existingImages.length + files.length;

  return (
    <div>
      {/* Drop zone */}
      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onClick={() => inputRef.current?.click()}
        className="image-dropzone"
      >
        <p className="dropzone-text">
          Drop images here or click to browse
        </p>
        <p className="dropzone-hint">
          JPEG, PNG, GIF, WebP — max 10 MB each
          ({totalImages}/{maxImages})
        </p>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept="image/jpeg,image/png,image/gif,image/webp"
          onChange={(e) => e.target.files && addFiles(e.target.files)}
          hidden
        />
      </div>

      {/* Validation error */}
      {error && <p className="dropzone-error">{error}</p>}

      {/* Preview grid */}
      <div className="image-preview-grid">
        {/* Existing images (saved on server) */}
        {existingImages.map((img) => (
          <div key={`existing-${img.id}`} className="image-preview-item">
            <img src={img.url} alt={img.originalFilename} />
            {onExistingImageRemove && (
              <button
                type="button"
                className="image-remove-btn"
                onClick={() => onExistingImageRemove(img.id)}
              >
                &times;
              </button>
            )}
          </div>
        ))}

        {/* New files (not yet uploaded) */}
        {files.map((f) => (
          <div key={f.id} className="image-preview-item image-preview-new">
            <img src={f.preview} alt={f.file.name} />
            <button
              type="button"
              className="image-remove-btn"
              onClick={() => removeFile(f.id)}
            >
              &times;
            </button>
            <span className="image-new-badge">NEW</span>
          </div>
        ))}
      </div>
    </div>
  );
}
```

### ImageDropzone.css

```css
/* src/components/admin/ImageDropzone.css */

.image-dropzone {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  background: #fafafa;
  transition: border-color 0.2s;
}

.image-dropzone:hover {
  border-color: #3b82f6;
}

.dropzone-text {
  margin: 0;
  color: #6b7280;
}

.dropzone-hint {
  margin: 0.5rem 0 0;
  font-size: 0.875rem;
  color: #9ca3af;
}

.dropzone-error {
  color: #ef4444;
  font-size: 0.875rem;
  margin-top: 0.5rem;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.image-preview-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.image-preview-item img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.image-preview-new {
  border: 2px solid #3b82f6;
}

.image-remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-remove-btn:hover {
  background: #dc2626;
}

.image-new-badge {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: #3b82f6;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}
```

---

## 6. Admin product form integration

Shows how to wire the dropzone and upload hook into a create/edit product form.

```jsx
// src/pages/admin/AdminProductForm.jsx

import { useState } from 'react';
import { useImageUpload } from '../../hooks/useImageUpload';
import { ImageDropzone } from '../../components/admin/ImageDropzone';
import {
  createProduct,
  updateProduct,
  deleteProductImage,
} from '../../services/api/productImageApi';

/**
 * @param {Object} props
 * @param {Object} [props.existingProduct] - Product to edit (null for create)
 * @param {Function} [props.onSuccess] - Called with saved product after success
 */
export function AdminProductForm({ existingProduct, onSuccess }) {
  const { files, error, addFiles, removeFile, clearFiles, getFilesForUpload } =
    useImageUpload(10);

  const [existingImages, setExistingImages] = useState(
    existingProduct?.images ?? []
  );
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Remove an image that is already on the server
  const handleExistingImageRemove = async (imageId) => {
    if (!existingProduct) return;
    try {
      await deleteProductImage(existingProduct.id, imageId);
      setExistingImages((prev) => prev.filter((img) => img.id !== imageId));
    } catch (err) {
      console.error('Failed to delete image:', err);
    }
  };

  const handleSubmit = async (formValues) => {
    setIsSubmitting(true);
    try {
      const newImages = getFilesForUpload();
      let result;

      if (existingProduct) {
        result = await updateProduct(existingProduct.id, formValues, newImages);
      } else {
        result = await createProduct(formValues, newImages);
      }

      clearFiles();
      setExistingImages(result.images);
      onSuccess?.(result);
    } catch (err) {
      console.error('Failed to save product:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={/* connect to your form handler */}>

      {/* ... Name, price, category, brand, material, variants ... */}

      {/* Image upload section */}
      <section>
        <label>Product Images</label>
        <ImageDropzone
          existingImages={existingImages}
          onExistingImageRemove={handleExistingImageRemove}
          maxImages={10}
          files={files}
          error={error}
          addFiles={addFiles}
          removeFile={removeFile}
        />
      </section>

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting
          ? 'Saving...'
          : existingProduct
            ? 'Update Product'
            : 'Create Product'}
      </button>
    </form>
  );
}
```

---

## 7. Product image gallery (customer)

A simple gallery with main image + thumbnail strip for the product detail page.

```jsx
// src/components/product/ProductImageGallery.jsx

import { useState } from 'react';
import './ProductImageGallery.css';

/**
 * @param {Object} props
 * @param {Array} props.images - ProductImage objects from API
 * @param {string} [props.fallbackUrl] - Legacy imageUrl field as fallback
 * @param {string} [props.alt] - Alt text for images
 */
export function ProductImageGallery({ images, fallbackUrl, alt = 'Product' }) {
  const [selectedIndex, setSelectedIndex] = useState(0);

  // Fall back to legacy imageUrl if no uploaded images exist
  const displayImages =
    images.length > 0
      ? images.map((img) => ({ url: img.url, alt: img.originalFilename }))
      : fallbackUrl
        ? [{ url: fallbackUrl, alt }]
        : [];

  if (displayImages.length === 0) {
    return (
      <div className="gallery-placeholder">
        No images available
      </div>
    );
  }

  return (
    <div className="product-gallery">
      {/* Main image */}
      <img
        src={displayImages[selectedIndex].url}
        alt={displayImages[selectedIndex].alt}
        className="gallery-main-image"
      />

      {/* Thumbnails — only shown when there are multiple images */}
      {displayImages.length > 1 && (
        <div className="gallery-thumbnails">
          {displayImages.map((img, idx) => (
            <img
              key={idx}
              src={img.url}
              alt={img.alt}
              onClick={() => setSelectedIndex(idx)}
              className={`gallery-thumb ${idx === selectedIndex ? 'active' : ''}`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
```

### ProductImageGallery.css

```css
/* src/components/product/ProductImageGallery.css */

.product-gallery {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.gallery-main-image {
  width: 100%;
  max-height: 500px;
  object-fit: contain;
  border-radius: 12px;
  background: #f9fafb;
}

.gallery-thumbnails {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
}

.gallery-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  opacity: 0.6;
  transition: all 0.2s;
}

.gallery-thumb:hover {
  opacity: 0.85;
}

.gallery-thumb.active {
  border-color: #3b82f6;
  opacity: 1;
}

.gallery-placeholder {
  width: 100%;
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  border-radius: 12px;
  color: #9ca3af;
  font-size: 1rem;
}
```

---

## 8. Key considerations

### FormData structure

The backend uses `@RequestPart("product")` and `@RequestPart("images")`.
The `product` part **must** be a `Blob` with `type: 'application/json'`:

```javascript
const blob = new Blob([JSON.stringify(productData)], { type: 'application/json' });
formData.append('product', blob);
```

Do **not** append the product as a plain string — Spring will reject it.

### Multiple images

Append each file with the **same key** `images`:

```javascript
files.forEach((file) => formData.append('images', file));
```

### No images

Simply omit the `images` parts. The backend treats them as optional.

### Image URLs

Use `image.url` directly from the API response (`https://utfs.io/f/...`).
No proxy or signed-URL logic is needed.

### Backward compatibility

The legacy `imageUrl` string field on products still exists.
The new `images` array is the recommended approach going forward.
Customer-facing components should check `images` first, fall back to `imageUrl`.

### Error handling

| HTTP Status | Meaning |
|-------------|---------|
| 400 | Invalid file type, file too large, or validation error |
| 401 | Missing or invalid auth token |
| 403 | Not an admin |
| 404 | Product or image not found |
| 500 | UploadThing service failure |

### Cleanup on revoke

Always call `URL.revokeObjectURL(preview)` when a local preview is removed
or the component unmounts to avoid memory leaks.

---

## Summary

| File | Purpose |
|------|---------|
| `src/services/api/productImageApi.js` | API calls: create, update, delete image |
| `src/hooks/useImageUpload.js` | Local file selection, validation, previews |
| `src/components/admin/ImageDropzone.jsx` | Drag-and-drop upload with preview grid |
| `src/components/admin/ImageDropzone.css` | Dropzone styles |
| `src/pages/admin/AdminProductForm.jsx` | Admin form wiring images into create/update |
| `src/components/product/ProductImageGallery.jsx` | Customer-facing main image + thumbnails |
| `src/components/product/ProductImageGallery.css` | Gallery styles |
