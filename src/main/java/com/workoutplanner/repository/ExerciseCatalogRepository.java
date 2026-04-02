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
     * Returns all system entries.
     */
    @Query(value = "SELECT * FROM exercise_catalog WHERE is_system = true",
           nativeQuery = true)
    List<ExerciseCatalog> findVisibleToUser();

    /**
     * Returns system entries filtered by exercise type.
     */
    @Query(value = "SELECT * FROM exercise_catalog " +
           "WHERE is_system = true " +
           "AND LOWER(exercise_type) = LOWER(:exerciseType)",
           nativeQuery = true)
    List<ExerciseCatalog> findVisibleToUserByType(
            @Param("exerciseType") String exerciseType);

    /**
     * Returns system entries whose name contains the given substring (case-insensitive).
     */
    @Query(value = "SELECT * FROM exercise_catalog " +
           "WHERE is_system = true " +
           "AND LOWER(name) LIKE LOWER(('%' || :name || '%'))",
           nativeQuery = true)
    List<ExerciseCatalog> findVisibleToUserByNameContaining(
            @Param("name") String name);

    /**
     * Returns system entries that list a given muscle group (substring match on stored text).
     */
    @Query(value = "SELECT * FROM exercise_catalog " +
           "WHERE is_system = true " +
           "AND LOWER(CAST(muscle_groups AS TEXT)) LIKE LOWER(('%' || :muscleGroup || '%'))",
           nativeQuery = true)
    List<ExerciseCatalog> findVisibleToUserByMuscleGroup(
            @Param("muscleGroup") String muscleGroup);

    /**
     * Returns system entries that list a given equipment type (substring match on stored text).
     */
    @Query(value = "SELECT * FROM exercise_catalog " +
           "WHERE is_system = true " +
           "AND LOWER(CAST(equipment_required AS TEXT)) LIKE LOWER(('%' || :equipment || '%'))",
           nativeQuery = true)
    List<ExerciseCatalog> findVisibleToUserByEquipment(
            @Param("equipment") String equipment);

    // --- Ownership / uniqueness helpers ---------------------------------

    Optional<ExerciseCatalog> findByNameAndCreatedByUserId(String name, String createdByUserId);

    Optional<ExerciseCatalog> findByNameAndIsSystemTrue(String name);

    List<ExerciseCatalog> findByCreatedByUserId(String createdByUserId);

    List<ExerciseCatalog> findByIsSystemTrue();

    // --- Media migration helpers ----------------------------------------

    /**
     * Returns system exercises whose video_url still points to ExerciseDB (not yet migrated to MinIO).
     */
    @Query("SELECT e FROM ExerciseCatalog e WHERE e.isSystem = true AND e.videoUrl LIKE '%exercisedb%'")
    List<ExerciseCatalog> findSystemExercisesWithExerciseDbUrls();

    /**
     * Returns system exercises that have no video_url — candidates for sourcing from ExerciseDB API.
     */
    List<ExerciseCatalog> findByVideoUrlIsNullAndIsSystemTrue();

    /**
     * Returns system exercises that have no instructions — candidates for API enrichment.
     */
    List<ExerciseCatalog> findByIsSystemTrueAndInstructionsIsNull();
}
