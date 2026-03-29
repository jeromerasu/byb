package com.workoutplanner.service;

import com.workoutplanner.dto.FoodCatalogRequestDto;
import com.workoutplanner.dto.FoodCatalogResponseDto;
import com.workoutplanner.model.FoodCatalog;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.FoodCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodCatalogService {

    private static final Logger log = LoggerFactory.getLogger(FoodCatalogService.class);

    private final FoodCatalogRepository repository;

    @Autowired
    public FoodCatalogService(FoodCatalogRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // User-facing: create a custom (non-system) food entry
    // -------------------------------------------------------------------------

    @Transactional
    public FoodCatalogResponseDto createUserFood(FoodCatalogRequestDto request, String userId) {
        log.info("food_catalog.create.user userId={} name={}", userId, request.getName());

        checkDuplicateUserEntry(request.getName(), userId);

        FoodCatalog food = buildFromRequest(request);
        food.setSystem(false);
        food.setCreatedByUserId(userId);

        FoodCatalog saved = saveWithDuplicateGuard(food);
        log.info("food_catalog.created.user id={} userId={}", saved.getId(), userId);
        return FoodCatalogResponseDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // Admin: create a system food entry
    // -------------------------------------------------------------------------

    @Transactional
    public FoodCatalogResponseDto createSystemFood(FoodCatalogRequestDto request) {
        log.info("food_catalog.create.system name={}", request.getName());

        repository.findByNameAndIsSystemTrue(request.getName()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "System food entry with name '" + request.getName() + "' already exists");
        });

        FoodCatalog food = buildFromRequest(request);
        food.setSystem(true);
        food.setCreatedByUserId(null);

        FoodCatalog saved = saveWithDuplicateGuard(food);
        log.info("food_catalog.created.system id={} name={}", saved.getId(), saved.getName());
        return FoodCatalogResponseDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // User-facing: update own custom entry
    // -------------------------------------------------------------------------

    @Transactional
    public FoodCatalogResponseDto updateUserFood(Long id, FoodCatalogRequestDto request, String userId) {
        log.info("food_catalog.update.user id={} userId={}", id, userId);

        FoodCatalog food = requireById(id);

        if (food.isSystem()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "System food entries can only be updated by admins");
        }
        if (!userId.equals(food.getCreatedByUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own food entries");
        }

        applyUpdates(food, request);
        FoodCatalog saved = repository.save(food);
        log.info("food_catalog.updated.user id={} userId={}", id, userId);
        return FoodCatalogResponseDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // Admin: update a system entry
    // -------------------------------------------------------------------------

    @Transactional
    public FoodCatalogResponseDto updateSystemFood(Long id, FoodCatalogRequestDto request) {
        log.info("food_catalog.update.system id={}", id);

        FoodCatalog food = requireById(id);

        if (!food.isSystem()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Food entry " + id + " is not a system entry");
        }

        applyUpdates(food, request);
        FoodCatalog saved = repository.save(food);
        log.info("food_catalog.updated.system id={}", id);
        return FoodCatalogResponseDto.from(saved);
    }

    // -------------------------------------------------------------------------
    // Read: list foods visible to the current user (system + own custom)
    // -------------------------------------------------------------------------

    public List<FoodCatalogResponseDto> listVisibleToUser(String userId, String name, String category) {
        log.info("food_catalog.list.user userId={} name={} category={}", userId, name, category);
        return repository.findVisibleToUser(userId, name, category)
                .stream()
                .map(FoodCatalogResponseDto::from)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Read: get a single food entry (system or owned by current user)
    // -------------------------------------------------------------------------

    public FoodCatalogResponseDto getById(Long id, String userId) {
        log.info("food_catalog.get id={} userId={}", id, userId);

        FoodCatalog food = requireById(id);

        if (!food.isSystem() && !userId.equals(food.getCreatedByUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have access to food entry " + id);
        }

        return FoodCatalogResponseDto.from(food);
    }

    // -------------------------------------------------------------------------
    // Admin: get any food entry regardless of ownership
    // -------------------------------------------------------------------------

    public FoodCatalogResponseDto getByIdAsAdmin(Long id) {
        return FoodCatalogResponseDto.from(requireById(id));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private FoodCatalog requireById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Food entry " + id + " not found"));
    }

    private void checkDuplicateUserEntry(String name, String userId) {
        repository.findByNameAndCreatedByUserId(name, userId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a custom food entry named '" + name + "'");
        });
    }

    private FoodCatalog buildFromRequest(FoodCatalogRequestDto request) {
        FoodCatalog food = new FoodCatalog();
        food.setName(request.getName());
        food.setCategory(request.getCategory());
        food.setServingSize(request.getServingSize());
        food.setCalories(request.getCalories());
        food.setProteinGrams(request.getProteinGrams());
        food.setCarbsGrams(request.getCarbsGrams());
        food.setFatGrams(request.getFatGrams());
        food.setFiberGrams(request.getFiberGrams());
        return food;
    }

    private void applyUpdates(FoodCatalog food, FoodCatalogRequestDto request) {
        food.setName(request.getName());
        food.setCategory(request.getCategory());
        food.setServingSize(request.getServingSize());
        food.setCalories(request.getCalories());
        food.setProteinGrams(request.getProteinGrams());
        food.setCarbsGrams(request.getCarbsGrams());
        food.setFatGrams(request.getFatGrams());
        food.setFiberGrams(request.getFiberGrams());
    }

    private FoodCatalog saveWithDuplicateGuard(FoodCatalog food) {
        try {
            return repository.save(food);
        } catch (DataIntegrityViolationException e) {
            log.warn("food_catalog.duplicate name={} userId={}", food.getName(), food.getCreatedByUserId());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A food entry with that name already exists for this user");
        }
    }
}
