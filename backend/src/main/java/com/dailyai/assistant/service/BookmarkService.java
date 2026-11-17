package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.BookmarkDto;
import com.dailyai.assistant.dto.BookmarkRequest;
import com.dailyai.assistant.entity.AIResource;
import com.dailyai.assistant.entity.Bookmark;
import com.dailyai.assistant.entity.LearningTopic;
import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.exception.ResourceNotFoundException;
import com.dailyai.assistant.repository.AIResourceRepository;
import com.dailyai.assistant.repository.ArticleRepository;
import com.dailyai.assistant.repository.BookmarkRepository;
import com.dailyai.assistant.repository.LearningTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;
    private final AIResourceRepository resourceRepository;
    private final LearningTopicRepository topicRepository;
    private final CurrentUserService currentUserService;

    public BookmarkService(BookmarkRepository bookmarkRepository,
                           ArticleRepository articleRepository,
                           AIResourceRepository resourceRepository,
                           LearningTopicRepository topicRepository,
                           CurrentUserService currentUserService) {
        this.bookmarkRepository = bookmarkRepository;
        this.articleRepository = articleRepository;
        this.resourceRepository = resourceRepository;
        this.topicRepository = topicRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<BookmarkDto> list(Bookmark.ItemType itemType) {
        Long userId = currentUserService.getCurrentUserId();
        List<Bookmark> bookmarks = itemType == null
                ? bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : bookmarkRepository.findByUserIdAndItemTypeOrderByCreatedAtDesc(userId, itemType);
        return bookmarks.stream().map(this::toDto).toList();
    }

    @Transactional
    public BookmarkDto add(BookmarkRequest request) {
        User user = currentUserService.getCurrentUser();
        ensureItemExists(request.itemType(), request.itemId());

        Bookmark bookmark = bookmarkRepository
                .findByUserIdAndItemTypeAndItemId(user.getId(), request.itemType(), request.itemId())
                .orElseGet(() -> {
                    Bookmark created = new Bookmark();
                    created.setUser(user);
                    created.setItemType(request.itemType());
                    created.setItemId(request.itemId());
                    created.setCreatedAt(Instant.now());
                    return bookmarkRepository.save(created);
                });
        return toDto(bookmark);
    }

    @Transactional
    public void delete(Long bookmarkId) {
        Long userId = currentUserService.getCurrentUserId();
        Bookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark " + bookmarkId + " was not found"));
        bookmarkRepository.delete(bookmark);
    }

    @Transactional
    public void deleteByItem(Bookmark.ItemType itemType, Long itemId) {
        Long userId = currentUserService.getCurrentUserId();
        bookmarkRepository.findByUserIdAndItemTypeAndItemId(userId, itemType, itemId)
                .ifPresent(bookmarkRepository::delete);
    }

    @Transactional(readOnly = true)
    public Set<Long> bookmarkedIds(Bookmark.ItemType itemType) {
        Long userId = currentUserService.getCurrentUserId();
        List<Bookmark> bookmarks = bookmarkRepository.findByUserIdAndItemTypeOrderByCreatedAtDesc(userId, itemType);
        Set<Long> ids = new HashSet<>();
        bookmarks.forEach(bookmark -> ids.add(bookmark.getItemId()));
        return ids;
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Bookmark.ItemType itemType, Long itemId) {
        return bookmarkRepository.existsByUserIdAndItemTypeAndItemId(
                currentUserService.getCurrentUserId(), itemType, itemId);
    }

    @Transactional(readOnly = true)
    public long count() {
        Long userId = currentUserService.getCurrentUserId();
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
    }

    private void ensureItemExists(Bookmark.ItemType itemType, Long itemId) {
        boolean exists = switch (itemType) {
            case ARTICLE -> articleRepository.existsById(itemId);
            case TOOL -> resourceRepository.existsById(itemId);
            case TOPIC -> topicRepository.existsById(itemId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(itemType + " " + itemId + " was not found");
        }
    }

    private BookmarkDto toDto(Bookmark bookmark) {
        return switch (bookmark.getItemType()) {
            case ARTICLE -> articleRepository.findById(bookmark.getItemId())
                    .map(article -> new BookmarkDto(bookmark.getId(), "ARTICLE", article.getId(),
                            article.getTitle(), article.getSource(), article.getUrl(), bookmark.getCreatedAt()))
                    .orElseGet(() -> orphan(bookmark));
            case TOOL -> resourceRepository.findById(bookmark.getItemId())
                    .map(resource -> toolDto(resource, bookmark))
                    .orElseGet(() -> orphan(bookmark));
            case TOPIC -> topicRepository.findById(bookmark.getItemId())
                    .map(topic -> new BookmarkDto(bookmark.getId(), "TOPIC", topic.getId(),
                            topic.getTitle(), "Level " + topic.getLevel() + " - " + topic.getLevelName(),
                            null, bookmark.getCreatedAt()))
                    .orElseGet(() -> orphan(bookmark));
        };
    }

    private BookmarkDto toolDto(AIResource resource, Bookmark bookmark) {
        return new BookmarkDto(bookmark.getId(), "TOOL", resource.getId(), resource.getName(),
                resource.getCategory(), resource.getWebsite(), bookmark.getCreatedAt());
    }

    private BookmarkDto orphan(Bookmark bookmark) {
        return new BookmarkDto(bookmark.getId(), bookmark.getItemType().name(), bookmark.getItemId(),
                "Saved item", "This item is no longer available", null, bookmark.getCreatedAt());
    }
}
