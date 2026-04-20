package com.attirehub.product.mapper;

import com.attirehub.product.dto.ProductDetailResponse;
import com.attirehub.product.dto.ProductListResponse;
import com.attirehub.product.dto.VariantGroupImageResponse;
import com.attirehub.product.dto.VariantGroupResponse;
import com.attirehub.product.dto.VariantImageResponse;
import com.attirehub.product.dto.VariantResponse;
import com.attirehub.product.entity.Product;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.entity.ProductVariantGroup;
import com.attirehub.product.entity.VariantGroupImage;
import com.attirehub.product.entity.VariantImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "variantGroups", ignore = true)
    @Mapping(target = "newArrival", source = "newArrival")
    @Mapping(target = "isFeatured", source = "featured")
    @Mapping(target = "isTrending", source = "trending")
    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "reviewCount", source = "reviewCount")
    ProductListResponse toListResponse(Product product);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "isFeatured", source = "featured")
    @Mapping(target = "isTrending", source = "trending")
    @Mapping(target = "newArrival", source = "newArrival")
    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "reviewCount", source = "reviewCount")
    @Mapping(target = "variantGroups", ignore = true)
    ProductDetailResponse toDetailResponse(Product product);

    @Mapping(target = "isActive", source = "active")
    VariantResponse toVariantResponse(ProductVariant variant);

    List<VariantResponse> toVariantResponseList(List<ProductVariant> variants);

    VariantImageResponse toVariantImageResponse(VariantImage image);

    List<VariantImageResponse> toVariantImageResponseList(List<VariantImage> images);

    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "sizeOptions", ignore = true)
    VariantGroupResponse toVariantGroupResponse(ProductVariantGroup group);

    List<VariantGroupResponse> toVariantGroupResponseList(List<ProductVariantGroup> groups);

    VariantGroupImageResponse toVariantGroupImageResponse(VariantGroupImage image);

    List<VariantGroupImageResponse> toVariantGroupImageResponseList(List<VariantGroupImage> images);
}
