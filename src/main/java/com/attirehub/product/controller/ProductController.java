package com.attirehub.product.controller;

import com.attirehub.product.dto.ProductDetailResponse;
import com.attirehub.product.dto.ProductListResponse;
import com.attirehub.product.dto.VariantResponse;
import com.attirehub.product.service.ProductService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

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

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<ProductListResponse>>> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "7") int days) {
        List<ProductListResponse> trending = productService.getTrendingProducts(limit, days);
        return ResponseEntity.ok(ApiResponse.success(trending));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductBySlug(
            @PathVariable String slug) {
        ProductDetailResponse product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> getProductVariants(
            @PathVariable Long id) {
        List<VariantResponse> variants = productService.getProductVariants(id);
        return ResponseEntity.ok(ApiResponse.success(variants));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<ProductListResponse>>> getRelatedProducts(
            @PathVariable Long id) {
        List<ProductListResponse> related = productService.getRelatedProducts(id);
        return ResponseEntity.ok(ApiResponse.success(related));
    }

    @GetMapping("/slug/{slug}/related")
    public ResponseEntity<ApiResponse<List<ProductListResponse>>> getRelatedProductsBySlug(
            @PathVariable String slug) {
        List<ProductListResponse> related = productService.getRelatedProductsBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(related));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<ProductListResponse>>> suggestProducts(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        List<ProductListResponse> suggestions = productService.suggestProducts(query, limit);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
}
