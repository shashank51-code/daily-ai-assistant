package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.RssRefreshRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RssRefreshRunRepository extends JpaRepository<RssRefreshRun, Long> {

    Optional<RssRefreshRun> findFirstByStatusOrderByStartedAtDesc(String status);

    Optional<RssRefreshRun> findTopByOrderByStartedAtDesc();

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RssRefreshRun r
            set r.status = 'FAILED',
                r.finishedAt = :finishedAt,
                r.errorSummary = :errorSummary
            where r.status = 'RUNNING'
              and r.startedAt < :cutoff
            """)
    int recoverStaleRuns(@Param("cutoff") Instant cutoff,
                         @Param("finishedAt") Instant finishedAt,
                         @Param("errorSummary") String errorSummary);
}