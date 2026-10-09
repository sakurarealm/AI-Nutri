package com.xs.ai.aiheathagent.repository;

import com.xs.ai.aiheathagent.entity.FoodCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodCatalogRepository extends JpaRepository<FoodCatalog, Long> {
    long countByIsActive(Integer isActive);

    Optional<FoodCatalog> findTopByNameIgnoreCase(String name);

    @Query("""
            select f from FoodCatalog f
            where (:isActive is null or f.isActive = :isActive)
              and (:q = '' or (
                    lower(f.name) like lower(concat('%', :q, '%'))
                    or lower(f.keywords) like lower(concat('%', :q, '%'))
              ))
            order by f.sortOrder desc, f.id asc
            """)
    Page<FoodCatalog> pageAdmin(@Param("q") String q, @Param("isActive") Integer isActive, Pageable pageable);

    @Query("""
            select f from FoodCatalog f
            where f.isActive = 1 and lower(f.mealTags) like lower(concat('%', :tag, '%'))
            order by f.sortOrder desc, f.id asc
            """)
    List<FoodCatalog> recommendByMealTag(@Param("tag") String tag, Pageable pageable);

    @Query("""
            select f from FoodCatalog f
            where f.isActive = 1 and (
                lower(f.name) like lower(concat('%', :q, '%'))
                or lower(f.keywords) like lower(concat('%', :q, '%'))
            )
            order by f.sortOrder desc, f.id asc
            """)
    List<FoodCatalog> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select f from FoodCatalog f
            where f.isActive = 1
            order by f.sortOrder desc, f.id asc
            """)
    List<FoodCatalog> listActive(Pageable pageable);
}
