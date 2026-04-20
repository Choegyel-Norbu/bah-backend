package com.attirehub.review.mapper;

import com.attirehub.review.dto.ReviewResponse;
import com.attirehub.review.entity.Review;
import com.attirehub.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "productId", source = "variant.product.id")
    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userDisplayName", expression = "java(toUserDisplayName(review.getUser()))")
    @Mapping(target = "verifiedPurchase", constant = "true")
    ReviewResponse toResponse(Review review);

    default String toUserDisplayName(User user) {
        if (user == null) return null;
        String first = user.getFirstName();
        String last = user.getLastName();
        if (first != null && !first.isBlank() || last != null && !last.isBlank()) {
            return ((first != null ? first.trim() : "") + " " + (last != null ? last.trim() : "")).trim();
        }
        return user.getEmail();
    }
}
