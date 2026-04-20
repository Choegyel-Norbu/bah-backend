package com.attirehub.product.service;

import com.attirehub.product.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryBySlug(String slug);
}
