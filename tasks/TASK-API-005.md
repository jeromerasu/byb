# TASK-API-005 — Workout & Diet Feedback + AI Prompt Integration

## Goal
Add feedback fields to `WorkoutLog` and `MealLog` tables so users can rate and comment on exercises and meals. Feed this feedback into the next week's plan generation prompt so the AI adapts plans based on user input.

## Priority
High

## Scope
Repo: `byb`
Area: entity/model enhancement, Flyway migration, repository, service, controller, OpenAI prompt builder

## In Scope
- Flyway migration to add feedback columns to `workout_log` and `meal_log` tables:
  - `workout_log`: `rating` (VARCHAR — TOO_EASY, JUST_RIGHT, TOO_HARD), `feedback_comment` (TEXT, nullable), `pain_flag` (BOOLEAN default false), `substitution_requested` (BOOLEAN default false)
  - `meal_log`: `rating` (VARCHAR — DISLIKED, OKAY, LOVED), `feedback_comment` (TEXT, nullable)
- New enums: `WorkoutRating` (TOO_EASY, JUST_RIGHT, TOO_HARD) and `MealRating` (DISLIKED, OKAY, LOVED)
- Update `WorkoutLog` and `MealLog` JPA entities with the new fields
- Feedback submission endpoints:
  - `PUT /api/v1/progress/workout-log/{id}/feedback` — submit feedback on a workout log entry
  - `PUT /api/v1/progress/meal-log/{id}/feedback` — submit feedback on a meal log entry
- Feedback retrieval endpoints:
  - `GET /api/v1/progress/workout-feedback?week=N` — get all workout feedback for a given week
  - `GET /api/v1/progress/meal-feedback?week=N` — get all meal feedback for a given week
- Repository methods to query feedback by user and week
- Modify `OpenAIService.java` prompt builder to:
  - Query previous week's feedback before generating a new plan
  - Include feedback summary in the system prompt (exercises rated too hard/easy, pain flags, substitution requests, disliked meals)
  - AI adapts next week's plan based on this feedback

## Out of Scope
- Frontend/mobile UI for feedback submission
- Push notifications triggered by feedback
- Feedback analytics dashboard

## Dependencies
- TASK-API-001 — WorkoutLog enhancement must be done first (sets, reps, duration, exercise_type, notes fields must exist)

## Constraints
- Preserve backward compatibility — new columns must be nullable or have defaults
- Add validation + clear error handling
- Keep DB/schema migrations explicit and reversible

## API Contract Impact

### PUT /api/v1/progress/workout-log/{id}/feedback
Request:
```json
{
  "rating": "TOO_HARD",
  "feedback_comment": "Struggled with the last set",
  "pain_flag": true,
  "substitution_requested": false
}
```
Response `200`:
```json
{
  "id": "uuid",
  "exercise": "Bench Press",
  "rating": "TOO_HARD",
  "feedback_comment": "Struggled with the last set",
  "pain_flag": true,
  "substitution_requested": false,
  "updated_at": "2026-03-27T10:00:00"
}
```

### PUT /api/v1/progress/meal-log/{id}/feedback
Request:
```json
{
  "rating": "LOVED",
  "feedback_comment": "Great taste, very filling"
}
```
Response `200`:
```json
{
  "id": "uuid",
  "meal_name": "Grilled Chicken",
  "rating": "LOVED",
  "feedback_comment": "Great taste, very filling",
  "updated_at": "2026-03-27T10:00:00"
}
```

### GET /api/v1/progress/workout-feedback?week=N
Response `200`:
```json
[
  {
    "id": "uuid",
    "exercise": "Squat",
    "rating": "TOO_EASY",
    "feedback_comment": "Need more weight",
    "pain_flag": false,
    "substitution_requested": false,
    "date": "2026-03-25"
  }
]
```

### GET /api/v1/progress/meal-feedback?week=N
Response `200`:
```json
[
  {
    "id": "uuid",
    "meal_name": "Oatmeal",
    "rating": "DISLIKED",
    "feedback_comment": "Too bland",
    "date": "2026-03-25"
  }
]
```

## Files Likely Affected
- `src/main/resources/db/migration/V012__add_feedback_fields_to_logs.sql`
- `src/main/java/.../model/WorkoutRating.java` (new enum)
- `src/main/java/.../model/MealRating.java` (new enum)
- `src/main/java/.../model/WorkoutLog.java`
- `src/main/java/.../model/MealLog.java`
- `src/main/java/.../repository/WorkoutLogRepository.java`
- `src/main/java/.../repository/MealLogRepository.java` (new or existing)
- `src/main/java/.../dto/WorkoutFeedbackRequest.java` (new)
- `src/main/java/.../dto/MealFeedbackRequest.java` (new)
- `src/main/java/.../service/ProgressService.java` (new or existing)
- `src/main/java/.../controller/ProgressController.java` (new or existing)
- `src/main/java/.../service/OpenAIService.java`

## Acceptance Criteria
1. Flyway migration `V012` runs cleanly on existing schema; new columns visible in DB.
2. `WorkoutLog` and `MealLog` entities include all new fields with correct JPA mappings.
3. `PUT /api/v1/progress/workout-log/{id}/feedback` persists rating, comment, pain_flag, and substitution_requested; returns `200` with updated resource.
4. `PUT /api/v1/progress/meal-log/{id}/feedback` persists rating and comment; returns `200` with updated resource.
5. `GET /api/v1/progress/workout-feedback?week=N` returns all workout log entries with feedback for week N of the authenticated user.
6. `GET /api/v1/progress/meal-feedback?week=N` returns all meal log entries with feedback for week N of the authenticated user.
7. Feedback endpoints enforce ownership — users cannot read or modify another user's feedback.
8. `OpenAIService` includes a feedback summary in the system prompt when feedback exists for the previous week.
9. AI-generated plan reflects feedback (e.g., replaces exercises flagged for substitution, adjusts difficulty for TOO_EASY/TOO_HARD ratings, avoids DISLIKED meals).

## Test Steps
1. Start app with test profile.
2. Register/authenticate a user and obtain a JWT.
3. Create a workout log entry via `POST /api/v1/workout/logs`.
4. Submit feedback via `PUT /api/v1/progress/workout-log/{id}/feedback`; confirm `200` and persisted fields.
5. `GET /api/v1/progress/workout-feedback?week=N`; confirm entry appears with feedback fields populated.
6. Create a meal log entry and repeat steps 4–5 for meal feedback.
7. Trigger plan generation and inspect the OpenAI prompt log to confirm feedback summary is included.
8. Attempt to submit feedback for another user's log entry; confirm `403` or `404`.
9. Submit feedback with invalid rating value; confirm `400` response.

## Runtime Proof Requirements
- Curl or HTTP request/response examples for each endpoint
- DB query output showing persisted rows with feedback columns populated
- OpenAI prompt log excerpt showing the feedback summary section
- Runtime proof block per TESTING_REQUIREMENTS.md

## TDD + Unit Test Coverage (required)
- Write/commit unit tests first for task scope (red -> green -> refactor)
- Add/update unit tests for all new core logic and edge cases in task scope
- Target **>=80% unit test coverage for testable business logic changed by this task**
- Exclude low-value coverage targets from strict threshold (simple DTO/entity/model POJOs, config/wiring classes, generated code)
- If excluded files are changed, provide brief justification in deliverables
- Include coverage report output (JaCoCo or equivalent) in deliverables

## Mandatory Local Testing Verification (required)
- Create and use local test profile (`application-test.properties`) per `TESTING_REQUIREMENTS.md`
- Verify app startup with test profile
- Verify endpoint reachability (no connection refused/404)
- Verify authenticated flow where applicable (test user + bearer token)
- Verify request/response schema correctness and no 500s
- Document exact commands used

## Deliverables
- Commit hash
- Changed files
- Migration SQL
- Request/response examples
- OpenAI prompt excerpt showing feedback summary
- Risks/rollback notes
- Local testing evidence (startup, endpoint checks, commands, response validation)

## Status
`READY`

## Notes
- Phase 1 (this task definition): data model + feedback field migration only
- Phase 2: feedback submission and retrieval endpoints
- Phase 3: OpenAI prompt integration
- Feedback fields must be nullable so existing log entries are unaffected
- `pain_flag` and `substitution_requested` default to `false` at DB level to avoid null checks in business logic
