package com.workoutplanner.service;

import com.workoutplanner.dto.ExerciseCatalogRequestDto;
import com.workoutplanner.dto.ExerciseCatalogResponseDto;
import com.workoutplanner.model.ExerciseCatalog;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-DATA-001: Unit tests for ExerciseCatalogService access-rule logic.
 * Covers: create (admin vs user path), update (ownership check, admin override),
 * list (filter by user scope), duplicate name conflict.
 */
@ExtendWith(MockitoExtension.class)
class ExerciseCatalogServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ExerciseCatalogServiceTest.class);

    @Mock
    private ExerciseCatalogRepository repository;

    private ExerciseCatalogService service;

    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";

    @BeforeEach
    void setUp() {
        service = new ExerciseCatalogService(repository);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ExerciseCatalog makeSystem(Long id, String name) {
        ExerciseCatalog e = new ExerciseCatalog();
        e.setId(id);
        e.setName(name);
        e.setSystem(true);
        e.setCreatedByUserId(null);
        return e;
    }

    private ExerciseCatalog makeCustom(Long id, String name, String userId) {
        ExerciseCatalog e = new ExerciseCatalog();
        e.setId(id);
        e.setName(name);
        e.setSystem(false);
        e.setCreatedByUserId(userId);
        return e;
    }

    private ExerciseCatalogRequestDto makeRequest(String name) {
        ExerciseCatalogRequestDto dto = new ExerciseCatalogRequestDto();
        dto.setName(name);
        dto.setExerciseType("STRENGTH");
        dto.setMuscleGroups(List.of("CHEST", "TRICEPS"));
        dto.setEquipmentRequired(List.of("BARBELL"));
        dto.setDifficultyLevel("INTERMEDIATE");
        return dto;
    }

    // ---------------------------------------------------------------
    // createForUser
    // ---------------------------------------------------------------

    @Test
    void createForUser_NewName_ShouldPersistCustomEntry() {
        ExerciseCatalogRequestDto dto = makeRequest("Bench Press");
        when(repository.findByNameAndCreatedByUserId("Bench Press", USER_A)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            ExerciseCatalog e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        ExerciseCatalogResponseDto result = service.createForUser(dto, USER_A);

        log.info("test.createForUser id={} isSystem={}", result.getId(), result.isSystem());
        assertFalse(result.isSystem());
        assertEquals(USER_A, result.getCreatedByUserId());
        assertEquals("Bench Press", result.getName());
        verify(repository).save(any(ExerciseCatalog.class));
    }

    @Test
    void createForUser_DuplicateName_ShouldThrow409() {
        ExerciseCatalogRequestDto dto = makeRequest("Bench Press");
        when(repository.findByNameAndCreatedByUserId("Bench Press", USER_A))
                .thenReturn(Optional.of(makeCustom(1L, "Bench Press", USER_A)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createForUser(dto, USER_A));

        log.info("test.createForUser.duplicate status={}", ex.getStatusCode());
        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void createForUser_ShouldSetIsSystemFalse() {
        ExerciseCatalogRequestDto dto = makeRequest("Pull-up");
        when(repository.findByNameAndCreatedByUserId("Pull-up", USER_A)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            ExerciseCatalog e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        ExerciseCatalogResponseDto result = service.createForUser(dto, USER_A);

        assertFalse(result.isSystem());
    }

    // ---------------------------------------------------------------
    // createSystemEntry
    // ---------------------------------------------------------------

    @Test
    void createSystemEntry_NewName_ShouldPersistSystemEntry() {
        ExerciseCatalogRequestDto dto = makeRequest("Squat");
        when(repository.findByNameAndIsSystemTrue("Squat")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            ExerciseCatalog e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        ExerciseCatalogResponseDto result = service.createSystemEntry(dto);

        log.info("test.createSystemEntry id={} isSystem={}", result.getId(), result.isSystem());
        assertTrue(result.isSystem());
        assertNull(result.getCreatedByUserId());
        verify(repository).save(any(ExerciseCatalog.class));
    }

    @Test
    void createSystemEntry_DuplicateName_ShouldThrow409() {
        ExerciseCatalogRequestDto dto = makeRequest("Squat");
        when(repository.findByNameAndIsSystemTrue("Squat"))
                .thenReturn(Optional.of(makeSystem(10L, "Squat")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createSystemEntry(dto));

        log.info("test.createSystemEntry.duplicate status={}", ex.getStatusCode());
        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // updateForUser — ownership check
    // ---------------------------------------------------------------

    @Test
    void updateForUser_Owner_ShouldUpdateSuccessfully() {
        ExerciseCatalog existing = makeCustom(5L, "Curl", USER_A);
        ExerciseCatalogRequestDto dto = makeRequest("Barbell Curl");
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.findByNameAndCreatedByUserId("Barbell Curl", USER_A)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseCatalogResponseDto result = service.updateForUser(5L, dto, USER_A);

        log.info("test.updateForUser.owner name={}", result.getName());
        assertEquals("Barbell Curl", result.getName());
        verify(repository).save(any());
    }

    @Test
    void updateForUser_NotOwner_ShouldThrow403() {
        ExerciseCatalog existing = makeCustom(5L, "Curl", USER_A);
        ExerciseCatalogRequestDto dto = makeRequest("Curl");
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateForUser(5L, dto, USER_B));

        log.info("test.updateForUser.notOwner status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void updateForUser_SystemEntry_ShouldThrow403() {
        ExerciseCatalog existing = makeSystem(5L, "Deadlift");
        ExerciseCatalogRequestDto dto = makeRequest("Deadlift");
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateForUser(5L, dto, USER_A));

        log.info("test.updateForUser.systemEntry status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void updateForUser_NotFound_ShouldThrow404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateForUser(99L, makeRequest("X"), USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }

    // ---------------------------------------------------------------
    // updateSystemEntry — admin override
    // ---------------------------------------------------------------

    @Test
    void updateSystemEntry_SystemEntry_ShouldUpdateSuccessfully() {
        ExerciseCatalog existing = makeSystem(10L, "Squat");
        ExerciseCatalogRequestDto dto = makeRequest("Back Squat");
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.findByNameAndIsSystemTrue("Back Squat")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseCatalogResponseDto result = service.updateSystemEntry(10L, dto);

        log.info("test.updateSystemEntry.ok name={}", result.getName());
        assertEquals("Back Squat", result.getName());
        assertTrue(result.isSystem());
    }

    @Test
    void updateSystemEntry_CustomEntry_ShouldThrow403() {
        ExerciseCatalog existing = makeCustom(5L, "Curl", USER_A);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateSystemEntry(5L, makeRequest("Curl")));

        log.info("test.updateSystemEntry.customEntry status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void updateSystemEntry_DuplicateName_ShouldThrow409() {
        ExerciseCatalog existing = makeSystem(10L, "Squat");
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.findByNameAndIsSystemTrue("Deadlift"))
                .thenReturn(Optional.of(makeSystem(11L, "Deadlift")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateSystemEntry(10L, makeRequest("Deadlift")));

        assertEquals(409, ex.getStatusCode().value());
    }

    // ---------------------------------------------------------------
    // listForUser — scope
    // ---------------------------------------------------------------

    @Test
    void listForUser_NoFilter_ShouldReturnSystemExercises() {
        List<ExerciseCatalog> visible = List.of(makeSystem(1L, "Squat"));
        when(repository.findVisibleToUser()).thenReturn(visible);

        List<ExerciseCatalogResponseDto> result = service.listForUser(null, null, null, null);

        log.info("test.listForUser count={}", result.size());
        assertEquals(1, result.size());
        assertTrue(result.get(0).isSystem());
    }

    @Test
    void listForUser_TypeFilter_ShouldDelegateToTypeQuery() {
        when(repository.findVisibleToUserByType("STRENGTH"))
                .thenReturn(List.of(makeSystem(1L, "Squat")));

        List<ExerciseCatalogResponseDto> result = service.listForUser(null, "STRENGTH", null, null);

        log.info("test.listForUser.type count={}", result.size());
        assertEquals(1, result.size());
        verify(repository).findVisibleToUserByType("STRENGTH");
    }

    @Test
    void listForUser_NameFilter_ShouldDelegateToNameQuery() {
        when(repository.findVisibleToUserByNameContaining("press"))
                .thenReturn(List.of(makeSystem(2L, "Bench Press")));

        List<ExerciseCatalogResponseDto> result = service.listForUser("press", null, null, null);

        assertEquals(1, result.size());
        verify(repository).findVisibleToUserByNameContaining("press");
    }

    @Test
    void listForUser_MuscleGroupFilter_ShouldDelegateToMuscleGroupQuery() {
        when(repository.findVisibleToUserByMuscleGroup("CHEST"))
                .thenReturn(List.of(makeSystem(3L, "Push-up")));

        List<ExerciseCatalogResponseDto> result = service.listForUser(null, null, "CHEST", null);

        assertEquals(1, result.size());
        verify(repository).findVisibleToUserByMuscleGroup("CHEST");
    }

    @Test
    void listForUser_EquipmentFilter_ShouldDelegateToEquipmentQuery() {
        when(repository.findVisibleToUserByEquipment("BARBELL"))
                .thenReturn(List.of(makeSystem(4L, "Deadlift")));

        List<ExerciseCatalogResponseDto> result = service.listForUser(null, null, null, "BARBELL");

        assertEquals(1, result.size());
        verify(repository).findVisibleToUserByEquipment("BARBELL");
    }

    // ---------------------------------------------------------------
    // getForUser — visibility
    // ---------------------------------------------------------------

    @Test
    void getForUser_SystemEntry_ShouldBeVisibleToAnyUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(makeSystem(1L, "Squat")));

        ExerciseCatalogResponseDto result = service.getForUser(1L, USER_A);

        log.info("test.getForUser.system name={}", result.getName());
        assertEquals("Squat", result.getName());
    }

    @Test
    void getForUser_OwnCustomEntry_ShouldBeVisible() {
        when(repository.findById(2L)).thenReturn(Optional.of(makeCustom(2L, "My Curl", USER_A)));

        ExerciseCatalogResponseDto result = service.getForUser(2L, USER_A);

        assertEquals("My Curl", result.getName());
    }

    @Test
    void getForUser_OtherUsersCustomEntry_ShouldThrow403() {
        when(repository.findById(3L)).thenReturn(Optional.of(makeCustom(3L, "Their Curl", USER_B)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getForUser(3L, USER_A));

        log.info("test.getForUser.otherUser status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getForUser_NotFound_ShouldThrow404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getForUser(99L, USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }
}
