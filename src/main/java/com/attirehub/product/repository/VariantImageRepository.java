package com.attirehub.product.repository;

import com.attirehub.product.entity.VariantImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantImageRepository extends JpaRepository<VariantImage, Long> {

    List<VariantImage> findByVariantIdOrderBySortOrderAscIdAsc(Long variantId);

    void deleteByVariantId(Long variantId);
}

