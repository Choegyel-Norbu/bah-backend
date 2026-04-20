package com.attirehub.product.repository;

import com.attirehub.product.entity.VariantGroupImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantGroupImageRepository extends JpaRepository<VariantGroupImage, Long> {

    List<VariantGroupImage> findByGroupIdOrderBySortOrderAscIdAsc(Long groupId);

    void deleteByGroupId(Long groupId);
}

