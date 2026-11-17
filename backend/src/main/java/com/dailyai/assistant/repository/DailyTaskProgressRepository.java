package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.DailyTaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DailyTaskProgressRepository extends JpaRepository<DailyTaskProgress, Long> {

    List<DailyTaskProgress> findByUserId(Long userId);

    List<DailyTaskProgress> findByUserIdAndCompletedAtAfter(Long userId, Instant after);

    Optional<DailyTaskProgress> findByUserIdAndTaskId(Long userId, Long taskId);

    boolean existsByUserIdAndTaskId(Long userId, Long taskId);

    long countByUserId(Long userId);

    void deleteByUserIdAndTaskId(Long userId, Long taskId);
}
