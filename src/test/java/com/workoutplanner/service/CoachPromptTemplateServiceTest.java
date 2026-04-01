package com.workoutplanner.service;

import com.workoutplanner.model.CoachPromptTemplate;
import com.workoutplanner.repository.CoachPromptTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachPromptTemplateServiceTest {

    @Mock
    private CoachPromptTemplateRepository repository;

    @InjectMocks
    private CoachPromptTemplateService service;

    @Test
    void upsert_createsNew_whenNoneExist() {
        when(repository.findByCoachIdAndUserId("c1", "u1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CoachPromptTemplate result = service.upsert("c1", "u1", "custom prompt");
        assertThat(result.getCoachId()).isEqualTo("c1");
        assertThat(result.getUserId()).isEqualTo("u1");
        assertThat(result.getPromptContent()).isEqualTo("custom prompt");
        assertThat(result.isCoachDefault()).isFalse();
    }

    @Test
    void upsert_updatesExisting_whenFound() {
        CoachPromptTemplate existing = new CoachPromptTemplate();
        existing.setCoachId("c1");
        existing.setUserId("u1");
        existing.setPromptContent("old");
        when(repository.findByCoachIdAndUserId("c1", "u1")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CoachPromptTemplate result = service.upsert("c1", "u1", "new");
        assertThat(result.getPromptContent()).isEqualTo("new");
        assertThat(result).isSameAs(existing);
    }

    @Test
    void upsert_nullUserId_setsCoachDefault() {
        when(repository.findByCoachIdAndUserIdIsNull("c1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CoachPromptTemplate result = service.upsert("c1", null, "default prompt");
        assertThat(result.getUserId()).isNull();
        assertThat(result.isCoachDefault()).isTrue();
    }

    @Test
    void resolve_returnsPerClient_first() {
        CoachPromptTemplate perClient = new CoachPromptTemplate();
        perClient.setUserId("u1");
        when(repository.findByCoachIdAndUserId("c1", "u1")).thenReturn(Optional.of(perClient));

        Optional<CoachPromptTemplate> resolved = service.resolve("c1", "u1");
        assertThat(resolved).isPresent().contains(perClient);
        verify(repository, never()).findByCoachIdAndUserIdIsNull(any());
    }

    @Test
    void resolve_fallsBackToDefault_whenNoPerClient() {
        when(repository.findByCoachIdAndUserId("c1", "u1")).thenReturn(Optional.empty());
        CoachPromptTemplate defaultTemplate = new CoachPromptTemplate();
        when(repository.findByCoachIdAndUserIdIsNull("c1")).thenReturn(Optional.of(defaultTemplate));

        Optional<CoachPromptTemplate> resolved = service.resolve("c1", "u1");
        assertThat(resolved).isPresent().contains(defaultTemplate);
    }

    @Test
    void resolve_returnsEmpty_whenNoTemplatesExist() {
        when(repository.findByCoachIdAndUserId("c1", "u1")).thenReturn(Optional.empty());
        when(repository.findByCoachIdAndUserIdIsNull("c1")).thenReturn(Optional.empty());

        assertThat(service.resolve("c1", "u1")).isEmpty();
    }
}
