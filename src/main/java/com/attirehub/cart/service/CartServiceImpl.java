package com.attirehub.cart.service;

import com.attirehub.cart.dto.*;
import com.attirehub.cart.entity.Cart;
import com.attirehub.cart.entity.CartItem;
import com.attirehub.cart.repository.CartItemRepository;
import com.attirehub.cart.repository.CartRepository;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.repository.ProductVariantRepository;
import com.attirehub.shared.exception.BadRequestException;
import com.attirehub.shared.exception.ResourceNotFoundException;
import com.attirehub.user.entity.User;
import com.attirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);

        if (cart == null) {
            return CartResponse.builder()
                    .items(List.of())
                    .subtotal(BigDecimal.ZERO)
                    .totalItems(0)
                    .build();
        }

        return mapCartToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", "id", request.getVariantId()));

        if (!variant.isActive()) {
            throw new BadRequestException("This product variant is no longer available");
        }

        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + variant.getStockQuantity());
        }

        // Get or create cart
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });

        // Check if variant already in cart
        var existingItem = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            if (newQuantity > variant.getStockQuantity()) {
                throw new BadRequestException("Cannot add more. Stock available: " + variant.getStockQuantity());
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        log.info("Item added to cart: userId={}, variantId={}", userId, request.getVariantId());

        // Reload cart with EntityGraph
        Cart updatedCart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        return mapCartToResponse(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        if (request.getQuantity() > item.getVariant().getStockQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: "
                    + item.getVariant().getStockQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        log.info("Cart item updated: userId={}, itemId={}, quantity={}", userId, itemId, request.getQuantity());

        Cart updatedCart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        return mapCartToResponse(updatedCart);
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        cart.getItems().remove(item);
        cartRepository.save(cart);
        log.info("Cart item removed: userId={}, itemId={}", userId, itemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
            log.info("Cart cleared: userId={}", userId);
        }
    }

    private CartResponse mapCartToResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapCartItemToResponse)
                .toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .totalItems(totalItems)
                .build();
    }

    private CartItemResponse mapCartItemToResponse(CartItem item) {
        ProductVariant variant = item.getVariant();
        String imageUrl = null;

        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            imageUrl = variant.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.isPrimary()))
                    .findFirst()
                    .orElse(variant.getImages().get(0))
                    .getImageUrl();
        } else if (variant.getGroup() != null
                && variant.getGroup().getImages() != null
                && !variant.getGroup().getImages().isEmpty()) {
            imageUrl = variant.getGroup().getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.isPrimary()))
                    .findFirst()
                    .orElse(variant.getGroup().getImages().iterator().next())
                    .getImageUrl();
        }

        return CartItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .unitPrice(variant.getPrice())
                .discount(variant.getDiscount())
                .quantity(item.getQuantity())
                .totalPrice(variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .imageUrl(imageUrl)
                .availableStock(variant.getStockQuantity())
                .build();
    }
}
