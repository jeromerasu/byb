package com.workoutplanner.service;

import com.workoutplanner.dto.FoodCatalogRequestDto;
import com.workoutplanner.dto.FoodCatalogResponseDto;
import com.workoutplanner.model.FoodCatalog;
import com.workoutplanner.repository.FoodCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-DATA-002: Unit tests for FoodCatalogService access-rule logic.
 */
@ExtendWith(MockitoExtension.class)
class FoodCatalogServiceTest {

    private static final Logger log = LoggerFactory.getLogger(FoodCatalogServiceTest.class);

    @Mock
    private FoodCatalogRepository repository;

    private FoodCatalogService service;

    private static final String USER_A = "user-a-id";
    private static final String USER_B = "user-b-id";

    @BeforeEach
    void setUp() {
        service = new FoodCatalogService(repository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FoodCatalogRequestDto makeRequest(String name) {
        FoodCatalogRequestDto req = new FoodCatalogRequestDto();
        req.setName(name);
        req.setCategory("PROTEIN");
        req.setServingSize("100g");
        req.setCalories(200);
        req.setProteinGrams(new BigDecimal("25.0"));
        req.setCarbsGrams(new BigDecimal("5.0"));
        req.setFatGrams(new BigDecimal("8.0"));
        req.setFiberGrams(new BigDecimal("1.0"));
        return req;
    }

    private FoodCatalog makeUserFood(Long id, String name, String userId) {
        FoodCatalog f = new FoodCatalog();
        f.setId(id);
        f.setName(name);
        f.setSystem(false);
        f.setCreatedByUserId(userId);
        return f;
    }

    private FoodCatalog makeSystemFood(Long id, String name) {
        FoodCatalog f = new FoodCatalog();
        f.setId(id);
        f.setName(name);
        f.setSystem(true);
        f.setCreatedByUserId(null);
        return f;
    }

    // -------------------------------------------------------------------------
    // createUserFood
    // -------------------------------------------------------------------------

    @Test
    void createUserFood_NewEntry_ShouldSetIsSystemFalseAndUserId() {
        when(repository.findByNameAndCreatedByUserId("Chicken", USER_A)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            FoodCatalog f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        FoodCatalogResponseDto result = service.createUserFood(makeRequest("Chicken"), USER_A);

        log.info("test.createUserFood.success id={} isSystem={}", result.getId(), result.isSystem());
        assertFalse(result.isSystem());
        assertEquals(USER_A, result.getCreatedByUserId());
        assertEquals("Chicken", result.getName());

        ArgumentCaptor<FoodCatalog> captor = ArgumentCaptor.forClass(FoodCatalog.class);
        verify(repository).save(captor.capture());
        assertFalse(captor.getValue().isSystem());
        assertEquals(USER_A, captor.getValue().getCreatedByUserId());
    }

    @Test
    void createUserFood_DuplicateName_ShouldThrow409() {
        FoodCatalog existing = makeUserFood(1L, "Chicken", USER_A);
        when(repository.findByNameAndCreatedByUserId("Chicken", USER_A)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createUserFood(makeRequest("Chicken"), USER_A));

        log.info("test.createUserFood.duplicate status={}", ex.getStatusCode());
        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // createSystemFood
    // -------------------------------------------------------------------------

    @Test
    void createSystemFood_NewEntry_ShouldSetIsSystemTrueAndNullUserId() {
        when(repository.findByNameAndIsSystemTrue("Brown Rice")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            FoodCatalog f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });

        FoodCatalogResponseDto result = service.createSystemFood(makeRequest("Brown Rice"));

        log.info("test.createSystemFood.success id={} isSystem={}", result.getId(), result.isSystem());
        assertTrue(result.isSystem());
        assertNull(result.getCreatedByUserId());
    }

    @Test
    void createSystemFood_DuplicateSystemName_ShouldThrow409() {
        FoodCatalog existing = makeSystemFood(5L, "Brown Rice");
        when(repository.findByNameAndIsSystemTrue("Brown Rice")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createSystemFood(makeRequest("Brown Rice")));

        log.info("test.createSystemFood.duplicate status={}", ex.getStatusCode());
        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateUserFood — ownership checks
    // -------------------------------------------------------------------------

    @Test
    void updateUserFood_OwnEntry_ShouldSucceed() {
        FoodCatalog food = makeUserFood(1L, "Chicken", USER_A);
        when(repository.findById(1L)).thenReturn(Optional.of(food));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodCatalogResponseDto result = service.updateUserFood(1L, makeRequest("Chicken Breast"), USER_A);

        log.info("test.updateUserFood.own name={}", result.getName());
        assertEquals("Chicken Breast", result.getName());
        verify(repository).save(food);
    }

    @Test
    void updateUserFood_OtherUserEntry_ShouldThrow403() {
        FoodCatalog food = makeUserFood(2L, "Salmon", USER_B);
        when(repository.findById(2L)).thenReturn(Optional.of(food));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateUserFood(2L, makeRequest("Salmon"), USER_A));

        log.info("test.updateUserFood.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void updateUserFood_SystemEntry_ShouldThrow403() {
        FoodCatalog food = makeSystemFood(3L, "Oats");
        when(repository.findById(3L)).thenReturn(Optional.of(food));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateUserFood(3L, makeRequest("Oats"), USER_A));

        log.info("test.updateUserFood.system status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void updateUserFood_NotFound_ShouldThrow404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateUserFood(99L, makeRequest("X"), USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // updateSystemFood — admin path
    // -------------------------------------------------------------------------

    @Test
    void updateSystemFood_ValidSystemEntry_ShouldSucceed() {
        FoodCatalog food = makeSystemFood(5L, "Quinoa");
        when(repository.findById(5L)).thenReturn(Optional.of(food));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodCatalogResponseDto result = service.updateSystemFood(5L, makeRequest("Quinoa Updated"));

        log.info("test.updateSystemFood.success name={}", result.getName());
        assertEquals("Quinoa Updated", result.getName());
        assertTrue(result.isSystem());
    }

    @Test
    void updateSystemFood_UserEntry_ShouldThrow400() {
        FoodCatalog food = makeUserFood(6L, "Tofu", USER_A);
        when(repository.findById(6L)).thenReturn(Optional.of(food));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateSystemFood(6L, makeRequest("Tofu")));

        log.info("test.updateSystemFood.notSystem status={}", ex.getStatusCode());
        assertEquals(400, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // listVisibleToUser — scoping
    // -------------------------------------------------------------------------

    @Test
    void listVisibleToUser_ShouldReturnSystemAndOwnEntries() {
        FoodCatalog systemFood = makeSystemFood(1L, "Brown Rice");
        FoodCatalog userFood = makeUserFood(2L, "My Protein Bar", USER_A);
        when(repository.findVisibleToUser(USER_A, null, null))
                .thenReturn(List.of(systemFood, userFood));

        List<FoodCatalogResponseDto> result = service.listVisibleToUser(USER_A, null, null);

        log.info("test.listVisibleToUser count={}", result.size());
        assertEquals(2, result.size());
    }

    @Test
    void listVisibleToUser_WithCategoryFilter_ShouldDelegate() {
        when(repository.findVisibleToUser(USER_A, null, "PROTEIN")).thenReturn(List.of());

        service.listVisibleToUser(USER_A, null, "PROTEIN");

        verify(repository).findVisibleToUser(USER_A, null, "PROTEIN");
    }

    @Test
    void listVisibleToUser_WithNameFilter_ShouldDelegate() {
        when(repository.findVisibleToUser(USER_A, "rice", null)).thenReturn(List.of());

        service.listVisibleToUser(USER_A, "rice", null);

        verify(repository).findVisibleToUser(USER_A, "rice", null);
    }

    // -------------------------------------------------------------------------
    // getById — access control
    // -------------------------------------------------------------------------

    @Test
    void getById_SystemEntry_ShouldBeAccessibleByAnyUser() {
        FoodCatalog food = makeSystemFood(1L, "Oats");
        when(repository.findById(1L)).thenReturn(Optional.of(food));

        FoodCatalogResponseDto result = service.getById(1L, USER_A);

        log.info("test.getById.system id={}", result.getId());
        assertNotNull(result);
        assertTrue(result.isSystem());
    }

    @Test
    void getById_OwnCustomEntry_ShouldBeAccessible() {
        FoodCatalog food = makeUserFood(2L, "My Bar", USER_A);
        when(repository.findById(2L)).thenReturn(Optional.of(food));

        FoodCatalogResponseDto result = service.getById(2L, USER_A);

        assertEquals(USER_A, result.getCreatedByUserId());
    }

    @Test
    void getById_OtherUserEntry_ShouldThrow403() {
        FoodCatalog food = makeUserFood(3L, "Other Bar", USER_B);
        when(repository.findById(3L)).thenReturn(Optional.of(food));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(3L, USER_A));

        log.info("test.getById.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getById_NotFound_ShouldThrow404() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(999L, USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }
}
