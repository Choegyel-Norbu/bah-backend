package com.attirehub.product.service;

import com.attirehub.product.dto.CreateProductRequest;
import com.attirehub.product.dto.CreateProductVariantRequest;
import com.attirehub.product.dto.ProductDetailResponse;
import com.attirehub.product.dto.ProductListResponse;
import com.attirehub.product.dto.UpdateProductRequest;
import com.attirehub.product.dto.UpdateProductVariantRequest;
import com.attirehub.product.dto.VariantResponse;
import com.attirehub.shared.dto.PagedResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductDetailResponse createProduct(CreateProductRequest request, MultiValueMap<String, MultipartFile> images);

    ProductDetailResponse updateProduct(Long id, UpdateProductRequest request, MultiValueMap<String, MultipartFile> images);

    VariantResponse createVariant(Long productId, CreateProductVariantRequest request);

    VariantResponse updateVariant(Long productId, Long variantId, UpdateProductVariantRequest request);

    void clearVariantImage(Long productId, Long variantId);

    void deleteVariantImage(Long productId, Long variantId, Long imageId);

    void clearVariantGroupImages(Long productId, Long groupId);

    void deleteVariantGroupImage(Long productId, Long groupId, Long imageId);

    void deleteVariant(Long productId, Long variantId);

    void deleteVariantGroupSize(Long productId, Long groupId, Long variantId);

    /**
     * Deletes a color variant group and all its size options and images for the product.
     */
    void deleteVariantGroup(Long productId, Long groupId);

    void deleteProduct(Long productId);

    PagedResponse<ProductListResponse> getProducts(
            int page, int size, String sort,
            String category, String sizeFilter, String color,
            BigDecimal minPrice, BigDecimal maxPrice,
            String search, Boolean featured, Boolean trending, Boolean onSale,
            Boolean newArrivalsOnly);

    ProductDetailResponse getProductBySlug(String slug);

    List<VariantResponse> getProductVariants(Long productId);

    List<ProductListResponse> getRelatedProducts(Long productId);

    /**
     * Lightweight search suggestions for use in typeahead/autocomplete.
     */
    List<ProductListResponse> suggestProducts(String query, int limit);

    /**
     * Related products lookup using the product slug instead of ID.
     */
    List<ProductListResponse> getRelatedProductsBySlug(String slug);

    PagedResponse<ProductListResponse> getProductsByCategory(String categorySlug, int page, int size);

    List<ProductListResponse> getTrendingProducts(int limit, int days);
}
