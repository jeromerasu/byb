# TASK-API-002 — Diet/Meal Logging Endpoints

## Goal
Expose REST endpoints for logging meals with calorie and macro detail. The `MealLog` table already exists in the database but has no REST layer; this task wires up the full CRUD API and enhances the model if any tracking fields are missing.

## Priority
High

## Scope
Repo: `byb`
Area: entity/model review, optional Flyway migration, repository, service, controller

## In Scope
- Audit `MealLog` entity for completeness; add any missing fields needed for proper tracking:
  - `meal_type` (enum: BREAKFAST, LUNCH, DINNER, SNACK)
  - `calories` (integer)
  - `protein_g` (decimal)
  - `carbs_g` (decimal)
  - `fat_g` (decimal)
  - `food_name` (string)
  - `quantity` (decimal, e.g. grams or servings)
  - `logged_at` (timestamp)
- Flyway migration to add missing columns if the audit finds gaps
- CRUD REST endpoints under `/api/v1/diet/logs`:
  - `POST /api/v1/diet/logs` — create a meal log entry
  - `GET /api/v1/diet/logs?startDate=&endDate=` — get meal logs by date range
  - `PUT /api/v1/diet/logs/{id}` — update a meal log entry
  - `DELETE /api/v1/diet/logs/{id}` — delete a meal log entry
- Repository methods for date-range queries and per-meal-type breakdowns
- Request/response DTOs with validation annotations
- Proper structured logging (SLF4J) for CRUD operations

## Out of Scope
- Daily macro totals or progress aggregation (see TASK-API-004)
- Diet plan generation or diet-plan parsing

## Acceptance Criteria
1. `MealLog` entity and DB schema contain all fields required for calorie and macro tracking.
2. Flyway migration (if needed) runs cleanly without data loss.
3. `POST /api/v1/diet/logs` creates a persisted meal log and returns the created resource.
4. `GET /api/v1/diet/logs?startDate=&endDate=` returns only entries within the given date range for the authenticated user.
5. `PUT` and `DELETE` endpoints enforce ownership — users cannot modify another user's logs.
6. Input validation rejects missing required fields with a `400` response.

## Files Likely Affected
- `src/main/java/.../model/MealLog.java`
- `src/main/java/.../repository/MealLogRepository.java`
- `src/main/java/.../service/MealLogService.java` (new or existing)
- `src/main/java/.../controller/MealLogController.java` (new)
- `src/main/java/.../dto/MealLogRequest.java` (new)
- `src/main/java/.../dto/MealLogResponse.java` (new)
- `src/main/resources/db/migration/V{n}__enhance_meal_log.sql` (if schema changes needed)

## Test Steps
1. Start app with test profile.
2. Register/authenticate a user and obtain a JWT.
3. `POST /api/v1/diet/logs` with a full meal payload; confirm `201` and persisted fields.
4. `GET /api/v1/diet/logs?startDate=2026-03-01&endDate=2026-03-31`; confirm only own entries returned.
5. `PUT /api/v1/diet/logs/{id}` with updated calories; confirm change persisted.
6. `DELETE /api/v1/diet/logs/{id}`; confirm `204` and entry no longer returned.
7. Attempt to update/delete another user's log; confirm `403` or `404`.

## Runtime Proof Requirements
- Curl or HTTP request/response examples for each endpoint
- DB query showing persisted row with macro fields populated
- Runtime proof block per TESTING_REQUIREMENTS.md

## Deliverables
- Commit hash
- Changed files
- Migration SQL (if applicable)
- Request/response examples
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
