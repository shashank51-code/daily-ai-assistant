package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.LearningTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningTopicRepository extends JpaRepository<LearningTopic, Long> {

    List<LearningTopic> findAllByOrderByLevelAscOrderIndexAsc();

    List<LearningTopic> findByLevelOrderByOrderIndexAsc(Integer level);

    Optional<LearningTopic> findBySlug(String slug);

    long countByLevel(Integer level);
}
