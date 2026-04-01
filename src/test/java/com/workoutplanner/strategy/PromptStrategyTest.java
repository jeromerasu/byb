package com.workoutplanner.strategy;

import com.workoutplanner.model.CoachDirective;
import com.workoutplanner.model.CoachPromptTemplate;
import com.workoutplanner.model.DirectiveType;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.CoachDirectiveRepository;
import com.workoutplanner.repository.CoachPromptTemplateRepository;
import com.workoutplanner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptStrategyTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CoachPromptTemplateRepository templateRepository;
    @Mock
    private CoachDirectiveRepository directiveRepository;

    private StandardPromptStrategy standardStrategy;
    private CoachingPromptStrategy coachingStrategy;

    @BeforeEach
    void setUp() {
        standardStrategy = new StandardPromptStrategy();
        coachingStrategy = new CoachingPromptStrategy(userRepository, templateRepository, directiveRepository);
    }

    // --- StandardPromptStrategy ---

    @Test
    void standard_resolvesGpt4oMini() {
        assertThat(standardStrategy.resolveModel()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void standard_returnsBaseSystemPrompt() {
        assertThat(standardStrategy.resolveSystemPrompt("any")).isEqualTo(StandardPromptStrategy.BASE_SYSTEM_PROMPT);
    }

    @Test
    void standard_returnsEmptyDirectives() {
        assertThat(standardStrategy.resolveDirectives("any")).isEmpty();
    }

    // --- CoachingPromptStrategy ---

    @Test
    void coaching_resolvesGpt4o() {
        assertThat(coachingStrategy.resolveModel()).isEqualTo("gpt-4o");
    }

    @Test
    void coaching_perClientTemplateTakesPrecedence() {
        User user = new User();
        user.setCoachId("coach1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        CoachPromptTemplate template = new CoachPromptTemplate();
        template.setPromptContent("Per-client prompt");
        when(templateRepository.findByCoachIdAndUserId("coach1", "u1")).thenReturn(Optional.of(template));

        String prompt = coachingStrategy.resolveSystemPrompt("u1");
        assertThat(prompt).isEqualTo("Per-client prompt");
        verifyNoMoreInteractions(directiveRepository);
    }

    @Test
    void coaching_fallsBackToCoachDefault_whenNoPerClientTemplate() {
        User user = new User();
        user.setCoachId("coach1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(templateRepository.findByCoachIdAndUserId("coach1", "u1")).thenReturn(Optional.empty());

        CoachPromptTemplate defaultTemplate = new CoachPromptTemplate();
        defaultTemplate.setPromptContent("Coach default prompt");
        when(templateRepository.findByCoachIdAndUserIdIsNull("coach1")).thenReturn(Optional.of(defaultTemplate));

        String prompt = coachingStrategy.resolveSystemPrompt("u1");
        assertThat(prompt).isEqualTo("Coach default prompt");
    }

    @Test
    void coaching_fallsBackToBasePrompt_whenNoTemplatesExist() {
        User user = new User();
        user.setCoachId("coach1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(templateRepository.findByCoachIdAndUserId("coach1", "u1")).thenReturn(Optional.empty());
        when(templateRepository.findByCoachIdAndUserIdIsNull("coach1")).thenReturn(Optional.empty());

        String prompt = coachingStrategy.resolveSystemPrompt("u1");
        assertThat(prompt).isEqualTo(StandardPromptStrategy.BASE_SYSTEM_PROMPT);
    }

    @Test
    void coaching_fallsBackToBasePrompt_whenNoCoachAssigned() {
        User user = new User();
        user.setCoachId(null);  // no coach assigned
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        String prompt = coachingStrategy.resolveSystemPrompt("u1");
        assertThat(prompt).isEqualTo(StandardPromptStrategy.BASE_SYSTEM_PROMPT);
    }

    @Test
    void coaching_injectsActiveDirectives() {
        when(directiveRepository.findByUserIdAndActive("u1", true)).thenReturn(List.of(
                makeDirective(DirectiveType.WORKOUT, "Focus on hypertrophy"),
                makeDirective(DirectiveType.DIET, "Avoid dairy")
        ));

        List<String> directives = coachingStrategy.resolveDirectives("u1");
        assertThat(directives).hasSize(2);
        assertThat(directives.get(0)).contains("WORKOUT").contains("Focus on hypertrophy");
        assertThat(directives.get(1)).contains("DIET").contains("Avoid dairy");
    }

    @Test
    void coaching_returnsEmptyDirectives_whenNoneActive() {
        when(directiveRepository.findByUserIdAndActive("u1", true)).thenReturn(List.of());
        assertThat(coachingStrategy.resolveDirectives("u1")).isEmpty();
    }

    @Test
    void coaching_resolve_returnsFullContext() {
        User user = new User();
        user.setCoachId("coach1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(templateRepository.findByCoachIdAndUserId("coach1", "u1")).thenReturn(Optional.empty());
        when(templateRepository.findByCoachIdAndUserIdIsNull("coach1")).thenReturn(Optional.empty());
        when(directiveRepository.findByUserIdAndActive("u1", true)).thenReturn(List.of());

        ResolvedPromptContext ctx = coachingStrategy.resolve("u1");
        assertThat(ctx.model()).isEqualTo("gpt-4o");
        assertThat(ctx.directives()).isEmpty();
    }

    private CoachDirective makeDirective(DirectiveType type, String content) {
        CoachDirective d = new CoachDirective();
        d.setDirectiveType(type);
        d.setContent(content);
        d.setActive(true);
        return d;
    }
}
