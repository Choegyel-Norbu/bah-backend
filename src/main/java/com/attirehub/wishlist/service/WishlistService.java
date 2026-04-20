package com.attirehub.wishlist.service;

import com.attirehub.wishlist.dto.WishlistResponse;

import java.util.List;

public interface WishlistService {

    List<WishlistResponse> getWishlist(Long userId);

    WishlistResponse addToWishlist(Long userId, Long productId);

    void removeFromWishlist(Long userId, Long productId);
}
