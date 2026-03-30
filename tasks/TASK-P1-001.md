# TASK-P1-001 — WorkoutLog REST Layer

## Goal
Expose full CRUD REST endpoints for workout logging, wiring the existing `WorkoutLog` entity (with enhanced fields) to a service and controller layer, and linking log entries to the `ExerciseCatalog` via FK.

## Priority
High

## Scope
Repo: `byb`
Area: controller, service, repository, DTOs
Implements: TASK-API-001

## In Scope
- `WorkoutLogController` with endpoints:
  - `POST /api/v1/workout/logs` — create a log entry
  - `GET /api/v1/workout/logs?startDate=&endDate=` — list logs for authenticated user with optional date range filter
  - `GET /api/v1/workout/logs/{id}` — get a single log entry
  - `PUT /api/v1/workout/logs/{id}` — update a log entry
  - `DELETE /api/v1/workout/logs/{id}` — delete a log entry
- `WorkoutLogService` implementing all CRUD operations
- `WorkoutLogRepository` date-range query methods
- Uses existing `WorkoutLog` entity fields: `sets`, `reps`, `weight_kg`, `exercise_type`, `duration_minutes`, `notes`, feedback fields
- `exercise_catalog_id` FK linking to `ExerciseCatalog`
- Request/response DTOs (`WorkoutLogRequest`, `WorkoutLogResponse`) with validation annotations
- Ownership enforcement: users may only read/modify their own logs (403 or 404 for others)
- Structured SLF4J logging for all CRUD operations
- Unit tests for service layer (≥80% coverage on business logic)
- Render deploy verification

## Out of Scope
- Aggregation or personal-records computation (see TASK-P1-004)
- Feedback sub-resource endpoints (see TASK-P1-011)
- Plan generation integration

## Dependencies
- None — `WorkoutLog` entity and `ExerciseCatalog` already exist from TASK-DATA-001

## API Contract Impact

### New Endpoints
- `POST /api/v1/workout/logs`
- `GET /api/v1/workout/logs?startDate=&endDate=`
- `GET /api/v1/workout/logs/{id}`
- `PUT /api/v1/workout/logs/{id}`
- `DELETE /api/v1/workout/logs/{id}`

### Sample Request — POST /api/v1/workout/logs
```json
{
  "exerciseCatalogId": 42,
  "exerciseName": "Bench Press",
  "sets": 4,
  "reps": 8,
  "weightKg": 80.0,
  "exerciseType": "STRENGTH",
  "durationMinutes": 45,
  "loggedAt": "2026-03-29",
  "notes": "Felt strong today"
}
```

### Sample Response — 201 Created
```json
{
  "id": 101,
  "exerciseCatalogId": 42,
  "exerciseName": "Bench Press",
  "sets": 4,
  "reps": 8,
  "weightKg": 80.0,
  "exerciseType": "STRENGTH",
  "durationMinutes": 45,
  "loggedAt": "2026-03-29",
  "notes": "Felt strong today",
  "createdAt": "2026-03-29T10:00:00Z"
}
```

## Acceptance Criteria
1. `POST /api/v1/workout/logs` creates a persisted log entry and returns `201` with the created resource.
2. `GET /api/v1/workout/logs?startDate=&endDate=` returns only entries within the given range for the authenticated user.
3. `GET /api/v1/workout/logs/{id}` returns the entry or `404` if not found / not owned by user.
4. `PUT /api/v1/workout/logs/{id}` updates and persists changes; returns `200`.
5. `DELETE /api/v1/workout/logs/{id}` removes the entry and returns `204`.
6. Attempting to modify another user's log returns `403` or `404`.
7. Missing required fields on `POST`/`PUT` return `400` with validation errors.
8. All endpoints verified reachable on Render after deploy.

## Files Likely Affected
- `src/main/java/.../controller/WorkoutLogController.java` (new)
- `src/main/java/.../service/WorkoutLogService.java` (new or enhance existing)
- `src/main/java/.../repository/WorkoutLogRepository.java` (add date-range query methods)
- `src/main/java/.../dto/WorkoutLogRequest.java` (new)
- `src/main/java/.../dto/WorkoutLogResponse.java` (new)
- `src/test/java/.../service/WorkoutLogServiceTest.java` (new)
- `src/test/java/.../controller/WorkoutLogControllerTest.java` (new)

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Register/authenticate a user and obtain a JWT.
3. `POST /api/v1/workout/logs` with full payload; confirm `201` and all fields persisted.
4. `GET /api/v1/workout/logs?startDate=2026-03-01&endDate=2026-03-31`; confirm only own entries returned.
5. `GET /api/v1/workout/logs/{id}`; confirm correct entry returned.
6. `PUT /api/v1/workout/logs/{id}` with updated reps; confirm change persisted and `200` returned.
7. `DELETE /api/v1/workout/logs/{id}`; confirm `204` and entry no longer returned.
8. Attempt to read/update/delete another user's log; confirm `403` or `404`.
9. `POST` with missing required fields; confirm `400` with validation message.
10. Deploy to Render and repeat steps 3–7 against live endpoint.

## TDD + Unit Test Coverage (required)
- Write service unit tests first (red → green → refactor)
- Cover: create, read by range, read by id (owned and not owned), update, delete
- Cover: ownership enforcement — user cannot modify another user's log
- Cover: date-range filtering returns correct subset
- Target ≥80% unit test coverage for `WorkoutLogService`
- Exclude from strict threshold: DTOs, `WorkoutLog` entity POJO, repository interface
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Verify all five endpoints are reachable (no 404/500 on correct paths)
- Verify authenticated flow: register → JWT → CRUD sequence
- Verify date-range filter returns correct subset
- Verify ownership enforcement (cross-user test)
- Document exact curl commands used

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for each endpoint)
- JaCoCo coverage report summary
- Runtime proof block: startup log snippet, curl commands with responses, ownership enforcement demo
- Render verification: endpoint reachability confirmation after deploy

## Status
`READY`

## Notes
- Can be developed in parallel with TASK-P1-002 and TASK-P1-003
- TASK-P1-004 and TASK-P1-011 depend on this task being complete
- `exercise_catalog_id` FK should be nullable to allow free-text exercise entries not yet in catalog
