package com.workoutplanner.service;

import com.workoutplanner.dto.MealFeedbackResponse;
import com.workoutplanner.dto.OverloadSummaryResponse;
import com.workoutplanner.dto.WorkoutFeedbackResponse;
import com.workoutplanner.model.DietFeedback;
import com.workoutplanner.model.MealLog;
import com.workoutplanner.model.WorkoutFeedback;
import com.workoutplanner.model.WorkoutLog;
import com.workoutplanner.model.WorkoutRating;
import com.workoutplanner.repository.DietFeedbackRepository;
import com.workoutplanner.repository.MealLogRepository;
import com.workoutplanner.repository.WorkoutFeedbackRepository;
import com.workoutplanner.repository.WorkoutLogRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P1-011: Feedback retrieval, progressive overload computation, and AI prompt feedback block builder.
 */
@Service
public class OverloadService {

    private static final Logger log = LoggerFactory.getLogger(OverloadService.class);

    private static final List<String> SUBSTITUTE_KEYWORDS = List.of("hate", "injury", "skip", "never again");

    private final WorkoutLogRepository workoutLogRepository;
    private final MealLogRepository mealLogRepository;
    private final WorkoutProfileRepository workoutProfileRepository;
    private final StorageService storageService;
    private final WorkoutFeedbackRepository workoutFeedbackRepository;
    private final DietFeedbackRepository dietFeedbackRepository;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    public OverloadService(WorkoutLogRepository workoutLogRepository,
                           MealLogRepository mealLogRepository,
                           WorkoutProfileRepository workoutProfileRepository,
                           StorageService storageService,
                           WorkoutFeedbackRepository workoutFeedbackRepository,
                           DietFeedbackRepository dietFeedbackRepository) {
        this.workoutLogRepository = workoutLogRepository;
        this.mealLogRepository = mealLogRepository;
        this.workoutProfileRepository = workoutProfileRepository;
        this.storageService = storageService;
        this.workoutFeedbackRepository = workoutFeedbackRepository;
        this.dietFeedbackRepository = dietFeedbackRepository;
    }

    // -------------------------------------------------------------------------
    // Feedback queries
    // -------------------------------------------------------------------------

    public List<WorkoutFeedbackResponse> getWorkoutFeedback(String userId, LocalDate from, LocalDate to) {
        log.info("overload.workout_feedback userId={} from={} to={}", userId, from, to);
        return workoutLogRepository.findWithFeedbackByUserIdAndDateBetween(userId, from, to)
                .stream()
                .map(WorkoutFeedbackResponse::from)
                .collect(Collectors.toList());
    }

    public List<MealFeedbackResponse> getMealFeedback(String userId, LocalDate from, LocalDate to) {
        log.info("overload.meal_feedback userId={} from={} to={}", userId, from, to);
        return mealLogRepository.findWithFeedbackByUserIdAndDateBetween(userId, from, to)
                .stream()
                .map(MealFeedbackResponse::from)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Overload summary
    // -------------------------------------------------------------------------

    public List<OverloadSummaryResponse> getOverloadSummary(String userId, LocalDate from, LocalDate to) {
        log.info("overload.summary userId={} from={} to={}", userId, from, to);

        List<WorkoutLog> logs = workoutLogRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);
        Map<String, PrescribedValues> prescribed = loadPrescribedExercises(userId);

        // Group logs by exercise name (case-insensitive key, but preserve original name)
        Map<String, List<WorkoutLog>> byExercise = new LinkedHashMap<>();
        for (WorkoutLog log : logs) {
            String key = log.getExercise().trim().toLowerCase(Locale.ROOT);
            byExercise.computeIfAbsent(key, k -> new ArrayList<>()).add(log);
        }

        List<OverloadSummaryResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<WorkoutLog>> entry : byExercise.entrySet()) {
            String key = entry.getKey();
            List<WorkoutLog> exerciseLogs = entry.getValue();
            PrescribedValues pv = prescribed.getOrDefault(key, null);
            result.add(buildSummary(exerciseLogs, pv));
        }

        // Prescribed exercises with no log entry → HOLD
        for (Map.Entry<String, PrescribedValues> entry : prescribed.entrySet()) {
            if (!byExercise.containsKey(entry.getKey())) {
                OverloadSummaryResponse missed = new OverloadSummaryResponse();
                missed.setExerciseName(entry.getKey());
                missed.setPrescribedSets(entry.getValue().sets);
                missed.setPrescribedReps(entry.getValue().reps);
                missed.setPrescribedWeight(entry.getValue().weight);
                missed.setActualSets(0);
                missed.setActualReps(0);
                missed.setActualWeight(BigDecimal.ZERO);
                missed.setCompletionRate(0.0);
                missed.setSuggestedProgression("HOLD");
                result.add(missed);
            }
        }

        return result;
    }

    private OverloadSummaryResponse buildSummary(List<WorkoutLog> logs, PrescribedValues prescribed) {
        // Use most recent log for sets/reps/weight/rating/feedbackComment
        WorkoutLog latest = logs.get(0); // already sorted desc by date

        // Combine pain/substitution across all logs in the range
        boolean anyPain = logs.stream().anyMatch(WorkoutLog::isPainFlag);
        boolean anySubstitution = logs.stream().anyMatch(WorkoutLog::isSubstitutionRequested);
        String feedbackComment = logs.stream()
                .map(WorkoutLog::getFeedbackComment)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        WorkoutRating rating = latest.getRating();

        int actualSets = latest.getSets() != null ? latest.getSets() : 0;
        int actualReps = latest.getReps() != null ? latest.getReps() : 0;
        BigDecimal actualWeight = latest.getWeight() != null ? latest.getWeight() : BigDecimal.ZERO;

        double completionRate = computeCompletionRate(actualSets, actualReps, prescribed);

        OverloadSummaryResponse resp = new OverloadSummaryResponse();
        resp.setExerciseName(latest.getExercise());
        resp.setActualSets(actualSets);
        resp.setActualReps(actualReps);
        resp.setActualWeight(actualWeight);
        resp.setCompletionRate(completionRate);
        resp.setRating(rating);
        resp.setPainFlag(anyPain);
        resp.setSubstitutionRequested(anySubstitution);

        if (prescribed != null) {
            resp.setPrescribedSets(prescribed.sets);
            resp.setPrescribedReps(prescribed.reps);
            resp.setPrescribedWeight(prescribed.weight);
        }

        resp.setSuggestedProgression(
                suggestProgression(anyPain, anySubstitution, feedbackComment, rating, completionRate));

        return resp;
    }

    // -------------------------------------------------------------------------
    // Progressive overload rule engine (priority order)
    // -------------------------------------------------------------------------

    /**
     * Applies progressive overload rules in priority order and returns a suggestion string.
     */
    public String suggestProgression(boolean painFlag,
                                      boolean substitutionRequested,
                                      String feedbackComment,
                                      WorkoutRating rating,
                                      double completionRate) {
        // Rule 1: pain flag
        if (painFlag) return "SUBSTITUTE";

        // Rule 2: substitution requested
        if (substitutionRequested) return "SUBSTITUTE";

        // Rule 3: negative keywords in comment
        if (containsNegativeKeyword(feedbackComment)) return "SUBSTITUTE";

        // Rule 4 + 5: missed or incomplete
        if (completionRate < 100.0) return "HOLD";

        // Rule 6: completed + TOO_HARD (no pain)
        if (rating == WorkoutRating.TOO_HARD) return "DECREASE";

        // Rule 7: completed + JUST_RIGHT
        if (rating == WorkoutRating.JUST_RIGHT) return "INCREASE_WEIGHT";

        // Rule 8: completed + TOO_EASY
        if (rating == WorkoutRating.TOO_EASY) return "INCREASE_WEIGHT";

        // Default: no rating or unknown state
        return "HOLD";
    }

    /**
     * completionRate = (actualSets × actualReps) / (prescribedSets × prescribedReps) × 100, capped at 100%.
     */
    public double computeCompletionRate(int actualSets, int actualReps, PrescribedValues prescribed) {
        if (prescribed == null) return 100.0;
        int prescribedTotal = (prescribed.sets != null ? prescribed.sets : 0)
                * (prescribed.reps != null ? prescribed.reps : 0);
        if (prescribedTotal == 0) return 100.0;
        double rate = ((double) (actualSets * actualReps) / prescribedTotal) * 100.0;
        return Math.min(rate, 100.0);
    }

    public boolean containsNegativeKeyword(String comment) {
        if (comment == null || comment.isBlank()) return false;
        String lower = comment.toLowerCase(Locale.ROOT);
        return SUBSTITUTE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    // -------------------------------------------------------------------------
    // AI prompt feedback block builder
    // -------------------------------------------------------------------------

    /**
     * Builds a structured feedback block for injection into the OpenAI system prompt.
     * Returns empty string if no feedback exists.
     */
    public String buildFeedbackBlock(String userId, LocalDate from, LocalDate to) {
        List<OverloadSummaryResponse> overload = getOverloadSummary(userId, from, to);
        List<MealFeedbackResponse> mealFeedback = getMealFeedback(userId, from, to);
        List<WorkoutFeedback> sessionWorkoutFeedback = workoutFeedbackRepository.findByUserIdAndWorkoutDateAfter(userId, from);
        List<DietFeedback> sessionDietFeedback = dietFeedbackRepository.findByUserIdAndFeedbackDateAfter(userId, from);

        if (overload.isEmpty() && mealFeedback.isEmpty()
                && sessionWorkoutFeedback.isEmpty() && sessionDietFeedback.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("Previous Week Feedback:\n");

        for (WorkoutFeedback wf : sessionWorkoutFeedback) {
            sb.append("- Workout session (").append(wf.getWorkoutDate()).append("):");
            if (wf.getRating() != null) {
                sb.append(" rating=").append(wf.getRating()).append("/10.");
            }
            if (wf.getSessionComments() != null && !wf.getSessionComments().isEmpty()) {
                sb.append(" Comments: ").append(String.join("; ", wf.getSessionComments())).append(".");
            }
            if (wf.getFlaggedExercises() != null && !wf.getFlaggedExercises().isEmpty()) {
                sb.append(" Flagged exercises: ").append(String.join(", ", wf.getFlaggedExercises())).append(".");
            }
            if (wf.getFreeFormNote() != null && !wf.getFreeFormNote().isBlank()) {
                sb.append(" Note: ").append(wf.getFreeFormNote()).append(".");
            }
            sb.append("\n");
        }

        for (DietFeedback df : sessionDietFeedback) {
            sb.append("- Diet session (").append(df.getFeedbackDate()).append("):");
            if (df.getRating() != null) {
                sb.append(" rating=").append(df.getRating()).append("/10.");
            }
            if (df.getSessionComments() != null && !df.getSessionComments().isEmpty()) {
                sb.append(" Comments: ").append(String.join("; ", df.getSessionComments())).append(".");
            }
            if (df.getFlaggedMeals() != null && !df.getFlaggedMeals().isEmpty()) {
                sb.append(" Flagged meals: ").append(String.join(", ", df.getFlaggedMeals())).append(".");
            }
            if (df.getFreeFormNote() != null && !df.getFreeFormNote().isBlank()) {
                sb.append(" Note: ").append(df.getFreeFormNote()).append(".");
            }
            sb.append("\n");
        }

        for (OverloadSummaryResponse o : overload) {
            sb.append("- ").append(o.getExerciseName()).append(": ");

            if (o.isPainFlag()) {
                sb.append("PAIN FLAG.");
            } else {
                String actual = o.getActualSets() + "x" + o.getActualReps()
                        + "@" + (o.getActualWeight() != null ? o.getActualWeight() + "lbs" : "bodyweight");
                String prescribed = (o.getPrescribedSets() != null)
                        ? o.getPrescribedSets() + "x" + o.getPrescribedReps()
                          + "@" + (o.getPrescribedWeight() != null ? o.getPrescribedWeight() + "lbs" : "bodyweight")
                        : "no plan";
                sb.append("completed ").append(actual)
                        .append(" (prescribed ").append(prescribed).append(").");
                if (o.getRating() != null) {
                    sb.append(" Rated ").append(o.getRating()).append(".");
                }
            }

            String suggestion = buildWorkoutSuggestion(o);
            if (!suggestion.isEmpty()) {
                sb.append(" Suggest: ").append(suggestion).append(".");
            }
            sb.append("\n");
        }

        for (MealFeedbackResponse m : mealFeedback) {
            sb.append("- ").append(m.getMealName()).append(": ");
            if (m.getRating() != null) {
                sb.append(m.getRating()).append(".");
            }
            if (m.getFeedbackComment() != null && !m.getFeedbackComment().isBlank()) {
                sb.append(" Comment: ").append(m.getFeedbackComment()).append(".");
            }
            sb.append(" Suggest: replace with different option in the same meal slot.\n");
        }

        return sb.toString().trim();
    }

    private String buildWorkoutSuggestion(OverloadSummaryResponse o) {
        return switch (o.getSuggestedProgression()) {
            case "SUBSTITUTE" -> "substitute with alternative exercise";
            case "HOLD" -> "keep same weight and reps";
            case "DECREASE" -> "decrease weight or reps slightly";
            case "INCREASE_WEIGHT" -> {
                if (o.getRating() == WorkoutRating.TOO_EASY) {
                    String newWeight = o.getPrescribedWeight() != null
                            ? o.getPrescribedWeight().add(BigDecimal.TEN) + "lbs"
                            : "increase by 10lbs";
                    yield "increase to " + newWeight;
                } else {
                    String newWeight = o.getPrescribedWeight() != null
                            ? o.getPrescribedWeight().add(BigDecimal.valueOf(5)) + "lbs"
                            : "increase by 5lbs";
                    yield "increase to " + newWeight;
                }
            }
            default -> "";
        };
    }

    // -------------------------------------------------------------------------
    // Plan lookup — prescribed values
    // -------------------------------------------------------------------------

    private Map<String, PrescribedValues> loadPrescribedExercises(String userId) {
        try {
            return workoutProfileRepository.findByUserId(userId)
                    .filter(p -> p.getCurrentPlanStorageKey() != null)
                    .map(p -> {
                        String bucketName = betaMode ? "workoutbeta" : "workout";
                        Map<String, Object> plan = storageService.retrieveWorkoutPlan(bucketName, userId, p.getCurrentPlanStorageKey());
                        return extractPrescribedExercises(plan);
                    })
                    .orElse(Collections.emptyMap());
        } catch (Exception e) {
            log.warn("overload.prescribed_lookup_failed userId={} error={}", userId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, PrescribedValues> extractPrescribedExercises(Map<String, Object> plan) {
        Map<String, PrescribedValues> result = new HashMap<>();
        if (plan == null) return result;

        Object weeksObj = plan.get("weeks");
        if (!(weeksObj instanceof Map<?, ?> weeks)) return result;

        for (Object weekObj : weeks.values()) {
            if (!(weekObj instanceof Map<?, ?> week)) continue;
            for (Object dayObj : week.values()) {
                if (!(dayObj instanceof Map<?, ?> day)) continue;
                Object exercisesObj = day.get("exercises");
                if (!(exercisesObj instanceof List<?> exercises)) continue;
                for (Object exObj : exercises) {
                    if (!(exObj instanceof Map<?, ?> ex)) continue;
                    Object nameObj = ex.get("name");
                    if (nameObj == null) continue;
                    String name = nameObj.toString().trim();
                    if (name.isBlank()) continue;
                    Integer sets = toInt(ex.get("sets"));
                    Integer reps = toInt(ex.get("reps"));
                    result.put(name.toLowerCase(Locale.ROOT), new PrescribedValues(sets, reps, null));
                }
            }
        }
        return result;
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return null; }
    }

    // -------------------------------------------------------------------------
    // Inner type
    // -------------------------------------------------------------------------

    public static class PrescribedValues {
        public final Integer sets;
        public final Integer reps;
        public final BigDecimal weight;

        public PrescribedValues(Integer sets, Integer reps, BigDecimal weight) {
            this.sets = sets;
            this.reps = reps;
            this.weight = weight;
        }
    }
}
