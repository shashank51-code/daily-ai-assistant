package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.AiUsageDaily;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AiUsageDailyRepository extends JpaRepository<AiUsageDaily, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usage from AiUsageDaily usage where usage.usageDate = :usageDate")
    Optional<AiUsageDaily> findByUsageDateForUpdate(@Param("usageDate") LocalDate usageDate);
}
