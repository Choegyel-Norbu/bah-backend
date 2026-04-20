package com.attirehub.product.service;

import com.attirehub.config.cache.CacheNames;
import com.attirehub.product.dto.CategoryResponse;
import com.attirehub.product.entity.Category;
import com.attirehub.product.mapper.CategoryMapper;
import com.attirehub.product.repository.CategoryRepository;
import com.attirehub.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Cacheable(value = CacheNames.CATEGORIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Category> rootCategories = categoryRepository.findRootCategories();
        return categoryMapper.toResponseList(rootCategories);
    }

    @Override
    @Cacheable(value = CacheNames.CATEGORIES, key = "#slug")
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toResponse(category);
    }
}
