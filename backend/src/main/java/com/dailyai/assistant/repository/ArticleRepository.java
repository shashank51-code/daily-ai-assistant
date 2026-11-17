package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByUrl(String url);

    boolean existsByUrl(String url);

    Page<Article> findByCategorySlug(String slug, Pageable pageable);

    Page<Article> findByPublishedAtAfter(Instant from, Pageable pageable);

    List<Article> findByCategorySlugInOrderByPublishedAtDesc(List<String> slugs, Pageable pageable);

    List<Article> findByPublishedAtIsNotNullOrderByPublishedAtDesc(Pageable pageable);

    long countByPublishedAtAfter(Instant from);

    long countByFetchedAtAfter(Instant from);

    long countByCategorySlug(String slug);

    @Query("""
            select a from Article a
            where lower(a.title) like lower(concat('%', :q, '%'))
               or lower(coalesce(a.description, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(a.source, '')) like lower(concat('%', :q, '%'))
            """)
    Page<Article> search(@Param("q") String query, Pageable pageable);

    @Query("""
            select a from Article a
            where (lower(a.title) like lower(concat('%', :q, '%'))
               or lower(coalesce(a.description, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(a.source, '')) like lower(concat('%', :q, '%')))
              and a.category.slug = :category
            """)
    Page<Article> searchInCategory(@Param("q") String query, @Param("category") String category, Pageable pageable);
}
