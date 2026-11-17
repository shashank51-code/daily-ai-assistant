package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.BookmarkDto;
import com.dailyai.assistant.dto.BookmarkRequest;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.Bookmark;
import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.exception.ResourceNotFoundException;
import com.dailyai.assistant.repository.AIResourceRepository;
import com.dailyai.assistant.repository.ArticleRepository;
import com.dailyai.assistant.repository.BookmarkRepository;
import com.dailyai.assistant.repository.LearningTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private AIResourceRepository resourceRepository;
    @Mock
    private LearningTopicRepository topicRepository;
    @Mock
    private CurrentUserService currentUserService;

    private BookmarkService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new BookmarkService(bookmarkRepository, articleRepository, resourceRepository,
                topicRepository, currentUserService);
        user = new User("Fresher", "fresher@dailyai.local");
        user.setId(1L);
    }

    @Test
    void addingABookmarkReturnsTheArticleSummary() {
        Article article = new Article();
        article.setId(7L);
        article.setTitle("AI news");
        article.setUrl("https://example.com/7");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(articleRepository.existsById(7L)).thenReturn(true);
        when(bookmarkRepository.findByUserIdAndItemTypeAndItemId(1L, Bookmark.ItemType.ARTICLE, 7L))
                .thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(invocation -> {
            Bookmark bookmark = invocation.getArgument(0);
            bookmark.setId(11L);
            return bookmark;
        });
        when(articleRepository.findById(7L)).thenReturn(Optional.of(article));

        BookmarkDto dto = service.add(new BookmarkRequest(Bookmark.ItemType.ARTICLE, 7L));

        assertThat(dto.id()).isEqualTo(11L);
        assertThat(dto.title()).isEqualTo("AI news");
        assertThat(dto.itemType()).isEqualTo("ARTICLE");
    }

    @Test
    void addingTheSameBookmarkTwiceDoesNotCreateSecondRow() {
        Bookmark existing = new Bookmark();
        existing.setId(11L);
        existing.setUser(user);
        existing.setItemType(Bookmark.ItemType.ARTICLE);
        existing.setItemId(7L);

        Article article = new Article();
        article.setId(7L);
        article.setTitle("AI news");

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(articleRepository.existsById(7L)).thenReturn(true);
        when(bookmarkRepository.findByUserIdAndItemTypeAndItemId(1L, Bookmark.ItemType.ARTICLE, 7L))
                .thenReturn(Optional.of(existing));
        when(articleRepository.findById(7L)).thenReturn(Optional.of(article));

        service.add(new BookmarkRequest(Bookmark.ItemType.ARTICLE, 7L));

        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    void bookmarkingAMissingItemFails() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(articleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.add(new BookmarkRequest(Bookmark.ItemType.ARTICLE, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
