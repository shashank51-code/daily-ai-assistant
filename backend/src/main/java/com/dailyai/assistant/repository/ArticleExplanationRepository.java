package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.ArticleExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleExplanationRepository extends JpaRepository<ArticleExplanation, Long> {

    Optional<ArticleExplanation> findByArticleId(Long articleId);

    boolean existsByArticleId(Long articleId);
}
