package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {

    List<DailyTask> findAllByOrderByDayNumberAscOrderIndexAsc();

    List<DailyTask> findByDayNumberOrderByOrderIndexAsc(Integer dayNumber);

    Optional<DailyTask> findByDayNumberAndOrderIndex(Integer dayNumber, Integer orderIndex);

    List<DailyTask> findByDayNumberGreaterThanEqualOrderByDayNumberAscOrderIndexAsc(Integer dayNumber);
}
