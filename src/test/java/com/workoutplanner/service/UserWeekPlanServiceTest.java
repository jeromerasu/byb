package com.workoutplanner.service;

import com.workoutplanner.model.GeneratedBy;
import com.workoutplanner.model.UserWeekPlan;
import com.workoutplanner.repository.UserWeekPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserWeekPlanServiceTest {

    @Mock
    private UserWeekPlanRepository repository;

    @InjectMocks
    private UserWeekPlanService service;

    @Test
    void upsert_createsNewRow_whenNoneExists() {
        LocalDate wednesday = LocalDate.of(2026, 4, 1); // Wednesday — week starts Monday
        when(repository.findByUserIdAndWeekStart("u1", LocalDate.of(2026, 3, 30))).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserWeekPlan saved = service.upsert("u1", wednesday, "wk", "dk", GeneratedBy.MANUAL);

        assertThat(saved.getUserId()).isEqualTo("u1");
        assertThat(saved.getWeekStart()).isEqualTo(LocalDate.of(2026, 3, 30)); // Monday of the week
        assertThat(saved.getWorkoutStorageKey()).isEqualTo("wk");
        assertThat(saved.getDietStorageKey()).isEqualTo("dk");
        assertThat(saved.getGeneratedBy()).isEqualTo(GeneratedBy.MANUAL);
    }

    @Test
    void upsert_updatesExistingRow_withNewStorageKeys() {
        LocalDate monday = LocalDate.of(2026, 3, 30);
        UserWeekPlan existing = new UserWeekPlan();
        existing.setUserId("u1");
        existing.setWeekStart(monday);
        existing.setWorkoutStorageKey("old-wk");
        existing.setDietStorageKey("old-dk");
        existing.setGeneratedBy(GeneratedBy.CRON);

        when(repository.findByUserIdAndWeekStart("u1", monday)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserWeekPlan saved = service.upsert("u1", monday, "new-wk", "new-dk", GeneratedBy.COACH);

        assertThat(saved.getWorkoutStorageKey()).isEqualTo("new-wk");
        assertThat(saved.getDietStorageKey()).isEqualTo("new-dk");
        assertThat(saved.getGeneratedBy()).isEqualTo(GeneratedBy.COACH);

        // Verify we did NOT create a new row (same object mutated)
        ArgumentCaptor<UserWeekPlan> captor = ArgumentCaptor.forClass(UserWeekPlan.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void upsert_normalizesToMondayOfReferenceWeek() {
        LocalDate friday = LocalDate.of(2026, 4, 3);
        LocalDate expectedMonday = LocalDate.of(2026, 3, 30);

        when(repository.findByUserIdAndWeekStart("u1", expectedMonday)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserWeekPlan saved = service.upsert("u1", friday, "wk", "dk", GeneratedBy.CRON);
        assertThat(saved.getWeekStart()).isEqualTo(expectedMonday);
    }
}
