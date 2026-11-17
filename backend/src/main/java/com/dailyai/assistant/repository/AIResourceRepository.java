package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.AIResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AIResourceRepository extends JpaRepository<AIResource, Long> {

    Page<AIResource> findByCategoryIgnoreCase(String category, Pageable pageable);

    List<AIResource> findByTrendingTrueOrderByNameAsc();

    List<AIResource> findAllByOrderByNameAsc();

    @Query("""
            select r from AIResource r
            where lower(r.name) like lower(concat('%', :q, '%'))
               or lower(coalesce(r.description, '')) like lower(concat('%', :q, '%'))
               or lower(r.category) like lower(concat('%', :q, '%'))
            """)
    Page<AIResource> search(@Param("q") String query, Pageable pageable);
}
