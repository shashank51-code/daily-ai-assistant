package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Bookmark> findByUserIdAndItemTypeOrderByCreatedAtDesc(Long userId, Bookmark.ItemType itemType);

    Optional<Bookmark> findByUserIdAndItemTypeAndItemId(Long userId, Bookmark.ItemType itemType, Long itemId);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndItemTypeAndItemId(Long userId, Bookmark.ItemType itemType, Long itemId);

    long countByUserIdAndItemType(Long userId, Bookmark.ItemType itemType);
}
