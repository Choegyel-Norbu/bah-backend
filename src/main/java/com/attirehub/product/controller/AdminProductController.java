package com.attirehub.product.controller;

import com.attirehub.product.dto.CreateProductRequest;
import com.attirehub.product.dto.CreateProductVariantRequest;
import com.attirehub.product.dto.ProductDetailResponse;
import com.attirehub.product.dto.ProductListResponse;
import com.attirehub.product.dto.UpdateProductRequest;
import com.attirehub.product.dto.UpdateProductVariantRequest;
import com.attirehub.product.dto.VariantResponse;
import com.attirehub.product.service.ProductService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductListResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String category,
            @RequestParam(name = "size_filter", required = false) String sizeFilter,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) Boolean newArrivalsOnly) {
        PagedResponse<ProductListResponse> products =
                productService.getProducts(page, size, sort, category, sizeFilter, color,
                        minPrice, maxPrice, search, featured, trending, onSale, newArrivalsOnly);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestParam(required = false) MultiValueMap<String, MultipartFile> images) {
        ProductDetailResponse created = productService.createProduct(request, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") @Valid UpdateProductRequest request,
            @RequestParam(required = false) MultiValueMap<String, MultipartFile> images) {
        ProductDetailResponse updated = productService.updateProduct(id, request, images);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    /**
     * Deletes a product, its variants, and all variant images from UploadThing.
     * Fails if the product's variants are referenced by orders (e.g. order_items).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<VariantResponse>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductVariantRequest request) {
        VariantResponse created = productService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Variant created successfully", created));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<VariantResponse>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateProductVariantRequest request) {
        VariantResponse updated = productService.updateVariant(productId, variantId, request);
        return ResponseEntity.ok(ApiResponse.success("Variant updated successfully", updated));
    }

    /**
     * Deletes a variant and its associated image from UploadThing (if any).
     */
    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        productService.deleteVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears the image for a variant (sets imageUrl to null).
     * Use this instead of the removed product-level image delete.
     */
    @DeleteMapping("/{productId}/variants/{variantId}/image")
    public ResponseEntity<ApiResponse<Void>> clearVariantImage(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        productService.clearVariantImage(productId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Variant image cleared"));
    }

    /**
     * Deletes a single image from a variant.
     */
    @DeleteMapping("/{productId}/variants/{variantId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteVariantImage(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @PathVariable Long imageId) {
        productService.deleteVariantImage(productId, variantId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Variant image deleted"));
    }

    /**
     * Clears all images for a color variant group.
     */
    @DeleteMapping("/{productId}/variant-groups/{groupId}/images")
    public ResponseEntity<ApiResponse<Void>> clearVariantGroupImages(
            @PathVariable Long productId,
            @PathVariable Long groupId) {
        productService.clearVariantGroupImages(productId, groupId);
        return ResponseEntity.ok(ApiResponse.success("Variant group images cleared"));
    }

    /**
     * Deletes a single image from a color variant group.
     */
    @DeleteMapping("/{productId}/variant-groups/{groupId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteVariantGroupImage(
            @PathVariable Long productId,
            @PathVariable Long groupId,
            @PathVariable Long imageId) {
        productService.deleteVariantGroupImage(productId, groupId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Variant group image deleted"));
    }

    /**
     * Deletes a single size option (product_variant) from a color variant group.
     */
    @DeleteMapping("/{productId}/variant-groups/{groupId}/sizes/{variantId}")
    public ResponseEntity<Void> deleteVariantGroupSize(
            @PathVariable Long productId,
            @PathVariable Long groupId,
            @PathVariable Long variantId) {
        productService.deleteVariantGroupSize(productId, groupId, variantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes an entire color variant group (all size rows and group/variant images).
     */
    @DeleteMapping("/{productId}/variant-groups/{groupId}")
    public ResponseEntity<Void> deleteVariantGroup(
            @PathVariable Long productId,
            @PathVariable Long groupId) {
        productService.deleteVariantGroup(productId, groupId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deprecated: product-level images were removed. Images are now per variant.
     * Returns 410 Gone so clients can migrate to DELETE .../variants/{variantId}/image.
     */
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImageDeprecated(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error(
                        "Product-level images are no longer supported. Use DELETE /api/v1/admin/products/" + productId + "/variants/{variantId}/image to clear a variant image."));
    }
}
