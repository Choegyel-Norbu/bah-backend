package com.attirehub.product.repository;

import com.attirehub.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"category", "variantGroups", "variantGroups.images", "variantGroups.variants"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "variantGroups", "variantGroups.images", "variantGroups.variants"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategoryAndVariants(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.category.slug = :categorySlug AND p.isActive = true")
    Page<Product> findByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id <> :productId AND p.isActive = true")
    List<Product> findRelatedProducts(@Param("categoryId") Long categoryId,
                                      @Param("productId") Long productId,
                                      Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.isActive = true")
    List<Product> findByIdInAndIsActiveTrue(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.isTrending = true AND p.isActive = true ORDER BY p.updatedAt DESC")
    List<Product> findTrendingProducts(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY p.name ASC
            """)
    List<Product> searchSuggestions(@Param("query") String query, Pageable pageable);

    boolean existsBySlug(String slug);
}
