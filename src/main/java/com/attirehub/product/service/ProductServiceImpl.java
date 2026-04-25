package com.attirehub.product.service;

import com.attirehub.config.cache.CacheNames;
import com.attirehub.filestorage.service.FileStorageService;
import com.attirehub.product.dto.CreateProductRequest;
import com.attirehub.product.dto.CreateProductVariantGroupRequest;
import com.attirehub.product.dto.CreateProductVariantRequest;
import com.attirehub.product.dto.CreateVariantSizeOptionRequest;
import com.attirehub.product.dto.ProductDetailResponse;
import com.attirehub.product.dto.UpdateProductRequest;
import com.attirehub.product.dto.UpdateProductVariantRequest;
import com.attirehub.product.dto.ProductListResponse;
import com.attirehub.product.dto.VariantGroupResponse;
import com.attirehub.product.dto.VariantResponse;
import com.attirehub.product.entity.Category;
import com.attirehub.product.entity.Product;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.entity.ProductVariantGroup;
import com.attirehub.product.entity.Size;
import com.attirehub.product.entity.VariantGroupImage;
import com.attirehub.product.mapper.ProductMapper;
import com.attirehub.product.repository.CategoryRepository;
import com.attirehub.product.repository.ProductRepository;
import com.attirehub.product.repository.ProductSpecification;
import com.attirehub.product.repository.SizeRepository;
import com.attirehub.product.repository.VariantGroupImageRepository;
import com.attirehub.product.repository.VariantImageRepository;
import com.attirehub.product.repository.ProductVariantRepository;
import com.attirehub.product.entity.VariantImage;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.exception.BadRequestException;
import com.attirehub.shared.exception.DuplicateResourceException;
import com.attirehub.shared.enums.ProductStatus;
import com.attirehub.shared.enums.SourcingType;
import com.attirehub.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VariantImageRepository variantImageRepository;
    private final VariantGroupImageRepository variantGroupImageRepository;
    private final SizeRepository sizeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request, MultiValueMap<String, MultipartFile> images) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        String slug = resolveSlug(request.getName(), request.getSlug());
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product", "slug", slug);
        }

        Set<String> skuReserved = new HashSet<>();
        Product product = Product.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .category(category)
                .brand(request.getBrand() != null ? request.getBrand().trim() : null)
                .material(request.getMaterial() != null ? request.getMaterial().trim() : null)
                .sourcingType(request.getSourcingType() != null ? request.getSourcingType() : SourcingType.OWNED)
                .productStatus(request.isActive() ? ProductStatus.ACTIVE : ProductStatus.DRAFT)
                .isActive(request.isActive())
                .isFeatured(request.isFeatured())
                .isNewArrival(request.isNewArrival())
                .isTrending(request.isTrending())
                .build();

        if (request.getVariantGroups() != null && !request.getVariantGroups().isEmpty()) {
            applyVariantGroupsToProduct(product, request.getVariantGroups(), slug, skuReserved);
        } else {
            for (CreateProductVariantRequest vReq : request.getVariants()) {
                String size = vReq.getSize() != null ? vReq.getSize().trim() : "";
                String color = vReq.getColor() != null ? vReq.getColor().trim() : "";
                String sku = resolveVariantSku(slug, size, color, vReq.getSku(), skuReserved);

                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .sku(sku)
                        .size(size)
                        .color(color)
                        .price(vReq.getPrice())
                        .discount(vReq.getDiscount() != null ? vReq.getDiscount() : BigDecimal.ZERO)
                        .stockQuantity(vReq.getStockQuantity())
                        .isActive(vReq.isActive())
                        .build();
                product.getVariants().add(variant);
            }
        }

        try {
            Product saved = productRepository.save(product);
            if (request.getVariantGroups() != null && !request.getVariantGroups().isEmpty()) {
                processVariantGroupImageUploads(
                        resolveGroupsInRequestOrder(saved, request.getVariantGroups()),
                        saved,
                        normalizeVariantGroupImages(images)
                );
            } else {
                processVariantGroupImageUploads(saved, normalizeVariantGroupImages(images));
            }
            evictAllEntries(CacheNames.PRODUCT_TRENDING);
            return toDetailWithVariantGroups(saved);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            if (msg != null && (msg.contains("uk_product_variants_sku") || msg.contains("Duplicate entry") && msg.contains("sku"))) {
                throw new DuplicateResourceException("A variant with this SKU already exists. Use unique SKUs per variant.");
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long id, UpdateProductRequest request, MultiValueMap<String, MultipartFile> images) {
        Product product = productRepository.findByIdWithCategoryAndVariants(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String oldSlug = product.getSlug();

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
        }
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = request.getSlug().trim().toLowerCase();
            productRepository.findBySlug(newSlug).ifPresent(existing -> {
                if (!existing.getId().equals(product.getId())) {
                    throw new DuplicateResourceException("Product", "slug", newSlug);
                }
            });
            product.setSlug(newSlug);
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand().trim().isEmpty() ? null : request.getBrand().trim());
        }
        if (request.getMaterial() != null) {
            product.setMaterial(request.getMaterial().trim().isEmpty() ? null : request.getMaterial().trim());
        }
        if (request.getSourcingType() != null) {
            product.setSourcingType(request.getSourcingType());
        }
        if (request.getIsActive() != null) {
            product.setProductStatus(request.getIsActive() ? ProductStatus.ACTIVE : ProductStatus.ARCHIVED);
            product.setActive(request.getIsActive());
        }
        if (request.getIsFeatured() != null) {
            product.setFeatured(request.getIsFeatured());
        }
        if (request.getIsNewArrival() != null) {
            product.setNewArrival(request.getIsNewArrival());
        }
        if (request.getIsTrending() != null) {
            product.setTrending(request.getIsTrending());
        }

        List<ProductVariantGroup> groupsInRequestOrder = null;
        if (request.getVariantGroups() != null && !request.getVariantGroups().isEmpty()) {
            groupsInRequestOrder = upsertVariantGroupsOnUpdate(product, request.getVariantGroups(), product.getSlug());
        }

        productRepository.save(product);

        Map<Integer, List<MultipartFile>> groupImages = normalizeVariantGroupImages(images);
        if (groupsInRequestOrder != null) {
            processVariantGroupImageUploads(groupsInRequestOrder, product, groupImages);
        } else {
            processVariantGroupImageUploads(product, groupImages);
        }

        Product refreshed = productRepository.findBySlug(product.getSlug())
                .orElse(product);

        evictProductCaches(id, oldSlug);
        if (!oldSlug.equals(refreshed.getSlug())) {
            evictCache(CacheNames.PRODUCT_DETAIL, refreshed.getSlug());
        }

        return toDetailWithVariantGroups(refreshed);
    }

    @Override
    @Transactional
    public VariantResponse createVariant(Long productId, CreateProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        String slug = product.getSlug();
        String size = request.getSize() != null ? request.getSize().trim() : "";
        String color = request.getColor() != null ? request.getColor().trim() : "";
        String sku = resolveVariantSku(slug, size, color, request.getSku(), new HashSet<>());
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .size(size)
                .color(color)
                .price(request.getPrice())
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .stockQuantity(request.getStockQuantity())
                .isActive(request.isActive())
                .build();
        product.getVariants().add(variant);
        try {
            productRepository.save(product);
            evictCache(CacheNames.PRODUCT_VARIANTS, productId);
            evictCache(CacheNames.PRODUCT_DETAIL, product.getSlug());
            return productMapper.toVariantResponse(variant);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            if (msg != null && (msg.contains("uk_product_variants_sku") || msg.contains("Duplicate entry") && msg.contains("sku"))) {
                throw new DuplicateResourceException("A variant with this SKU already exists. Use unique SKUs per variant.");
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public VariantResponse updateVariant(Long productId, Long variantId, UpdateProductVariantRequest request) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        if (request.getSku() != null && !request.getSku().isBlank()) {
            String sku = request.getSku().trim();
            if (productVariantRepository.existsBySkuAndIdNot(sku, variant.getId())) {
                throw new DuplicateResourceException("ProductVariant", "sku", sku);
            }
            variant.setSku(sku);
        }
        if (request.getSize() != null && !request.getSize().isBlank()) {
            variant.setSize(request.getSize().trim());
        }
        if (request.getColor() != null && !request.getColor().isBlank()) {
            variant.setColor(request.getColor().trim());
        }
        if (request.getPrice() != null) {
            variant.setPrice(request.getPrice());
        }
        if (request.getDiscount() != null) {
            variant.setDiscount(request.getDiscount());
        }
        if (request.getStockQuantity() != null) {
            variant.setStockQuantity(request.getStockQuantity());
        }
        if (request.getIsActive() != null) {
            variant.setActive(request.getIsActive());
        }

        productRepository.save(product);
        evictCache(CacheNames.PRODUCT_VARIANTS, productId);
        evictCache(CacheNames.PRODUCT_DETAIL, product.getSlug());
        return productMapper.toVariantResponse(variant);
    }

    @Override
    @Transactional
    public void clearVariantImage(Long productId, Long variantId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        List<VariantImage> images = variantImageRepository.findByVariantIdOrderBySortOrderAscIdAsc(variant.getId());
        images.forEach(img -> fileStorageService.deleteFileByUrl(img.getImageUrl()));
        variantImageRepository.deleteAll(images);
        evictCache(CacheNames.PRODUCT_VARIANTS, productId);
        evictCache(CacheNames.PRODUCT_DETAIL, product.getSlug());
        log.debug("Cleared image for variant {} of product {}", variantId, productId);
    }

    @Override
    @Transactional
    public void deleteVariantImage(Long productId, Long variantId, Long imageId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        List<VariantImage> images = variantImageRepository.findByVariantIdOrderBySortOrderAscIdAsc(variant.getId());
        VariantImage toDelete = images.stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("VariantImage", "id", imageId));

        fileStorageService.deleteFileByUrl(toDelete.getImageUrl());
        variantImageRepository.delete(toDelete);

        // Re-sequence and ensure a single primary image if any remain.
        List<VariantImage> remaining = variantImageRepository.findByVariantIdOrderBySortOrderAscIdAsc(variant.getId());
        for (int i = 0; i < remaining.size(); i++) {
            VariantImage img = remaining.get(i);
            img.setSortOrder(i);
            img.setPrimary(i == 0);
        }
        variantImageRepository.saveAll(remaining);

        evictCache(CacheNames.PRODUCT_VARIANTS, productId);
        evictCache(CacheNames.PRODUCT_DETAIL, product.getSlug());
        log.debug("Deleted variant image {} from variant {} of product {}", imageId, variantId, productId);
    }

    @Override
    @Transactional
    public void clearVariantGroupImages(Long productId, Long groupId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariantGroup group = (product.getVariantGroups() == null ? List.<ProductVariantGroup>of() : product.getVariantGroups())
                .stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariantGroup", "id", groupId));

        List<VariantGroupImage> images = variantGroupImageRepository.findByGroupIdOrderBySortOrderAscIdAsc(group.getId());
        images.forEach(img -> fileStorageService.deleteFileByUrl(img.getImageUrl()));
        variantGroupImageRepository.deleteAll(images);

        evictProductCaches(productId, product.getSlug());
        log.debug("Cleared {} images for variant group {} of product {}", images.size(), groupId, productId);
    }

    @Override
    @Transactional
    public void deleteVariantGroupImage(Long productId, Long groupId, Long imageId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariantGroup group = (product.getVariantGroups() == null ? List.<ProductVariantGroup>of() : product.getVariantGroups())
                .stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariantGroup", "id", groupId));

        List<VariantGroupImage> images = variantGroupImageRepository.findByGroupIdOrderBySortOrderAscIdAsc(group.getId());
        VariantGroupImage toDelete = images.stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("VariantGroupImage", "id", imageId));

        fileStorageService.deleteFileByUrl(toDelete.getImageUrl());
        // Keep the managed aggregate consistent: the group owns images (orphanRemoval=true),
        // so we must remove the child from the collection as well.
        if (group.getImages() != null) {
            group.getImages().removeIf(img -> img.getId().equals(imageId));
        }
        toDelete.setGroup(null);
        variantGroupImageRepository.delete(toDelete);
        variantGroupImageRepository.flush();

        List<VariantGroupImage> remaining = images.stream()
                .filter(img -> !img.getId().equals(imageId))
                .toList();
        for (int i = 0; i < remaining.size(); i++) {
            VariantGroupImage img = remaining.get(i);
            img.setSortOrder(i);
            img.setPrimary(i == 0);
        }
        variantGroupImageRepository.saveAll(remaining);

        evictProductCaches(productId, product.getSlug());
        log.debug("Deleted variant group image {} from group {} of product {}", imageId, groupId, productId);
    }

    @Override
    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        product.getVariants().remove(variant);
        try {
            productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Cannot delete size option: it is referenced by orders, cart items, or reviews.");
        }
        evictCache(CacheNames.PRODUCT_VARIANTS, productId);
        evictCache(CacheNames.PRODUCT_DETAIL, product.getSlug());
        log.debug("Deleted variant {} of product {} (and UploadThing image if present)", variantId, productId);
    }

    @Override
    @Transactional
    public void deleteVariantGroupSize(Long productId, Long groupId, Long variantId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariantGroup group = (product.getVariantGroups() == null ? List.<ProductVariantGroup>of() : new ArrayList<>(product.getVariantGroups()))
                .stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariantGroup", "id", groupId));

        ProductVariant variant = (group.getVariants() == null ? List.<ProductVariant>of() : new ArrayList<>(group.getVariants()))
                .stream()
                .filter(v -> v.getId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", variantId));

        group.getVariants().remove(variant);
        product.getVariants().remove(variant);
        try {
            productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Cannot delete size option: it is referenced by orders, cart items, or reviews.");
        }

        evictCache(CacheNames.PRODUCT_VARIANTS, productId);
        evictCache(CacheNames.PRODUCT_DETAIL, product.getSlug());
        log.debug("Deleted size option {} from group {} of product {}", variantId, groupId, productId);
    }

    @Override
    @Transactional
    public void deleteVariantGroup(Long productId, Long groupId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariantGroup group = (product.getVariantGroups() == null ? List.<ProductVariantGroup>of() : new ArrayList<>(product.getVariantGroups()))
                .stream()
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariantGroup", "id", groupId));

        List<VariantGroupImage> groupImages = variantGroupImageRepository.findByGroupIdOrderBySortOrderAscIdAsc(group.getId());
        groupImages.forEach(img -> fileStorageService.deleteFileByUrl(img.getImageUrl()));
        variantGroupImageRepository.deleteAll(groupImages);

        List<ProductVariant> variantsToRemove = new ArrayList<>(
                group.getVariants() == null ? List.of() : group.getVariants());

        for (ProductVariant variant : variantsToRemove) {
            List<VariantImage> variantImages = variantImageRepository.findByVariantIdOrderBySortOrderAscIdAsc(variant.getId());
            variantImages.forEach(img -> fileStorageService.deleteFileByUrl(img.getImageUrl()));
            variantImageRepository.deleteAll(variantImages);
            group.getVariants().remove(variant);
            product.getVariants().remove(variant);
        }

        product.getVariantGroups().remove(group);

        try {
            productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException(
                    "Cannot delete color variant: one or more size options are referenced by orders, cart items, or reviews.");
        }

        evictProductCaches(productId, product.getSlug());
        log.debug("Deleted variant group {} from product {}", groupId, productId);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findByIdWithCategoryAndVariants(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        try {
            productRepository.delete(product);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException(
                    "Cannot delete product: it is referenced by orders, cart items, or wishlists. Remove those references first.");
        }
        evictProductCaches(productId, product.getSlug());
        log.debug("Deleted product {} and its variants (UploadThing images removed)", productId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductListResponse> getProducts(
            int page, int size, String sort,
            String category, String sizeFilter, String color,
            BigDecimal minPrice, BigDecimal maxPrice,
            String search, Boolean featured, Boolean trending, Boolean onSale,
            Boolean newArrivalsOnly) {

        Specification<Product> spec = Specification.where(ProductSpecification.isActive());

        if (category != null && !category.isBlank()) {
            Category cat = categoryRepository.findBySlug(category.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", category));
            List<Long> categoryIds = new ArrayList<>(List.of(cat.getId()));
            categoryIds.addAll(
                    categoryRepository.findByParent_Id(cat.getId()).stream()
                            .map(Category::getId)
                            .toList());
            spec = spec.and(ProductSpecification.hasCategoryIn(categoryIds));
        }
        if (sizeFilter != null && !sizeFilter.isBlank()) {
            spec = spec.and(ProductSpecification.hasSize(sizeFilter));
        }
        if (color != null && !color.isBlank()) {
            spec = spec.and(ProductSpecification.hasColor(color));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecification.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecification.priceLessThanOrEqual(maxPrice));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(ProductSpecification.searchByName(search));
        }
        if (Boolean.TRUE.equals(featured)) {
            spec = spec.and(ProductSpecification.isFeatured());
        }
        if (Boolean.TRUE.equals(trending)) {
            spec = spec.and(ProductSpecification.isTrending());
        }
        if (Boolean.TRUE.equals(onSale)) {
            spec = spec.and(ProductSpecification.onSale());
        }
        if (Boolean.TRUE.equals(newArrivalsOnly)) {
            spec = spec.and(ProductSpecification.isNewArrival());
        }

        Pageable pageable = buildPageable(page, size, sort);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        List<Product> products = new ArrayList<>(productPage.getContent());
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0];
            boolean ascending = parts.length <= 1 || !"desc".equalsIgnoreCase(parts[1]);

            if ("basePrice".equals(field) || "price".equals(field)) {
                products.sort((p1, p2) -> {
                    BigDecimal b1 = computeBasePriceOfFirstVariant(p1);
                    BigDecimal b2 = computeBasePriceOfFirstVariant(p2);
                    if (b1 == null && b2 == null) return 0;
                    if (b1 == null) return 1;
                    if (b2 == null) return -1;
                    return b1.compareTo(b2);
                });
                if (!ascending) {
                    java.util.Collections.reverse(products);
                }
            }
        }

        return buildPagedResponse(productPage, products);
    }

    @Override
    @Cacheable(value = CacheNames.PRODUCT_DETAIL, key = "'v2-' + #slug")
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return toDetailWithVariantGroups(product);
    }

    private ProductDetailResponse toDetailWithVariantGroups(Product product) {
        ProductDetailResponse dto = productMapper.toDetailResponse(product);

        // Always include a discount field in the response.
        // With the Redis ObjectMapper configured as NON_NULL, setting it to 0 ensures the field is serialized.
        BigDecimal maxDiscount = BigDecimal.ZERO;
        if (product.getVariants() != null) {
            for (ProductVariant v : product.getVariants()) {
                if (!v.isActive()) continue;
                BigDecimal d = v.getDiscount() != null ? v.getDiscount() : BigDecimal.ZERO;
                if (d.compareTo(maxDiscount) > 0) {
                    maxDiscount = d;
                }
            }
        }
        dto.setDiscount(maxDiscount);

        if (product.getVariantGroups() == null || product.getVariantGroups().isEmpty()) {
            dto.setVariantGroups(List.of());
            return dto;
        }
        List<ProductVariantGroup> groupEntities = new ArrayList<>(product.getVariantGroups());
        List<VariantGroupResponse> groups = productMapper.toVariantGroupResponseList(groupEntities);
        for (int i = 0; i < groups.size(); i++) {
            VariantGroupResponse gDto = groups.get(i);
            ProductVariantGroup gEntity = groupEntities.get(i);
            gDto.setSizeOptions(productMapper.toVariantResponseList(
                    gEntity.getVariants() != null ? new ArrayList<>(gEntity.getVariants()) : List.of()));
        }
        dto.setVariantGroups(groups);
        return dto;
    }

    private List<ProductVariantGroup> resolveGroupsInRequestOrder(Product product, List<CreateProductVariantGroupRequest> requested) {
        Map<String, ProductVariantGroup> byColor = (product.getVariantGroups() == null ? List.<ProductVariantGroup>of() : new ArrayList<>(product.getVariantGroups()))
                .stream()
                .collect(Collectors.toMap(g -> g.getColor().toLowerCase().trim(), g -> g, (a, b) -> a));

        List<ProductVariantGroup> ordered = new ArrayList<>();
        for (CreateProductVariantGroupRequest gReq : requested) {
            if (gReq.getColor() == null) continue;
            ProductVariantGroup g = byColor.get(gReq.getColor().toLowerCase().trim());
            if (g != null) ordered.add(g);
        }
        return ordered;
    }

    @Override
    @Cacheable(value = CacheNames.PRODUCT_VARIANTS, key = "#productId")
    @Transactional(readOnly = true)
    public List<VariantResponse> getProductVariants(Long productId) {
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(productId);
        return productMapper.toVariantResponseList(variants);
    }

    @Override
    @Cacheable(value = CacheNames.PRODUCT_RELATED, key = "#productId")
    @Transactional(readOnly = true)
    public List<ProductListResponse> getRelatedProducts(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (product.getCategory() == null) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, 4);
        List<Product> relatedProducts = productRepository.findRelatedProducts(
                product.getCategory().getId(), productId, pageable);
        return toListResponseWithVariantGroups(relatedProducts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductListResponse> suggestProducts(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        Pageable pageable = PageRequest.of(0, safeLimit);
        List<Product> products = productRepository.searchSuggestions(query.trim(), pageable);
        return toListResponseWithVariantGroups(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductListResponse> getRelatedProductsBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        if (product.getCategory() == null) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, 4);
        List<Product> relatedProducts = productRepository.findRelatedProducts(
                product.getCategory().getId(), product.getId(), pageable);
        return toListResponseWithVariantGroups(relatedProducts);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductListResponse> getProductsByCategory(String categorySlug, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findByCategorySlug(categorySlug, pageable);
        return buildPagedResponse(productPage, productPage.getContent());
    }

    @Override
    @Cacheable(value = CacheNames.PRODUCT_TRENDING, key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductListResponse> getTrendingProducts(int limit, int days) {
        List<Product> products = productRepository.findTrendingProducts(PageRequest.of(0, limit));
        return toListResponseWithVariantGroups(products);
    }

    /**
     * Uploads images grouped by variant-group index: images[0] -> group 0, images[1] -> group 1, etc.
     * Each index can contain multiple files; every non-empty file becomes a VariantGroupImage entry on that group.
     */
    private void processVariantGroupImageUploads(Product product, Map<Integer, List<MultipartFile>> imagesByGroupIndex) {
        if (imagesByGroupIndex == null || imagesByGroupIndex.isEmpty()) {
            return;
        }

        List<ProductVariantGroup> groups = new ArrayList<>(product.getVariantGroups());
        if (groups == null || groups.isEmpty()) {
            int totalFiles = imagesByGroupIndex.values().stream().mapToInt(List::size).sum();
            log.warn("Product {} has no variant groups; ignoring {} uploaded images", product.getId(), totalFiles);
            return;
        }

        imagesByGroupIndex.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> {
                    int groupIndex = entry.getKey();
                    if (groupIndex < 0 || groupIndex >= groups.size()) {
                        log.warn("Ignoring images for groupIndex {} (groups size = {}) on product {}",
                                groupIndex, groups.size(), product.getId());
                        return;
                    }
                    ProductVariantGroup group = groups.get(groupIndex);
                    List<MultipartFile> files = entry.getValue();
                    if (files == null || files.isEmpty()) return;

                    for (MultipartFile file : files) {
                        if (file == null || file.isEmpty()) continue;
                        String storageKey = "products/" + product.getId() + "/variant-groups/" + group.getId();
                        String url = fileStorageService.storeFile(file, storageKey);

                        VariantGroupImage image = VariantGroupImage.builder()
                                .group(group)
                                .imageUrl(url)
                                .primary(group.getImages().isEmpty())
                                .sortOrder(group.getImages().size())
                                .build();
                        group.getImages().add(image);
                    }
                });

        productRepository.save(product);
        int totalUploaded = imagesByGroupIndex.values().stream()
                .flatMap(List::stream)
                .mapToInt(f -> (f != null && !f.isEmpty()) ? 1 : 0)
                .sum();
        log.debug("Uploaded {} variant-group images for product {}", totalUploaded, product.getId());
    }

    private void processVariantGroupImageUploads(
            List<ProductVariantGroup> groupsInRequestOrder,
            Product product,
            Map<Integer, List<MultipartFile>> imagesByGroupIndex
    ) {
        if (imagesByGroupIndex == null || imagesByGroupIndex.isEmpty()) return;
        if (groupsInRequestOrder == null || groupsInRequestOrder.isEmpty()) {
            processVariantGroupImageUploads(product, imagesByGroupIndex);
            return;
        }

        imagesByGroupIndex.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> {
                    int groupIndex = entry.getKey();
                    if (groupIndex < 0 || groupIndex >= groupsInRequestOrder.size()) {
                        log.warn("Ignoring images for groupIndex {} (request groups size = {}) on product {}",
                                groupIndex, groupsInRequestOrder.size(), product.getId());
                        return;
                    }
                    ProductVariantGroup group = groupsInRequestOrder.get(groupIndex);
                    List<MultipartFile> files = entry.getValue();
                    if (files == null || files.isEmpty()) return;

                    for (MultipartFile file : files) {
                        if (file == null || file.isEmpty()) continue;
                        String storageKey = "products/" + product.getId() + "/variant-groups/" + group.getId();
                        String url = fileStorageService.storeFile(file, storageKey);

                        VariantGroupImage image = VariantGroupImage.builder()
                                .group(group)
                                .imageUrl(url)
                                .primary(group.getImages().isEmpty())
                                .sortOrder(group.getImages().size())
                                .build();
                        group.getImages().add(image);
                    }
                });

        productRepository.save(product);
    }

    private List<ProductVariantGroup> upsertVariantGroupsOnUpdate(
            Product product,
            List<CreateProductVariantGroupRequest> requested,
            String productSlug
    ) {
        Map<String, ProductVariantGroup> existingByColor = (product.getVariantGroups() == null ? List.<ProductVariantGroup>of() : new ArrayList<>(product.getVariantGroups()))
                .stream()
                .collect(Collectors.toMap(g -> g.getColor().toLowerCase().trim(), g -> g, (a, b) -> a));

        Set<String> skuReserved = new HashSet<>();
        List<ProductVariantGroup> ordered = new ArrayList<>();

        for (CreateProductVariantGroupRequest gReq : requested) {
            String color = gReq.getColor() != null ? gReq.getColor().trim() : "";
            if (color.isBlank()) continue;
            String key = color.toLowerCase();

            ProductVariantGroup group = existingByColor.get(key);
            if (group == null) {
                group = ProductVariantGroup.builder()
                        .product(product)
                        .color(color)
                        .isActive(gReq.isActive())
                        .build();
                product.getVariantGroups().add(group);
                existingByColor.put(key, group);
            } else {
                group.setActive(gReq.isActive());
            }

            // Upsert sizes under this color.
            List<ProductVariant> existingVariants = group.getVariants() == null ? List.of() : new ArrayList<>(group.getVariants());
            Map<Long, ProductVariant> variantsById = existingVariants.stream()
                    .filter(v -> v.getId() != null)
                    .collect(Collectors.toMap(ProductVariant::getId, v -> v, (a, b) -> a));
            Map<String, ProductVariant> variantsBySize = existingVariants.stream()
                    .filter(v -> v.getSize() != null)
                    .collect(Collectors.toMap(v -> v.getSize().toLowerCase().trim(), v -> v, (a, b) -> a));

            for (CreateVariantSizeOptionRequest sReq : (gReq.getSizes() == null ? List.<CreateVariantSizeOptionRequest>of() : gReq.getSizes())) {
                String sizeLabel = sReq.getSize() != null ? sReq.getSize().trim() : "";
                if (sizeLabel.isBlank()) continue;
                String sizeKey = sizeLabel.toLowerCase();

                ProductVariant variant = null;
                if (sReq.getId() != null) {
                    variant = variantsById.get(sReq.getId());
                    if (variant == null) {
                        throw new ResourceNotFoundException("ProductVariant", "id", sReq.getId());
                    }
                    // Prevent having two rows with same size in the same group after rename.
                    ProductVariant otherWithSameSize = variantsBySize.get(sizeKey);
                    if (otherWithSameSize != null && otherWithSameSize.getId() != null
                            && !otherWithSameSize.getId().equals(variant.getId())) {
                        throw new BadRequestException("Size '" + sizeLabel + "' already exists for color '" + color + "'");
                    }
                } else {
                    variant = variantsBySize.get(sizeKey);
                }

                Size size = sizeRepository.findByLabel(sizeLabel)
                        .orElseGet(() -> sizeRepository.save(Size.builder().label(sizeLabel).build()));

                if (variant == null) {
                    String sku = resolveVariantSku(productSlug, sizeLabel, color, sReq.getSku(), skuReserved);
                    variant = ProductVariant.builder()
                            .product(product)
                            .group(group)
                            .sizeRef(size)
                            .sku(sku)
                            .size(sizeLabel)
                            .color(color)
                            .price(sReq.getPrice())
                            .discount(sReq.getDiscount() != null ? sReq.getDiscount() : BigDecimal.ZERO)
                            .stockQuantity(sReq.getStockQuantity())
                            .isActive(sReq.isActive())
                            .build();

                    product.getVariants().add(variant);
                    group.getVariants().add(variant);
                    variantsBySize.put(sizeKey, variant);
                } else {
                    // Update existing row (including rename).
                    boolean sizeChanged = variant.getSize() == null || !variant.getSize().equals(sizeLabel);

                    // Update SKU if explicitly provided; otherwise regenerate on size change.
                    if (sReq.getSku() != null && !sReq.getSku().isBlank()) {
                        String newSku = sReq.getSku().trim();
                        if (!newSku.equals(variant.getSku()) && productVariantRepository.existsBySkuAndIdNot(newSku, variant.getId())) {
                            throw new DuplicateResourceException("ProductVariant", "sku", newSku);
                        }
                        variant.setSku(newSku);
                    } else if (sizeChanged) {
                        String regenerated = resolveVariantSku(productSlug, sizeLabel, color, null, skuReserved);
                        if (!regenerated.equals(variant.getSku()) && productVariantRepository.existsBySkuAndIdNot(regenerated, variant.getId())) {
                            throw new DuplicateResourceException("ProductVariant", "sku", regenerated);
                        }
                        variant.setSku(regenerated);
                    }

                    variant.setSize(sizeLabel);
                    variant.setSizeRef(size);
                    variant.setColor(color);
                    if (sReq.getPrice() != null) variant.setPrice(sReq.getPrice());
                    variant.setDiscount(sReq.getDiscount() != null ? sReq.getDiscount() : BigDecimal.ZERO);
                    variant.setStockQuantity(sReq.getStockQuantity());
                    variant.setActive(sReq.isActive());

                    if (sizeChanged) {
                        final Long updatedVariantId = variant.getId();
                        variantsBySize.values().removeIf(v -> v.getId() != null && v.getId().equals(updatedVariantId));
                        variantsBySize.put(sizeKey, variant);
                    }
                }
            }

            ordered.add(group);
        }

        return ordered;
    }

    private Map<Integer, List<MultipartFile>> normalizeVariantGroupImages(MultiValueMap<String, MultipartFile> parts) {
        if (parts == null || parts.isEmpty()) return Map.of();

        // Uses keys: images[0], images[1], ... (repeatable)
        Map<Integer, List<MultipartFile>> out = new HashMap<>();
        for (Map.Entry<String, List<MultipartFile>> entry : parts.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            if (!key.startsWith("images[") || !key.endsWith("]")) continue;
            String idxStr = key.substring("images[".length(), key.length() - 1);
            int idx;
            try {
                idx = Integer.parseInt(idxStr);
            } catch (NumberFormatException ignore) {
                continue;
            }
            if (idx < 0) continue;
            List<MultipartFile> files = entry.getValue();
            if (files == null || files.isEmpty()) continue;
            out.computeIfAbsent(idx, __ -> new ArrayList<>()).addAll(files);
        }
        return out;
    }

    private void applyVariantGroupsToProduct(
            Product product,
            List<CreateProductVariantGroupRequest> groups,
            String productSlug,
            Set<String> skuReserved
    ) {
        for (CreateProductVariantGroupRequest gReq : groups) {
            String color = gReq.getColor() != null ? gReq.getColor().trim() : "";
            if (color.isBlank()) throw new BadRequestException("Variant group color is required");

            ProductVariantGroup group = ProductVariantGroup.builder()
                    .product(product)
                    .color(color)
                    .isActive(gReq.isActive())
                    .build();
            product.getVariantGroups().add(group);

            if (gReq.getSizes() == null || gReq.getSizes().isEmpty()) continue;

            for (CreateVariantSizeOptionRequest sReq : gReq.getSizes()) {
                String sizeLabel = sReq.getSize() != null ? sReq.getSize().trim() : "";
                if (sizeLabel.isBlank()) throw new BadRequestException("Variant size is required");

                Size size = sizeRepository.findByLabel(sizeLabel)
                        .orElseGet(() -> sizeRepository.save(Size.builder().label(sizeLabel).build()));

                String sku = resolveVariantSku(productSlug, sizeLabel, color, sReq.getSku(), skuReserved);

                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .group(group)
                        .sizeRef(size)
                        .sku(sku)
                        .size(sizeLabel)
                        .color(color)
                        .price(sReq.getPrice())
                        .discount(sReq.getDiscount() != null ? sReq.getDiscount() : BigDecimal.ZERO)
                        .stockQuantity(sReq.getStockQuantity())
                        .isActive(sReq.isActive())
                        .build();

                product.getVariants().add(variant);
                group.getVariants().add(variant);
            }
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0];

            // Map known sort keys from the API/FE to actual entity fields.
            // basePrice/price are handled separately in-memory using variant prices.
            String mappedField = switch (field) {
                case "createdAt", "name", "averageRating", "reviewCount",
                     "isFeatured", "isTrending", "isNewArrival" -> field;
                case "basePrice", "price" -> "createdAt";
                default -> null;
            };

            if (mappedField != null) {
                Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                return PageRequest.of(page, size, Sort.by(direction, mappedField));
            } else {
                log.warn("Ignoring unsupported sort field '{}'; falling back to default sort", field);
            }
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PagedResponse<ProductListResponse> buildPagedResponse(Page<Product> productPage, List<Product> products) {
        List<ProductListResponse> content = toListResponseWithVariantGroups(products);
        return PagedResponse.<ProductListResponse>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    private BigDecimal computeBasePriceOfFirstVariant(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return null;
        }
        for (ProductVariant v : product.getVariants()) {
            if (v.isActive()) {
                BigDecimal price = v.getPrice();
                BigDecimal discount = v.getDiscount() != null ? v.getDiscount() : BigDecimal.ZERO;
                return price.subtract(discount);
            }
        }
        return null;
    }

    private List<ProductListResponse> toListResponseWithVariantGroups(List<Product> products) {
        List<ProductListResponse> content = products.stream()
                .map(productMapper::toListResponse)
                .toList();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            ProductListResponse dto = content.get(i);
            dto.setVariantGroups(toDetailWithVariantGroups(p).getVariantGroups());
            BigDecimal maxDiscount = BigDecimal.ZERO;
            if (p.getVariants() != null) {
                for (ProductVariant v : p.getVariants()) {
                    if (!v.isActive()) continue;
                    BigDecimal d = v.getDiscount() != null ? v.getDiscount() : BigDecimal.ZERO;
                    if (d.compareTo(maxDiscount) > 0) {
                        maxDiscount = d;
                    }
                }
            }
            dto.setDiscount(maxDiscount);
        }
        return content;
    }

    private String resolveSlug(String name, String slug) {
        if (slug != null && !slug.isBlank()) {
            return slug.trim().toLowerCase();
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Product name is required when slug is not provided");
        }
        return name.toLowerCase().trim().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private String resolveVariantSku(String productSlug, String size, String color, String providedSku, Set<String> skuReserved) {
        if (providedSku != null && !providedSku.isBlank()) {
            String sku = providedSku.trim();
            if (!skuReserved.add(sku)) {
                throw new BadRequestException("Duplicate variant SKU in request: " + sku);
            }
            if (productVariantRepository.existsBySku(sku)) {
                throw new DuplicateResourceException("ProductVariant", "sku", sku);
            }
            return sku;
        }
        String base = normalizeSkuPart(productSlug) + "-" + normalizeSkuPart(size) + "-" + normalizeSkuPart(color);
        if (base.length() > 100) {
            base = base.substring(0, 97);
        }
        String candidate = base;
        int attempt = 0;
        while (skuReserved.contains(candidate) || productVariantRepository.existsBySku(candidate)) {
            attempt++;
            candidate = (base.length() + 4 <= 100 ? base : base.substring(0, 96)) + "-" + attempt;
        }
        skuReserved.add(candidate);
        return candidate;
    }

    private String normalizeSkuPart(String part) {
        if (part == null || part.isBlank()) return "";
        return part.toLowerCase().trim().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    // ==================== Cache Eviction Helpers ====================

    private void evictProductCaches(Long productId, String... slugs) {
        for (String slug : slugs) {
            evictCache(CacheNames.PRODUCT_DETAIL, slug);
        }
        evictCache(CacheNames.PRODUCT_VARIANTS, productId);
        evictCache(CacheNames.PRODUCT_RELATED, productId);
        evictAllEntries(CacheNames.PRODUCT_TRENDING);
    }

    private void evictCache(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            // Also evict the v2 product-detail cache key to prevent stale responses.
            if (CacheNames.PRODUCT_DETAIL.equals(cacheName) && key instanceof String slug) {
                cache.evict("v2-" + slug);
            }
        }
    }

    private void evictAllEntries(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
