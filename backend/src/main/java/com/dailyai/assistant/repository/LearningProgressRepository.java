package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {

    Optional<LearningProgress> findByUserIdAndTopicId(Long userId, Long topicId);

    List<LearningProgress> findByUserId(Long userId);

    List<LearningProgress> findByUserIdAndStatus(Long userId, LearningProgress.Status status);

    List<LearningProgress> findByUserIdAndCompletedAtIsNotNullAndCompletedAtAfter(Long userId, Instant after);

    long countByUserIdAndStatus(Long userId, LearningProgress.Status status);

    long countByUserIdAndTopicLevelAndStatus(Long userId, Integer level, LearningProgress.Status status);
}
