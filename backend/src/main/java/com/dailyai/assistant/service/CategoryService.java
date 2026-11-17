package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.CategoryDto;
import com.dailyai.assistant.entity.Category;
import com.dailyai.assistant.repository.ArticleRepository;
import com.dailyai.assistant.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    public CategoryService(CategoryRepository categoryRepository, ArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    private CategoryDto toDto(Category category) {
        long count = articleRepository.countByCategorySlug(category.getSlug());
        return new CategoryDto(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), count);
    }
}
