package com.workoutplanner.service;

import com.workoutplanner.model.CoachDirective;
import com.workoutplanner.model.DirectiveType;
import com.workoutplanner.repository.CoachDirectiveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachDirectiveServiceTest {

    @Mock
    private CoachDirectiveRepository repository;

    @InjectMocks
    private CoachDirectiveService service;

    @Test
    void create_setsAllFieldsAndActive() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CoachDirective d = service.create("c1", "u1", DirectiveType.WORKOUT, "Focus on hypertrophy");
        assertThat(d.getCoachId()).isEqualTo("c1");
        assertThat(d.getUserId()).isEqualTo("u1");
        assertThat(d.getDirectiveType()).isEqualTo(DirectiveType.WORKOUT);
        assertThat(d.getContent()).isEqualTo("Focus on hypertrophy");
        assertThat(d.isActive()).isTrue();
    }

    @Test
    void update_togglesActiveToFalse() {
        CoachDirective existing = new CoachDirective();
        existing.setActive(true);
        when(repository.findById("d1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CoachDirective result = service.update("d1", null, false);
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void update_changesContent() {
        CoachDirective existing = new CoachDirective();
        existing.setContent("old");
        when(repository.findById("d1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CoachDirective result = service.update("d1", "new content", null);
        assertThat(result.getContent()).isEqualTo("new content");
    }

    @Test
    void update_throwsWhenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update("missing", null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivate_setsActiveFalse() {
        CoachDirective existing = new CoachDirective();
        existing.setActive(true);
        when(repository.findById("d1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate("d1");
        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void findByCoachAndUser_activeOnly_filtersCorrectly() {
        CoachDirective active = new CoachDirective();
        active.setActive(true);
        when(repository.findByCoachIdAndUserIdAndActive("c1", "u1", true)).thenReturn(List.of(active));

        List<CoachDirective> result = service.findByCoachAndUser("c1", "u1", true);
        assertThat(result).containsExactly(active);
    }
}
