package com.attirehub.wishlist.controller;

import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.user.entity.User;
import com.attirehub.wishlist.dto.WishlistAddRequest;
import com.attirehub.wishlist.dto.WishlistResponse;
import com.attirehub.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist(
            @AuthenticationPrincipal User currentUser) {
        List<WishlistResponse> wishlist = wishlistService.getWishlist(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody WishlistAddRequest request) {
        WishlistResponse wishlist = wishlistService.addToWishlist(currentUser.getId(), request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", wishlist));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlistByPath(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId) {
        WishlistResponse wishlist = wishlistService.addToWishlist(currentUser.getId(), productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", wishlist));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId) {
        wishlistService.removeFromWishlist(currentUser.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist"));
    }
}
