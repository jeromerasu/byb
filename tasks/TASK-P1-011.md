# TASK-P1-011 — Feedback + Progressive Overload + AI Prompt Integration

## Goal
Enable users to rate and comment on workouts and meals at log time. Compute progressive overload suggestions by comparing prescribed plan vs actual performance. Feed all feedback and progression data into the OpenAI prompt for next week's plan generation.

## Priority
High

## Scope
Repo: `byb`
Area: controller, service, overload computation, OpenAI prompt builder
Implements: TASK-API-005

## In Scope
- Confirm existing feedback fields on WorkoutLog and MealLog are accepted at log time (no new endpoints for submission)
- `GET /api/v1/workout/feedback?from=DATE&to=DATE` — retrieve workout entries with non-null feedback
- `GET /api/v1/diet/feedback?from=DATE&to=DATE` — retrieve diet entries with non-null feedback
- `GET /api/v1/progress/overload-summary?from=DATE&to=DATE` — per-exercise progression analysis
- Progressive overload rule engine in service layer
- OpenAI prompt builder integration — inject feedback and overload summary into plan generation
- Unit tests for overload computation and prompt builder
- Render deploy verification

## Out of Scope
- Separate feedback submission endpoints — feedback is submitted AT LOG TIME via existing POST endpoints
- Automatic plan regeneration triggered by feedback
- Feedback analytics dashboards

## Dependencies
- **TASK-P1-001** (WorkoutLog) — done
- **TASK-P1-002** (MealLog) — done

---

## Feedback at Log Time

Feedback is submitted through the existing log endpoints. No new submission endpoints are needed. The `WorkoutLog` and `MealLog` entities already have these fields — just ensure they are accepted and persisted:

- `rating` — enum or string: `TOO_EASY`, `JUST_RIGHT`, `TOO_HARD`
- `feedbackComment` — optional free text
- `painFlag` — boolean
- `substitutionRequested` — boolean

**Existing endpoints that already accept feedback:**
- `POST /api/v1/workout/logs`
- `POST /api/v1/diet/logs`

---

## Endpoints

### 1. GET /api/v1/workout/feedback
**Query params**: `from` (date), `to` (date)

Returns all workout log entries for the authenticated user where at least one feedback field is non-null, within the date range.

```
GET /api/v1/workout/feedback?from=2026-03-23&to=2026-03-30
```

**Response**:
```json
[
  {
    "logId": 101,
    "exerciseName": "Squat",
    "date": "2026-03-25",
    "sets": 3,
    "reps": 8,
    "weight": 135.0,
    "rating": "TOO_HARD",
    "feedbackComment": "Lower back tightness",
    "painFlag": true,
    "substitutionRequested": false
  }
]
```

---

### 2. GET /api/v1/diet/feedback
**Query params**: `from` (date), `to` (date)

Returns all meal log entries for the authenticated user where at least one feedback field is non-null, within the date range.

```
GET /api/v1/diet/feedback?from=2026-03-23&to=2026-03-30
```

**Response**:
```json
[
  {
    "logId": 55,
    "mealName": "Grilled Salmon",
    "mealType": "LUNCH",
    "date": "2026-03-24",
    "rating": "TOO_HARD",
    "feedbackComment": "Didn't enjoy this",
    "substitutionRequested": true
  }
]
```

---

### 3. GET /api/v1/progress/overload-summary
**Query params**: `from` (date), `to` (date)
**Powers**: progressive overload logic + AI prompt injection

Returns one entry per distinct exercise, comparing prescribed plan values against actual logged values and computing a `suggestedProgression`.

```
GET /api/v1/progress/overload-summary?from=2026-03-23&to=2026-03-30
```

**Response**:
```json
[
  {
    "exerciseName": "Bench Press",
    "prescribedSets": 3,
    "prescribedReps": 10,
    "prescribedWeight": 135.0,
    "actualSets": 3,
    "actualReps": 10,
    "actualWeight": 135.0,
    "completionRate": 100.0,
    "rating": "JUST_RIGHT",
    "painFlag": false,
    "substitutionRequested": false,
    "suggestedProgression": "INCREASE_WEIGHT"
  },
  {
    "exerciseName": "Squat",
    "prescribedSets": 3,
    "prescribedReps": 8,
    "prescribedWeight": 135.0,
    "actualSets": 2,
    "actualReps": 6,
    "actualWeight": 135.0,
    "completionRate": 50.0,
    "rating": "TOO_HARD",
    "painFlag": true,
    "substitutionRequested": false,
    "suggestedProgression": "SUBSTITUTE"
  }
]
```

---

## Progressive Overload Rules

Applied in priority order (first matching rule wins):

| Condition | Suggestion |
|---|---|
| `painFlag = true` | `SUBSTITUTE` |
| `substitutionRequested = true` | `SUBSTITUTE` |
| `feedbackComment` contains "hate", "injury", "skip", or "never again" | `SUBSTITUTE` |
| Missed workout entirely (no log entry for prescribed exercise) | `HOLD` |
| Did not complete all prescribed sets/reps | `HOLD` |
| Completed all + `TOO_HARD` (no pain) | `DECREASE` (same weight, fewer reps) |
| Completed all + `JUST_RIGHT` | `INCREASE_WEIGHT` (+5 lbs) or `INCREASE_REPS` (+1) |
| Completed all + `TOO_EASY` | `INCREASE_WEIGHT` (+10 lbs) or `ADD_SET` |

`completionRate` = (actualSets × actualReps) / (prescribedSets × prescribedReps) × 100, capped at 100%.

---

## AI Prompt Integration

When generating next week's plan (in `OpenAIService` or `PlanGenerationExecutorService`):

1. Query `overload-summary` for the previous 7 days
2. Query meal feedback for the previous 7 days
3. Build a structured feedback block and append to the system prompt

**Example prompt block**:
```
Previous Week Feedback:
- Bench Press: completed 3x10@135lbs (prescribed 3x10@135lbs). Rated JUST_RIGHT. Suggest: increase to 140lbs.
- Squat: PAIN FLAG. Comment: lower back tightness. Suggest: substitute with hip hinge alternative.
- Lunch - Grilled Salmon: DISLIKED. Suggest: replace with different protein source in the same meal slot.
```

Rules:
- If no feedback exists for the prior period, skip the block entirely — do not break the existing flow
- Meal feedback applies to that specific meal only, not the entire food category
- AI should try a different preparation or option in the same slot, not avoid the entire food group

---

## Files Likely Affected
- `src/main/java/.../controller/WorkoutLogController.java` (add GET feedback endpoint)
- `src/main/java/.../controller/DietLogController.java` (add GET feedback endpoint)
- `src/main/java/.../controller/ProgressController.java` (add overload-summary endpoint)
- `src/main/java/.../service/ProgressService.java` or new `OverloadService.java` (overload computation)
- `src/main/java/.../service/OpenAIService.java` (inject feedback block into prompt)
- `src/main/java/.../service/PlanGenerationExecutorService.java` (call overload + feedback queries before prompt build)
- `src/main/java/.../dto/WorkoutFeedbackResponse.java` (new)
- `src/main/java/.../dto/MealFeedbackResponse.java` (new)
- `src/main/java/.../dto/OverloadSummaryResponse.java` (new)
- `src/test/java/.../service/OverloadServiceTest.java` (new)
- `src/test/java/.../service/OpenAIPromptBuilderTest.java` (new or update)

## Acceptance Criteria
1. `POST /api/v1/workout/logs` and `POST /api/v1/diet/logs` accept and persist all feedback fields.
2. `GET /api/v1/workout/feedback` returns only entries with non-null feedback, scoped to the authenticated user.
3. `GET /api/v1/diet/feedback` returns only entries with non-null feedback, scoped to the authenticated user.
4. `GET /api/v1/progress/overload-summary` returns one entry per exercise with correct `completionRate` and `suggestedProgression`.
5. `SUBSTITUTE` is triggered by pain flag, substitution request, or negative keywords in comment.
6. `HOLD` is returned for missed workouts and incomplete sets/reps, not `DECREASE`.
7. Plan generation prompt includes the feedback block when prior-week data exists.
8. Plan generation prompt is unchanged when no prior feedback exists — no regression.
9. All endpoints verified reachable on Render after deploy.

## TDD + Unit Test Coverage (required)
- Write service unit tests first (red → green → refactor)
- Cover: overload computation for each rule (all 8 rule branches)
- Cover: `completionRate` edge cases (zero prescribed, partial completion, over-completion)
- Cover: keyword detection in `feedbackComment`
- Cover: prompt builder — no feedback (no block), pain flags, substitution, low ratings, mixed
- Target ≥80% unit test coverage for overload service methods and prompt builder changes
- Exclude from strict threshold: DTOs, entity POJOs
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Log a workout with `painFlag=true`; confirm overload-summary returns `SUBSTITUTE`
- Log a workout completing all sets with `JUST_RIGHT`; confirm `INCREASE_WEIGHT`
- Log a workout with partial completion; confirm `HOLD`
- Trigger plan generation; inspect logged OpenAI prompt to confirm feedback block appears
- Trigger plan generation with no feedback; confirm prompt is unchanged
- Document exact curl commands used

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for all three endpoints)
- JaCoCo coverage report summary
- Sample OpenAI prompt snippet showing feedback injection
- Runtime proof block: startup log, curl commands, prompt inspection
- Render verification after deploy

## Status
`BACKLOG`

## Notes
- Feedback submission requires no new endpoints — it already works through existing POST log endpoints
- Progressive overload rules are applied in priority order: pain/substitute checks first, then completion checks, then rating checks
- Prompt block should be a clearly labeled section so the AI can distinguish it from user profile and preferences
- Do not start until TASK-P1-001 and TASK-P1-002 are DONE (they are)
