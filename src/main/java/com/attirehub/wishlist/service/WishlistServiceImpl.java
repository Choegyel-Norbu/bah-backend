package com.attirehub.wishlist.service;

import com.attirehub.product.dto.ProductDetailResponse;
import com.attirehub.product.dto.VariantGroupResponse;
import com.attirehub.product.entity.Product;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.entity.ProductVariantGroup;
import com.attirehub.product.mapper.ProductMapper;
import com.attirehub.product.repository.ProductRepository;
import com.attirehub.shared.exception.DuplicateResourceException;
import com.attirehub.shared.exception.ResourceNotFoundException;
import com.attirehub.user.entity.User;
import com.attirehub.user.repository.UserRepository;
import com.attirehub.wishlist.dto.WishlistResponse;
import com.attirehub.wishlist.entity.Wishlist;
import com.attirehub.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private static final Logger log = LoggerFactory.getLogger(WishlistServiceImpl.class);

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("Wishlist item", "productId", productId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        Wishlist saved = wishlistRepository.save(wishlist);
        log.info("Product added to wishlist: userId={}, productId={}", userId, productId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item", "productId", productId));
        wishlistRepository.delete(wishlist);
        log.info("Product removed from wishlist: userId={}, productId={}", userId, productId);
    }

    private WishlistResponse mapToResponse(Wishlist wishlist) {
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .product(toProductDetailResponse(wishlist.getProduct()))
                .addedAt(wishlist.getCreatedAt())
                .build();
    }

    private ProductDetailResponse toProductDetailResponse(Product product) {
        ProductDetailResponse detail = productMapper.toDetailResponse(product);

        BigDecimal maxDiscount = BigDecimal.ZERO;
        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                if (!variant.isActive()) continue;
                BigDecimal discount = variant.getDiscount() != null ? variant.getDiscount() : BigDecimal.ZERO;
                if (discount.compareTo(maxDiscount) > 0) {
                    maxDiscount = discount;
                }
            }
        }
        detail.setDiscount(maxDiscount);

        if (product.getVariantGroups() == null || product.getVariantGroups().isEmpty()) {
            detail.setVariantGroups(List.of());
            return detail;
        }

        List<ProductVariantGroup> groupEntities = new ArrayList<>(product.getVariantGroups());
        List<VariantGroupResponse> groups = productMapper.toVariantGroupResponseList(groupEntities);
        for (int i = 0; i < groups.size(); i++) {
            VariantGroupResponse groupDto = groups.get(i);
            ProductVariantGroup groupEntity = groupEntities.get(i);
            groupDto.setSizeOptions(productMapper.toVariantResponseList(
                    groupEntity.getVariants() != null ? new ArrayList<>(groupEntity.getVariants()) : List.of()));
        }
        detail.setVariantGroups(groups);

        return detail;
    }
}
