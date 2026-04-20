package com.attirehub.product.repository;

import com.attirehub.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    List<ProductVariant> findByProductIdInAndIsActiveTrue(List<Long> productIds);

    boolean existsBySku(String sku);

    /** True if another variant (with different id) has this SKU. */
    boolean existsBySkuAndIdNot(String sku, Long id);
}
