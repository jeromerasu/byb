# TASK-P1-011 — Feedback REST Endpoints + AI Prompt Integration

## Goal
Expose REST endpoints for submitting and retrieving workout and meal log feedback, and modify the OpenAI prompt builder to include the previous week's feedback so generated plans adapt based on user experience.

## Priority
High

## Scope
Repo: `byb`
Area: controller, service, feedback DTOs, OpenAI prompt builder
Implements: TASK-API-005

## In Scope

### 1. Feedback REST endpoints
- `PUT /api/v1/workout/logs/{id}/feedback` — submit feedback for a specific workout log entry
- `PUT /api/v1/diet/logs/{id}/feedback` — submit feedback for a specific meal log entry
- `GET /api/v1/workout/feedback?week=N` — retrieve all workout feedback for week N for the authenticated user
- `GET /api/v1/diet/feedback?week=N` — retrieve all meal feedback for week N for the authenticated user
- Feedback fields (from V012 migration and enums):
  - `rating` (1–5 scale)
  - `painFlag` (boolean — exercise caused discomfort/pain)
  - `substitutionRequested` (boolean — user wants a different exercise/meal)
  - `notes` (optional free text)
- Ownership enforcement: users may only update/read their own log feedback
- Request/response DTOs with validation annotations
- Structured SLF4J logging

### 2. OpenAI prompt builder integration
- When building the prompt for plan generation, query the previous week's workout and meal feedback for the user
- Include in the prompt:
  - Exercises flagged with pain (`painFlag=true`) — instruct the AI to avoid or substitute these
  - Exercises with `substitutionRequested=true` — instruct the AI to replace with similar alternatives
  - Low-rated exercises (rating ≤ 2) — note user dislikes, suggest alternatives
  - Meals with `substitutionRequested=true` or low ratings — instruct AI to vary or replace
- Feedback injection should be conditional: if no feedback exists for the prior week, the prompt is unchanged
- Unit tests for prompt builder covering: no feedback (prompt unchanged), pain flags, substitution requests, low ratings

### 3. Tests
- Service unit tests for feedback CRUD and feedback aggregation by week
- Unit tests for prompt builder with feedback injection
- Controller tests for all four endpoints

## Out of Scope
- Automatic plan regeneration triggered by feedback (future task)
- Feedback analytics or dashboards

## Dependencies
- **TASK-P1-001** — `WorkoutLog` entries must exist; PUT feedback updates a log record
- **TASK-P1-002** — `MealLog` entries must exist; PUT feedback updates a log record

## API Contract Impact

### New Endpoints
- `PUT /api/v1/workout/logs/{id}/feedback`
- `PUT /api/v1/diet/logs/{id}/feedback`
- `GET /api/v1/workout/feedback?week=N`
- `GET /api/v1/diet/feedback?week=N`

### Sample Request — PUT /api/v1/workout/logs/{id}/feedback
```json
{
  "rating": 3,
  "painFlag": false,
  "substitutionRequested": true,
  "notes": "Too much volume for squats this week"
}
```

### Sample Response — 200 OK
```json
{
  "id": 101,
  "rating": 3,
  "painFlag": false,
  "substitutionRequested": true,
  "notes": "Too much volume for squats this week",
  "updatedAt": "2026-03-29T20:00:00Z"
}
```

### Sample Response — GET /api/v1/workout/feedback?week=12
```json
[
  {
    "logId": 101,
    "exerciseName": "Squat",
    "rating": 3,
    "painFlag": false,
    "substitutionRequested": true,
    "notes": "Too much volume for squats this week"
  },
  {
    "logId": 98,
    "exerciseName": "Deadlift",
    "rating": 5,
    "painFlag": false,
    "substitutionRequested": false,
    "notes": null
  }
]
```

## Acceptance Criteria
1. `PUT /api/v1/workout/logs/{id}/feedback` updates feedback fields on the log entry and returns `200`.
2. `PUT /api/v1/diet/logs/{id}/feedback` updates feedback fields on the meal log entry and returns `200`.
3. `GET /api/v1/workout/feedback?week=N` returns all workout log feedback for the specified week for the authenticated user.
4. `GET /api/v1/diet/feedback?week=N` returns all meal log feedback for the specified week.
5. Feedback endpoints enforce ownership — users cannot update or read another user's log feedback.
6. Rating validation: must be 1–5; returns `400` for out-of-range values.
7. Plan generation prompt includes prior week's pain flags, substitution requests, and low-rated items when feedback data exists.
8. Plan generation prompt is unchanged when no prior feedback exists (no regression).
9. All endpoints verified reachable on Render after deploy.

## Files Likely Affected
- `src/main/java/.../controller/WorkoutLogController.java` (add feedback sub-endpoints)
- `src/main/java/.../controller/DietLogController.java` (add feedback sub-endpoints)
- `src/main/java/.../service/WorkoutLogService.java` (add feedback update and aggregation)
- `src/main/java/.../service/MealLogService.java` (add feedback update and aggregation)
- `src/main/java/.../service/OpenAIPromptBuilder.java` (or equivalent — inject prior feedback)
- `src/main/java/.../dto/WorkoutFeedbackRequest.java` (new)
- `src/main/java/.../dto/MealFeedbackRequest.java` (new)
- `src/main/java/.../dto/FeedbackResponse.java` (new)
- `src/test/java/.../service/WorkoutLogServiceTest.java` (add feedback tests)
- `src/test/java/.../service/OpenAIPromptBuilderTest.java` (new or update)

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Register/authenticate a user; create workout and meal log entries (via TASK-P1-001/002 endpoints).
3. `PUT /api/v1/workout/logs/{id}/feedback` with pain flag and substitution request; confirm `200` and fields persisted.
4. `GET /api/v1/workout/feedback?week=N`; confirm entry appears with correct fields.
5. `PUT /api/v1/diet/logs/{id}/feedback` with low rating; confirm `200`.
6. `GET /api/v1/diet/feedback?week=N`; confirm entry appears.
7. Attempt feedback update on another user's log; confirm `403` or `404`.
8. Submit feedback with rating = 6; confirm `400`.
9. Trigger plan generation for a user with prior-week pain flags; inspect the outgoing OpenAI prompt (log it in test) and confirm pain-flagged exercises are mentioned.
10. Trigger plan generation for a user with no prior feedback; confirm no feedback section in the prompt.
11. Deploy to Render and verify all four endpoints are reachable.

## TDD + Unit Test Coverage (required)
- Write service unit tests first (red → green → refactor)
- Cover: feedback update (owned, not owned), feedback retrieval by week (with and without data)
- Cover: prompt builder — no feedback (no change), pain flags injected, substitution requests injected, low ratings injected
- Target ≥80% unit test coverage for feedback service methods and prompt builder changes
- Exclude from strict threshold: DTOs, entity POJO
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Verify all four feedback endpoints are reachable
- Verify feedback update flow end-to-end (create log → add feedback → retrieve feedback)
- Verify ownership enforcement (cross-user test)
- Log and inspect OpenAI prompt with prior feedback to confirm injection
- Document exact curl commands used

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for all four endpoints)
- JaCoCo coverage report summary
- Sample OpenAI prompt snippet showing feedback injection
- Runtime proof block: startup log, curl commands, prompt inspection
- Render verification after deploy

## Status
`BACKLOG`

## Notes
- Must not be started until TASK-P1-001 and TASK-P1-002 are DONE
- The V012 migration and enums already exist — no new migration needed
- Week number (`week=N`) should align with the ISO week number of the year, or use `startDate`/`endDate` if simpler — document the chosen convention
- Prompt injection should be clearly separated in the prompt (e.g., a "Previous Week Feedback" section) so the AI can clearly distinguish it from the user profile and preferences
