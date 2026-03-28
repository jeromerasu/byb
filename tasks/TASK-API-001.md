# TASK-API-001 — Workout Logging Endpoints

## Goal
Expose REST endpoints for logging individual workout sessions with full exercise detail (sets, reps, weight, duration, exercise type, notes), and enhance the existing `WorkoutLog` model and schema to capture all required fields.

## Priority
High

## Scope
Repo: `byb`
Area: entity/model enhancement, Flyway migration, repository, service, controller

## In Scope
- Enhance `WorkoutLog` entity with missing fields:
  - `sets` (integer)
  - `reps` (integer)
  - `weight_kg` (decimal)
  - `duration_minutes` (integer)
  - `exercise_type` (string/enum: e.g. STRENGTH, CARDIO, FLEXIBILITY)
  - `notes` (text, nullable)
- Flyway migration to add new columns to the existing `workout_log` table
- CRUD REST endpoints under `/api/v1/workout/logs`:
  - `POST /api/v1/workout/logs` — create a log entry
  - `GET /api/v1/workout/logs?startDate=&endDate=` — get logs by date range
  - `PUT /api/v1/workout/logs/{id}` — update a log entry
  - `DELETE /api/v1/workout/logs/{id}` — delete a log entry
- Repository methods for date-range queries and per-exercise history lookups
- Request/response DTOs with validation annotations
- Proper structured logging (SLF4J) for CRUD operations

## Out of Scope
- Aggregation or personal-records computation (see TASK-API-004)
- Plan generation integration

## Acceptance Criteria
1. `WorkoutLog` entity and DB schema include all required fields with appropriate constraints.
2. Flyway migration runs cleanly on an existing schema without data loss.
3. `POST /api/v1/workout/logs` creates a persisted log entry and returns the created resource.
4. `GET /api/v1/workout/logs?startDate=&endDate=` returns only entries within the given date range for the authenticated user.
5. `PUT` and `DELETE` endpoints enforce ownership — users cannot modify another user's logs.
6. Input validation rejects missing required fields with a `400` response.

## Files Likely Affected
- `src/main/java/.../model/WorkoutLog.java`
- `src/main/java/.../repository/WorkoutLogRepository.java`
- `src/main/java/.../service/WorkoutLogService.java` (new or existing)
- `src/main/java/.../controller/WorkoutLogController.java` (new or existing)
- `src/main/java/.../dto/WorkoutLogRequest.java` (new)
- `src/main/java/.../dto/WorkoutLogResponse.java` (new)
- `src/main/resources/db/migration/V{n}__enhance_workout_log.sql`

## Test Steps
1. Start app with test profile.
2. Register/authenticate a user and obtain a JWT.
3. `POST /api/v1/workout/logs` with full payload; confirm `201` and persisted fields.
4. `GET /api/v1/workout/logs?startDate=2026-03-01&endDate=2026-03-31`; confirm only own entries returned.
5. `PUT /api/v1/workout/logs/{id}` with updated reps; confirm change persisted.
6. `DELETE /api/v1/workout/logs/{id}`; confirm `204` and entry no longer returned.
7. Attempt to update/delete another user's log; confirm `403` or `404`.

## Runtime Proof Requirements
- Curl or HTTP request/response examples for each endpoint
- DB query showing persisted row with all new columns populated
- Runtime proof block per TESTING_REQUIREMENTS.md

## Deliverables
- Commit hash
- Changed files
- Migration SQL
- Request/response examples
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
