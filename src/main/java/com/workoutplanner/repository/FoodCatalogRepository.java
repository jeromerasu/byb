package com.workoutplanner.repository;

import com.workoutplanner.model.FoodCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodCatalogRepository extends JpaRepository<FoodCatalog, Long> {

    /** All system entries + the given user's custom entries, optionally filtered by name and/or category. */
    @Query("""
        SELECT f FROM FoodCatalog f
        WHERE (f.isSystem = true OR f.createdByUserId = :userId)
          AND (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:category IS NULL OR f.category = :category)
        ORDER BY f.isSystem DESC, f.name ASC
        """)
    List<FoodCatalog> findVisibleToUser(
            @Param("userId") String userId,
            @Param("name") String name,
            @Param("category") String category);

    /** All system entries only, optionally filtered. */
    @Query("""
        SELECT f FROM FoodCatalog f
        WHERE f.isSystem = true
          AND (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:category IS NULL OR f.category = :category)
        ORDER BY f.name ASC
        """)
    List<FoodCatalog> findSystemEntries(
            @Param("name") String name,
            @Param("category") String category);

    List<FoodCatalog> findByCreatedByUserId(String userId);

    List<FoodCatalog> findByCategory(String category);

    List<FoodCatalog> findByIsSystem(boolean isSystem);

    Optional<FoodCatalog> findByNameAndCreatedByUserId(String name, String createdByUserId);

    Optional<FoodCatalog> findByNameAndIsSystemTrue(String name);
}
