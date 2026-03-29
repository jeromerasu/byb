package com.workoutplanner.repository;

import com.workoutplanner.model.ExerciseCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ExerciseCatalog}.
 * <p>
 * Scope rules enforced at service layer:
 * - System entries (is_system=true) are readable by any authenticated user.
 * - Custom entries (is_system=false) are readable only by their owner.
 */
@Repository
public interface ExerciseCatalogRepository extends JpaRepository<ExerciseCatalog, Long> {

    // --- Scope-aware list queries ----------------------------------------

    /**
     * Returns all system entries plus entries owned by the given user.
     */
    @Query("SELECT e FROM ExerciseCatalog e WHERE e.isSystem = true OR e.createdByUserId = :userId")
    List<ExerciseCatalog> findVisibleToUser(@Param("userId") String userId);

    /**
     * Returns visible entries filtered by exercise type.
     */
    @Query("SELECT e FROM ExerciseCatalog e " +
           "WHERE (e.isSystem = true OR e.createdByUserId = :userId) " +
           "AND LOWER(e.exerciseType) = LOWER(:exerciseType)")
    List<ExerciseCatalog> findVisibleToUserByType(
            @Param("userId") String userId,
            @Param("exerciseType") String exerciseType);

    /**
     * Returns visible entries whose name contains the given substring (case-insensitive).
     */
    @Query("SELECT e FROM ExerciseCatalog e " +
           "WHERE (e.isSystem = true OR e.createdByUserId = :userId) " +
           "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<ExerciseCatalog> findVisibleToUserByNameContaining(
            @Param("userId") String userId,
            @Param("name") String name);

    /**
     * Returns visible entries that list a given muscle group (substring match on stored text).
     */
    @Query("SELECT e FROM ExerciseCatalog e " +
           "WHERE (e.isSystem = true OR e.createdByUserId = :userId) " +
           "AND LOWER(e.muscleGroups) LIKE LOWER(CONCAT('%', :muscleGroup, '%'))")
    List<ExerciseCatalog> findVisibleToUserByMuscleGroup(
            @Param("userId") String userId,
            @Param("muscleGroup") String muscleGroup);

    /**
     * Returns visible entries that list a given equipment type (substring match on stored text).
     */
    @Query("SELECT e FROM ExerciseCatalog e " +
           "WHERE (e.isSystem = true OR e.createdByUserId = :userId) " +
           "AND LOWER(e.equipmentRequired) LIKE LOWER(CONCAT('%', :equipment, '%'))")
    List<ExerciseCatalog> findVisibleToUserByEquipment(
            @Param("userId") String userId,
            @Param("equipment") String equipment);

    // --- Ownership / uniqueness helpers ---------------------------------

    Optional<ExerciseCatalog> findByNameAndCreatedByUserId(String name, String createdByUserId);

    Optional<ExerciseCatalog> findByNameAndIsSystemTrue(String name);

    List<ExerciseCatalog> findByCreatedByUserId(String createdByUserId);

    List<ExerciseCatalog> findByIsSystemTrue();
}
