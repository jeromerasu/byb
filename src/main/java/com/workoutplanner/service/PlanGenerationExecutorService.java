package com.workoutplanner.service;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * TASK-BE-016B: Executes plan generation for a claimed queue entry.
 *
 * Responsibilities:
 * - Load workout and diet profiles for the user
 * - Invoke OpenAIService to generate combined plans
 * - Return raw plan maps for persistence (016C)
 * - Throw descriptive exceptions on failures so 016D can decide retry vs. fatal
 */
@Service
public class PlanGenerationExecutorService {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationExecutorService.class);

    private final WorkoutProfileRepository workoutProfileRepository;
    private final DietProfileRepository dietProfileRepository;
    private final UserRepository userRepository;
    private final OpenAIService openAIService;
    private final OverloadService overloadService;

    public PlanGenerationExecutorService(WorkoutProfileRepository workoutProfileRepository,
                                         DietProfileRepository dietProfileRepository,
                                         UserRepository userRepository,
                                         OpenAIService openAIService,
                                         OverloadService overloadService) {
        this.workoutProfileRepository = workoutProfileRepository;
        this.dietProfileRepository = dietProfileRepository;
        this.userRepository = userRepository;
        this.openAIService = openAIService;
        this.overloadService = overloadService;
    }

    /**
     * Execute plan generation for a claimed queue entry.
     *
     * @param entry a CLAIMED PlanGenerationQueue entry
     * @return GenerationResult containing both raw plans
     * @throws PlanGenerationException if profiles are missing or OpenAI fails
     */
    public GenerationResult execute(PlanGenerationQueue entry) {
        String userId = entry.getUserId();
        log.info("queue.execute.start id={} userId={} attempt={}", entry.getId(), userId, entry.getAttemptCount());

        User user = loadUser(userId, entry.getId());
        WorkoutProfile workoutProfile = loadWorkoutProfile(userId, entry.getId());
        DietProfile dietProfile = loadDietProfile(userId, entry.getId());

        // Build feedback block from previous 7 days
        String feedbackBlock = buildFeedbackBlock(userId);

        log.info("queue.execute.generating id={} userId={}", entry.getId(), userId);
        try {
            OpenAIService.CombinedPlanResult result = openAIService.generateCombinedPlans(user, workoutProfile, dietProfile, feedbackBlock);

            log.info("queue.execute.done id={} userId={}", entry.getId(), userId);
            return new GenerationResult(result.getWorkoutPlan(), result.getDietPlan());

        } catch (Exception ex) {
            log.error("queue.execute.openai_error id={} userId={} error={}", entry.getId(), userId, ex.getMessage());
            throw new PlanGenerationException("OpenAI generation failed: " + ex.getMessage(), ex);
        }
    }

    private User loadUser(String userId, String entryId) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            log.error("queue.execute.no_user id={} userId={}", entryId, userId);
            throw new PlanGenerationException("User not found for userId=" + userId, true);
        }
        return opt.get();
    }

    private WorkoutProfile loadWorkoutProfile(String userId, String entryId) {
        Optional<WorkoutProfile> opt = workoutProfileRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            log.error("queue.execute.no_workout_profile id={} userId={}", entryId, userId);
            throw new PlanGenerationException("Workout profile not found for userId=" + userId, true);
        }
        return opt.get();
    }

    private DietProfile loadDietProfile(String userId, String entryId) {
        Optional<DietProfile> opt = dietProfileRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            log.error("queue.execute.no_diet_profile id={} userId={}", entryId, userId);
            throw new PlanGenerationException("Diet profile not found for userId=" + userId, true);
        }
        return opt.get();
    }

    private String buildFeedbackBlock(String userId) {
        try {
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(7);
            String block = overloadService.buildFeedbackBlock(userId, from, to);
            if (!block.isBlank()) {
                log.info("queue.execute.feedback_block_built userId={} chars={}", userId, block.length());
            } else {
                log.debug("queue.execute.no_feedback userId={}", userId);
            }
            return block;
        } catch (Exception e) {
            log.warn("queue.execute.feedback_build_failed userId={} error={}", userId, e.getMessage());
            return "";
        }
    }

    // -------------------------------------------------------------------------
    // Result and exception types
    // -------------------------------------------------------------------------

    /**
     * Holds the raw plan maps produced by OpenAI.
     */
    public static class GenerationResult {
        private final Map<String, Object> workoutPlan;
        private final Map<String, Object> dietPlan;

        public GenerationResult(Map<String, Object> workoutPlan, Map<String, Object> dietPlan) {
            this.workoutPlan = workoutPlan;
            this.dietPlan = dietPlan;
        }

        public Map<String, Object> getWorkoutPlan() { return workoutPlan; }
        public Map<String, Object> getDietPlan() { return dietPlan; }
    }

    /**
     * Signals a generation failure. fatal=true means no retry should be attempted
     * (e.g. missing profile data); fatal=false means a transient error (retry OK).
     */
    public static class PlanGenerationException extends RuntimeException {
        private final boolean fatal;

        public PlanGenerationException(String message, boolean fatal) {
            super(message);
            this.fatal = fatal;
        }

        public PlanGenerationException(String message, Throwable cause) {
            super(message, cause);
            this.fatal = false;
        }

        public boolean isFatal() { return fatal; }
    }
}
