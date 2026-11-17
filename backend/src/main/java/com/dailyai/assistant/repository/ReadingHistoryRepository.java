package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.ReadingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {

    Page<ReadingHistory> findByUserIdOrderByReadAtDesc(Long userId, Pageable pageable);

    List<ReadingHistory> findTop20ByUserIdOrderByReadAtDesc(Long userId);

    Optional<ReadingHistory> findByUserIdAndArticleId(Long userId, Long articleId);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
