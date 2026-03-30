# TASK-P1-003 — BodyMetrics REST Layer

## Goal
Expose REST endpoints for recording and retrieving body metric snapshots (weight, body fat, measurements), using the existing `BodyMetrics` entity from V010 migration.

## Priority
High

## Scope
Repo: `byb`
Area: controller, service, repository, DTOs
Implements: TASK-API-003

## In Scope
- `BodyMetricsController` with endpoints:
  - `POST /api/v1/progress/body-metrics` — record a new body metrics snapshot
  - `GET /api/v1/progress/body-metrics?startDate=&endDate=` — list snapshots for authenticated user with optional date range filter
  - `GET /api/v1/progress/body-metrics/latest` — return the most recent snapshot for the authenticated user
- `BodyMetricsService` implementing all operations
- `BodyMetricsRepository` date-range query and latest-entry query methods
- Request/response DTOs (`BodyMetricsRequest`, `BodyMetricsResponse`) with validation annotations
- Ownership enforcement: users may only read their own metrics
- Structured SLF4J logging
- Unit tests for service layer (≥80% coverage on business logic)
- Render deploy verification

## Out of Scope
- Weight trend aggregation (consumed by TASK-P1-004 but computed there)
- Plan generation integration

## Dependencies
- None — `BodyMetrics` entity already exists (V010 migration)

## API Contract Impact

### New Endpoints
- `POST /api/v1/progress/body-metrics`
- `GET /api/v1/progress/body-metrics?startDate=&endDate=`
- `GET /api/v1/progress/body-metrics/latest`

### Sample Request — POST /api/v1/progress/body-metrics
```json
{
  "weightKg": 82.5,
  "bodyFatPercent": 18.2,
  "recordedAt": "2026-03-29",
  "notes": "Morning measurement, fasted"
}
```

### Sample Response — 201 Created
```json
{
  "id": 12,
  "weightKg": 82.5,
  "bodyFatPercent": 18.2,
  "recordedAt": "2026-03-29",
  "notes": "Morning measurement, fasted",
  "createdAt": "2026-03-29T07:00:00Z"
}
```

### Sample Response — GET /api/v1/progress/body-metrics/latest
```json
{
  "id": 12,
  "weightKg": 82.5,
  "bodyFatPercent": 18.2,
  "recordedAt": "2026-03-29",
  "notes": "Morning measurement, fasted",
  "createdAt": "2026-03-29T07:00:00Z"
}
```

## Acceptance Criteria
1. `POST /api/v1/progress/body-metrics` creates a persisted snapshot and returns `201` with the created resource.
2. `GET /api/v1/progress/body-metrics?startDate=&endDate=` returns only entries within the given range for the authenticated user.
3. `GET /api/v1/progress/body-metrics/latest` returns the most recent entry by `recordedAt` for the authenticated user; returns `404` (or `200` with `null`) if no entries exist.
4. All endpoints scope results strictly to the authenticated user.
5. Missing required fields on `POST` return `400` with validation errors.
6. All endpoints verified reachable on Render after deploy.

## Files Likely Affected
- `src/main/java/.../controller/BodyMetricsController.java` (new)
- `src/main/java/.../service/BodyMetricsService.java` (new)
- `src/main/java/.../repository/BodyMetricsRepository.java` (add date-range and latest query methods)
- `src/main/java/.../dto/BodyMetricsRequest.java` (new)
- `src/main/java/.../dto/BodyMetricsResponse.java` (new)
- `src/test/java/.../service/BodyMetricsServiceTest.java` (new)
- `src/test/java/.../controller/BodyMetricsControllerTest.java` (new)

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Register/authenticate a user and obtain a JWT.
3. `POST /api/v1/progress/body-metrics` with full payload; confirm `201` and all fields persisted.
4. `POST` a second entry on a different date.
5. `GET /api/v1/progress/body-metrics?startDate=2026-03-01&endDate=2026-03-31`; confirm both entries returned.
6. `GET /api/v1/progress/body-metrics/latest`; confirm the most recent entry is returned.
7. Call `GET /api/v1/progress/body-metrics/latest` as a user with no data; confirm graceful response (no 500).
8. `POST` with missing `weightKg`; confirm `400`.
9. Deploy to Render and repeat steps 3–7 against live endpoint.

## TDD + Unit Test Coverage (required)
- Write service unit tests first (red → green → refactor)
- Cover: create, list by range, latest (with data and with no data)
- Cover: date-range filtering returns correct subset
- Target ≥80% unit test coverage for `BodyMetricsService`
- Exclude from strict threshold: DTOs, `BodyMetrics` entity POJO, repository interface
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Verify all three endpoints are reachable (no 404/500 on correct paths)
- Verify authenticated flow: register → JWT → post and query metrics
- Verify `/latest` returns most recent entry and handles empty state
- Document exact curl commands used

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for each endpoint)
- JaCoCo coverage report summary
- Runtime proof block: startup log snippet, curl commands with responses
- Render verification: endpoint reachability confirmation after deploy

## Status
`READY`

## Notes
- Can be developed in parallel with TASK-P1-001 and TASK-P1-002
- TASK-P1-004 depends on this task for weight trend data
