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
    @Query(value = """
        SELECT * FROM food_catalog
        WHERE (is_system = true OR CAST(created_by_user_id AS TEXT) = :userId)
          AND (CAST(:name AS TEXT) IS NULL OR LOWER(name) LIKE LOWER(('%' || :name || '%')))
          AND (CAST(:category AS TEXT) IS NULL OR category = :category)
        ORDER BY is_system DESC, name ASC
        """, nativeQuery = true)
    List<FoodCatalog> findVisibleToUser(
            @Param("userId") String userId,
            @Param("name") String name,
            @Param("category") String category);

    /** All system entries only, optionally filtered. */
    @Query(value = """
        SELECT * FROM food_catalog
        WHERE is_system = true
          AND (CAST(:name AS TEXT) IS NULL OR LOWER(name) LIKE LOWER(('%' || :name || '%')))
          AND (CAST(:category AS TEXT) IS NULL OR category = :category)
        ORDER BY name ASC
        """, nativeQuery = true)
    List<FoodCatalog> findSystemEntries(
            @Param("name") String name,
            @Param("category") String category);

    List<FoodCatalog> findByCreatedByUserId(String userId);

    List<FoodCatalog> findByCategory(String category);

    List<FoodCatalog> findByIsSystem(boolean isSystem);

    Optional<FoodCatalog> findByNameAndCreatedByUserId(String name, String createdByUserId);

    Optional<FoodCatalog> findByNameAndIsSystemTrue(String name);
}
