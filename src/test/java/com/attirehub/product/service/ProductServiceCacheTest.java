package com.attirehub.product.service;

import com.attirehub.config.cache.CacheNames;
import com.attirehub.filestorage.service.FileStorageService;
import com.attirehub.product.dto.ProductDetailResponse;
import com.attirehub.product.dto.VariantResponse;
import com.attirehub.product.entity.Product;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.mapper.ProductMapper;
import com.attirehub.product.repository.CategoryRepository;
import com.attirehub.product.repository.ProductRepository;
import com.attirehub.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({ProductServiceCacheTest.CacheTestConfig.class, ProductServiceImpl.class})
class ProductServiceCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheNames.PRODUCT_DETAIL,
                    CacheNames.PRODUCT_VARIANTS,
                    CacheNames.PRODUCT_RELATED,
                    CacheNames.PRODUCT_TRENDING
            );
        }

        @Bean
        ProductRepository productRepository() {
            return Mockito.mock(ProductRepository.class);
        }

        @Bean
        ProductVariantRepository productVariantRepository() {
            return Mockito.mock(ProductVariantRepository.class);
        }

        @Bean
        CategoryRepository categoryRepository() {
            return Mockito.mock(CategoryRepository.class);
        }

        @Bean
        ProductMapper productMapper() {
            return Mockito.mock(ProductMapper.class);
        }

        @Bean
        FileStorageService fileStorageService() {
            return Mockito.mock(FileStorageService.class);
        }
    }

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private ProductMapper productMapper;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        CacheNames.all().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        Mockito.reset(productRepository, productVariantRepository, productMapper);
    }

    @Test
    void getProductBySlug_shouldReturnCachedResultOnSecondCall() {
        Product product = buildProduct(1L, "test-product");
        ProductDetailResponse response = ProductDetailResponse.builder()
                .id(1L).slug("test-product").name("Test Product").build();

        when(productRepository.findBySlug("test-product")).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(response);

        ProductDetailResponse first = productService.getProductBySlug("test-product");
        ProductDetailResponse second = productService.getProductBySlug("test-product");

        assertThat(first).isEqualTo(second);
        assertThat(first.getSlug()).isEqualTo("test-product");
        verify(productRepository, times(1)).findBySlug("test-product");
    }

    @Test
    void getProductBySlug_differentSlugs_shouldCacheSeparately() {
        Product p1 = buildProduct(1L, "shirt");
        Product p2 = buildProduct(2L, "pants");
        ProductDetailResponse r1 = ProductDetailResponse.builder().id(1L).slug("shirt").build();
        ProductDetailResponse r2 = ProductDetailResponse.builder().id(2L).slug("pants").build();

        when(productRepository.findBySlug("shirt")).thenReturn(Optional.of(p1));
        when(productMapper.toDetailResponse(p1)).thenReturn(r1);
        when(productRepository.findBySlug("pants")).thenReturn(Optional.of(p2));
        when(productMapper.toDetailResponse(p2)).thenReturn(r2);

        productService.getProductBySlug("shirt");
        productService.getProductBySlug("pants");
        productService.getProductBySlug("shirt");
        productService.getProductBySlug("pants");

        verify(productRepository, times(1)).findBySlug("shirt");
        verify(productRepository, times(1)).findBySlug("pants");
    }

    @Test
    void getProductVariants_shouldReturnCachedResultOnSecondCall() {
        ProductVariant variant = ProductVariant.builder()
                .sku("SKU-1").size("M").color("Black")
                .price(BigDecimal.valueOf(29.99)).stockQuantity(50)
                .build();
        variant.setId(10L);

        VariantResponse variantResponse = VariantResponse.builder()
                .id(10L).sku("SKU-1").size("M").color("Black").build();

        when(productVariantRepository.findByProductIdAndIsActiveTrue(1L))
                .thenReturn(List.of(variant));
        when(productMapper.toVariantResponseList(List.of(variant)))
                .thenReturn(List.of(variantResponse));

        List<VariantResponse> first = productService.getProductVariants(1L);
        List<VariantResponse> second = productService.getProductVariants(1L);

        assertThat(first).isEqualTo(second);
        verify(productVariantRepository, times(1)).findByProductIdAndIsActiveTrue(1L);
    }

    @Test
    void deleteProduct_shouldEvictProductCaches() {
        Product product = buildProduct(1L, "to-delete");
        ProductDetailResponse response = ProductDetailResponse.builder()
                .id(1L).slug("to-delete").build();

        when(productRepository.findBySlug("to-delete")).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(response);

        productService.getProductBySlug("to-delete");
        verify(productRepository, times(1)).findBySlug("to-delete");

        when(productRepository.findByIdWithCategoryAndVariants(1L)).thenReturn(Optional.of(product));
        productService.deleteProduct(1L);

        productService.getProductBySlug("to-delete");
        verify(productRepository, times(2)).findBySlug("to-delete");
    }

    private Product buildProduct(Long id, String slug) {
        Product product = Product.builder()
                .name(slug.replace("-", " "))
                .slug(slug)
                .build();
        product.setId(id);
        product.setVariants(new ArrayList<>());
        return product;
    }
}
