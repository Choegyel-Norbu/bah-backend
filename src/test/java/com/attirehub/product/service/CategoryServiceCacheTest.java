package com.attirehub.product.service;

import com.attirehub.config.cache.CacheNames;
import com.attirehub.product.dto.CategoryResponse;
import com.attirehub.product.entity.Category;
import com.attirehub.product.mapper.CategoryMapper;
import com.attirehub.product.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({CategoryServiceCacheTest.CacheTestConfig.class, CategoryServiceImpl.class})
class CategoryServiceCacheTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.CATEGORIES);
        }

        @Bean
        CategoryRepository categoryRepository() {
            return Mockito.mock(CategoryRepository.class);
        }

        @Bean
        CategoryMapper categoryMapper() {
            return Mockito.mock(CategoryMapper.class);
        }
    }

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheNames.CATEGORIES).clear();
        Mockito.reset(categoryRepository, categoryMapper);
    }

    @Test
    void getAllCategories_shouldReturnCachedResultOnSecondCall() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Men");

        CategoryResponse response = CategoryResponse.builder()
                .id(1L)
                .name("Men")
                .build();

        when(categoryRepository.findRootCategories()).thenReturn(List.of(category));
        when(categoryMapper.toResponseList(List.of(category))).thenReturn(List.of(response));

        List<CategoryResponse> first = categoryService.getAllCategories();
        List<CategoryResponse> second = categoryService.getAllCategories();

        assertThat(first).isEqualTo(second);
        assertThat(first).containsExactly(response);
        verify(categoryRepository, times(1)).findRootCategories();
    }

    @Test
    void getCategoryBySlug_shouldReturnCachedResultOnSecondCall() {
        Category category = new Category();
        category.setId(1L);
        category.setSlug("men");
        category.setName("Men");

        CategoryResponse response = CategoryResponse.builder()
                .id(1L)
                .slug("men")
                .name("Men")
                .build();

        when(categoryRepository.findBySlug("men")).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse first = categoryService.getCategoryBySlug("men");
        CategoryResponse second = categoryService.getCategoryBySlug("men");

        assertThat(first).isEqualTo(second);
        assertThat(first.getSlug()).isEqualTo("men");
        verify(categoryRepository, times(1)).findBySlug("men");
    }

    @Test
    void getCategoryBySlug_differentSlugs_shouldCacheSeparately() {
        Category menCategory = new Category();
        menCategory.setId(1L);
        menCategory.setSlug("men");

        Category womenCategory = new Category();
        womenCategory.setId(2L);
        womenCategory.setSlug("women");

        CategoryResponse menResponse = CategoryResponse.builder().id(1L).slug("men").build();
        CategoryResponse womenResponse = CategoryResponse.builder().id(2L).slug("women").build();

        when(categoryRepository.findBySlug("men")).thenReturn(Optional.of(menCategory));
        when(categoryMapper.toResponse(menCategory)).thenReturn(menResponse);
        when(categoryRepository.findBySlug("women")).thenReturn(Optional.of(womenCategory));
        when(categoryMapper.toResponse(womenCategory)).thenReturn(womenResponse);

        CategoryResponse men = categoryService.getCategoryBySlug("men");
        CategoryResponse women = categoryService.getCategoryBySlug("women");

        categoryService.getCategoryBySlug("men");
        categoryService.getCategoryBySlug("women");

        assertThat(men.getId()).isEqualTo(1L);
        assertThat(women.getId()).isEqualTo(2L);
        verify(categoryRepository, times(1)).findBySlug("men");
        verify(categoryRepository, times(1)).findBySlug("women");
    }
}
