package com.attirehub.cart.service;

import com.attirehub.cart.dto.*;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request);

    void removeCartItem(Long userId, Long itemId);

    void clearCart(Long userId);
}
