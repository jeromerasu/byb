package com.workoutplanner.service;

import com.workoutplanner.dto.ExerciseCatalogRequestDto;
import com.workoutplanner.dto.ExerciseCatalogResponseDto;
import com.workoutplanner.model.ExerciseCatalog;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for exercise catalog operations.
 * <p>
 * Access rules:
 * <ul>
 *   <li>Admin — create/update any system entry (is_system=true).</li>
 *   <li>User — create/update only their own custom entries (is_system=false).</li>
 *   <li>Read — system entries plus the current user's custom entries.</li>
 * </ul>
 */
@Service
public class ExerciseCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseCatalogService.class);

    private final ExerciseCatalogRepository repository;

    @Autowired
    public ExerciseCatalogService(ExerciseCatalogRepository repository) {
        this.repository = repository;
    }

    // ---------------------------------------------------------------
    // Read operations
    // ---------------------------------------------------------------

    /**
     * Returns all exercises visible to the requesting user:
     * system entries plus their own custom entries.
     * Supports optional filters (applied at most one at a time in priority order).
     */
    public List<ExerciseCatalogResponseDto> listForUser(
            String userId, String name, String type, String muscleGroup, String equipment) {

        log.info("catalog.list userId={} name={} type={} muscleGroup={} equipment={}",
                userId, name, type, muscleGroup, equipment);

        List<ExerciseCatalog> results;

        if (name != null && !name.isBlank()) {
            results = repository.findVisibleToUserByNameContaining(userId, name.trim());
        } else if (type != null && !type.isBlank()) {
            results = repository.findVisibleToUserByType(userId, type.trim());
        } else if (muscleGroup != null && !muscleGroup.isBlank()) {
            results = repository.findVisibleToUserByMuscleGroup(userId, muscleGroup.trim());
        } else if (equipment != null && !equipment.isBlank()) {
            results = repository.findVisibleToUserByEquipment(userId, equipment.trim());
        } else {
            results = repository.findVisibleToUser(userId);
        }

        log.info("catalog.list result count={}", results.size());
        return results.stream().map(ExerciseCatalogResponseDto::from).collect(Collectors.toList());
    }

    /**
     * Returns a single exercise if visible to the requesting user.
     *
     * @throws ResponseStatusException 404 if not found, 403 if not accessible.
     */
    public ExerciseCatalogResponseDto getForUser(Long id, String userId) {
        ExerciseCatalog entry = findOrThrow(id);
        assertReadable(entry, userId);
        log.info("catalog.get id={} userId={}", id, userId);
        return ExerciseCatalogResponseDto.from(entry);
    }

    // ---------------------------------------------------------------
    // Write operations — user path
    // ---------------------------------------------------------------

    /**
     * Creates a custom entry owned by the requesting user (is_system=false).
     *
     * @throws ResponseStatusException 409 if duplicate name for this user.
     */
    @Transactional
    public ExerciseCatalogResponseDto createForUser(ExerciseCatalogRequestDto dto, String userId) {
        log.info("catalog.create.user userId={} name={}", userId, dto.getName());

        checkDuplicateForUser(dto.getName(), userId);

        ExerciseCatalog entry = applyDto(new ExerciseCatalog(), dto);
        entry.setSystem(false);
        entry.setCreatedByUserId(userId);

        ExerciseCatalog saved = repository.save(entry);
        log.info("catalog.create.user.done id={} userId={}", saved.getId(), userId);
        return ExerciseCatalogResponseDto.from(saved);
    }

    /**
     * Updates a custom entry owned by the requesting user.
     *
     * @throws ResponseStatusException 404 if not found, 403 if not owner.
     */
    @Transactional
    public ExerciseCatalogResponseDto updateForUser(Long id, ExerciseCatalogRequestDto dto, String userId) {
        log.info("catalog.update.user id={} userId={}", id, userId);

        ExerciseCatalog entry = findOrThrow(id);
        assertOwner(entry, userId);

        if (!entry.getName().equals(dto.getName())) {
            checkDuplicateForUser(dto.getName(), userId);
        }

        applyDto(entry, dto);
        ExerciseCatalog saved = repository.save(entry);
        log.info("catalog.update.user.done id={} userId={}", saved.getId(), userId);
        return ExerciseCatalogResponseDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Write operations — admin path
    // ---------------------------------------------------------------

    /**
     * Creates a system entry (is_system=true, no owning user).
     *
     * @throws ResponseStatusException 409 if duplicate system entry name.
     */
    @Transactional
    public ExerciseCatalogResponseDto createSystemEntry(ExerciseCatalogRequestDto dto) {
        log.info("catalog.create.system name={}", dto.getName());

        checkDuplicateSystem(dto.getName());

        ExerciseCatalog entry = applyDto(new ExerciseCatalog(), dto);
        entry.setSystem(true);
        entry.setCreatedByUserId(null);

        ExerciseCatalog saved = repository.save(entry);
        log.info("catalog.create.system.done id={}", saved.getId());
        return ExerciseCatalogResponseDto.from(saved);
    }

    /**
     * Updates a system entry. Admin may also update a user's custom entry by id.
     *
     * @throws ResponseStatusException 404 if not found, 403 if entry is not a system entry.
     */
    @Transactional
    public ExerciseCatalogResponseDto updateSystemEntry(Long id, ExerciseCatalogRequestDto dto) {
        log.info("catalog.update.system id={}", id);

        ExerciseCatalog entry = findOrThrow(id);

        if (!entry.isSystem()) {
            log.warn("catalog.update.system.rejected id={} reason=not_system_entry", id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin endpoint may only update system entries");
        }

        if (!entry.getName().equals(dto.getName())) {
            checkDuplicateSystem(dto.getName());
        }

        applyDto(entry, dto);
        ExerciseCatalog saved = repository.save(entry);
        log.info("catalog.update.system.done id={}", saved.getId());
        return ExerciseCatalogResponseDto.from(saved);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private ExerciseCatalog findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.warn("catalog.notFound id={}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found: " + id);
        });
    }

    private void assertReadable(ExerciseCatalog entry, String userId) {
        if (!entry.isSystem() && !userId.equals(entry.getCreatedByUserId())) {
            log.warn("catalog.read.forbidden id={} userId={}", entry.getId(), userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied to exercise " + entry.getId());
        }
    }

    private void assertOwner(ExerciseCatalog entry, String userId) {
        if (entry.isSystem()) {
            log.warn("catalog.update.forbidden id={} userId={} reason=system_entry", entry.getId(), userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "System entries can only be modified by admins");
        }
        if (!userId.equals(entry.getCreatedByUserId())) {
            log.warn("catalog.update.forbidden id={} userId={} reason=not_owner", entry.getId(), userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own exercise " + entry.getId());
        }
    }

    private void checkDuplicateForUser(String name, String userId) {
        Optional<ExerciseCatalog> existing = repository.findByNameAndCreatedByUserId(name, userId);
        if (existing.isPresent()) {
            log.warn("catalog.duplicate userId={} name={}", userId, name);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a custom exercise named '" + name + "'");
        }
    }

    private void checkDuplicateSystem(String name) {
        Optional<ExerciseCatalog> existing = repository.findByNameAndIsSystemTrue(name);
        if (existing.isPresent()) {
            log.warn("catalog.duplicate.system name={}", name);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A system exercise named '" + name + "' already exists");
        }
    }

    private ExerciseCatalog applyDto(ExerciseCatalog entry, ExerciseCatalogRequestDto dto) {
        entry.setName(dto.getName());
        entry.setExerciseType(dto.getExerciseType());
        entry.setMuscleGroups(dto.getMuscleGroups());
        entry.setEquipmentRequired(dto.getEquipmentRequired());
        entry.setDifficultyLevel(dto.getDifficultyLevel());
        entry.setVideoUrl(dto.getVideoUrl());
        entry.setThumbnailUrl(dto.getThumbnailUrl());
        entry.setInstructions(dto.getInstructions());
        return entry;
    }
}
