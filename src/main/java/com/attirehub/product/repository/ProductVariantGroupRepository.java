package com.attirehub.product.repository;

import com.attirehub.product.entity.ProductVariantGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductVariantGroupRepository extends JpaRepository<ProductVariantGroup, Long> {

    Optional<ProductVariantGroup> findByProductIdAndColor(Long productId, String color);
}

