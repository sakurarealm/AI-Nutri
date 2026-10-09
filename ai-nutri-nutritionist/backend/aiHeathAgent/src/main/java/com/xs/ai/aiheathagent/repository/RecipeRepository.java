package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findTopByTitleIgnoreCase(String title);

    @Query("""
            select r from Recipe r
            where (:isActive is null or r.isActive = :isActive)
              and (:q = '' or lower(r.title) like lower(concat('%', :q, '%')))
            order by r.sortOrder desc, r.id asc
            """)
    Page<Recipe> pageAdmin(@Param("q") String q, @Param("isActive") Integer isActive, Pageable pageable);

    @Query("""
            select r from Recipe r
            where r.isActive = 1
            order by r.sortOrder desc, r.id asc
            """)
    List<Recipe> listActive(Pageable pageable);

    @Query("""
            select r from Recipe r
            where r.isActive = 1 and lower(r.organKeys) like lower(concat('%', :organKey, '%'))
            order by r.sortOrder desc, r.id asc
            """)
    List<Recipe> listByOrganKey(@Param("organKey") String organKey, Pageable pageable);
}
