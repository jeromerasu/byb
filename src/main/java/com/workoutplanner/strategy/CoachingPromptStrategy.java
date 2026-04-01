package com.workoutplanner.strategy;

import com.workoutplanner.model.CoachDirective;
import com.workoutplanner.model.CoachPromptTemplate;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.CoachDirectiveRepository;
import com.workoutplanner.repository.CoachPromptTemplateRepository;
import com.workoutplanner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Strategy for COACHING tier users.
 *
 * Model: gpt-4o (higher-capability model for coached clients)
 *
 * System prompt resolution chain (first match wins):
 *   1. Per-client template  (coach_id + user_id)
 *   2. Coach default template (coach_id + user_id IS NULL)
 *   3. Base system prompt   (hardcoded fallback — never fails generation)
 *
 * Directives: all active CoachDirective rows for the user are injected as
 * additional system prompt lines.
 */
@Component
public class CoachingPromptStrategy implements PromptStrategy {

    private static final Logger log = LoggerFactory.getLogger(CoachingPromptStrategy.class);
    private static final String MODEL = "gpt-4o";

    private final UserRepository userRepository;
    private final CoachPromptTemplateRepository templateRepository;
    private final CoachDirectiveRepository directiveRepository;

    public CoachingPromptStrategy(UserRepository userRepository,
                                  CoachPromptTemplateRepository templateRepository,
                                  CoachDirectiveRepository directiveRepository) {
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
        this.directiveRepository = directiveRepository;
    }

    @Override
    public String resolveModel() {
        return MODEL;
    }

    @Override
    public String resolveSystemPrompt(String userId) {
        String coachId = resolveCoachId(userId);
        if (coachId == null) {
            log.warn("coaching.strategy.no_coach_assigned userId={} — falling back to base prompt", userId);
            return StandardPromptStrategy.BASE_SYSTEM_PROMPT;
        }

        // 1. Per-client template
        Optional<CoachPromptTemplate> perClient = templateRepository.findByCoachIdAndUserId(coachId, userId);
        if (perClient.isPresent()) {
            log.info("coaching.strategy.resolved_template type=per_client userId={} coachId={}", userId, coachId);
            return perClient.get().getPromptContent();
        }

        // 2. Coach default
        Optional<CoachPromptTemplate> coachDefault = templateRepository.findByCoachIdAndUserIdIsNull(coachId);
        if (coachDefault.isPresent()) {
            log.info("coaching.strategy.resolved_template type=coach_default userId={} coachId={}", userId, coachId);
            return coachDefault.get().getPromptContent();
        }

        // 3. Base fallback
        log.info("coaching.strategy.resolved_template type=base_fallback userId={} coachId={}", userId, coachId);
        return StandardPromptStrategy.BASE_SYSTEM_PROMPT;
    }

    @Override
    public List<String> resolveDirectives(String userId) {
        List<CoachDirective> directives = directiveRepository.findByUserIdAndActive(userId, true);
        if (directives.isEmpty()) {
            log.debug("coaching.strategy.no_directives userId={}", userId);
            return List.of();
        }
        log.info("coaching.strategy.directives_resolved count={} userId={}", directives.size(), userId);
        return directives.stream()
                .map(d -> "[" + d.getDirectiveType().name() + "] " + d.getContent())
                .collect(Collectors.toList());
    }

    private String resolveCoachId(String userId) {
        return userRepository.findById(userId)
                .map(User::getCoachId)
                .orElse(null);
    }
}
