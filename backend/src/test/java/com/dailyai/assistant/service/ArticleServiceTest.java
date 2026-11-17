package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.ArticleDto;
import com.dailyai.assistant.dto.PageResponse;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.Bookmark;
import com.dailyai.assistant.entity.Category;
import com.dailyai.assistant.exception.ResourceNotFoundException;
import com.dailyai.assistant.repository.ArticleRepository;
import com.dailyai.assistant.repository.CategoryRepository;
import com.dailyai.assistant.repository.ReadingHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BookmarkService bookmarkService;
    @Mock
    private ReadingHistoryRepository readingHistoryRepository;
    @Mock
    private ExplanationService explanationService;
    @Mock
    private CurrentUserService currentUserService;

    private ArticleService service;

    @BeforeEach
    void setUp() {
        service = new ArticleService(articleRepository, categoryRepository, bookmarkService,
                readingHistoryRepository, explanationService, currentUserService);
    }

    @Test
    void listMarksBookmarkedArticlesAndSortsNewest() {
        Article article = article(1L, "News", "generative-ai", Instant.parse("2026-11-16T00:00:00Z"));
        Page<Article> page = new PageImpl<>(List.of(article));
        when(articleRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(bookmarkService.bookmarkedIds(Bookmark.ItemType.ARTICLE)).thenReturn(Set.of(1L));

        PageResponse<ArticleDto> response = service.list("all", "newest", 0, 12);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).bookmarked()).isTrue();
        assertThat(response.totalElements()).isEqualTo(1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(articleRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("publishedAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void unknownCategoryReturnsNotFound() {
        when(categoryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list("unknown", "newest", 0, 12))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unsupportedSortValueIsRejected() {
        assertThatThrownBy(() -> service.list(null, "random", 0, 12))
                .isInstanceOf(com.dailyai.assistant.exception.BadRequestException.class);
        verify(articleRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void detailThrowsWhenArticleDoesNotExist() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    private Article article(Long id, String title, String slug, Instant publishedAt) {
        Category category = new Category("Generative AI", slug, null);
        category.setId(1L);
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setUrl("https://example.com/" + id);
        article.setPublishedAt(publishedAt);
        article.setCategory(category);
        return article;
    }
}
