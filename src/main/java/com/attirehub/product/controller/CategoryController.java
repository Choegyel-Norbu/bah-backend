package com.attirehub.product.controller;

import com.attirehub.product.dto.CategoryResponse;
import com.attirehub.product.dto.ProductListResponse;
import com.attirehub.product.service.CategoryService;
import com.attirehub.product.service.ProductService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(
            @PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/{slug}/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductListResponse>>> getProductsByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductListResponse> products =
                productService.getProductsByCategory(slug, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
