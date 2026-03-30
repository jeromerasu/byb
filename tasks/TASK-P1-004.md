# TASK-P1-004 — Progress Aggregation Endpoints

## Goal
Provide aggregation endpoints that surface actionable progress insights: combined stats, personal records per exercise, and a full progress summary (PRs, active days, weight trend, streak) — all computed from the workout log, meal log, and body metrics data.

## Priority
High

## Scope
Repo: `byb`
Area: aggregation queries, service layer, controller
Implements: TASK-API-004

## In Scope
- `ProgressController` with endpoints:
  - `GET /api/v1/progress/metrics?startDate=&endDate=` — combined stats: workout count, total volume, active days, macro totals for the period
  - `GET /api/v1/progress/personal-records` — max weight achieved per exercise for the authenticated user (optionally filter by exercise name)
  - `GET /api/v1/progress/summary?startDate=&endDate=` — combined summary object:
    - Personal records (exercise → max weight)
    - Active days count
    - Weight trend (list of `{date, weightKg}` from body metrics)
    - Current workout streak (consecutive days up to today)
- `ProgressService` implementing all aggregation logic
- Aggregation queries in `WorkoutLogRepository` and `BodyMetricsRepository` (JPQL or native SQL)
- Response DTOs: `ProgressMetricsResponse`, `PersonalRecordResponse`, `ProgressSummaryResponse`
- Structured SLF4J logging
- All endpoints return `200` with empty/zero values when no data exists — never `500`
- Unit tests for service layer (≥80% coverage on business logic)
- Render deploy verification

## Out of Scope
- Push notifications or goal alerts
- Historical plan comparison
- Leaderboards or social features

## Dependencies
- **TASK-P1-001** — workout logs must exist
- **TASK-P1-002** — meal logs must exist (for macro totals in combined metrics)
- **TASK-P1-003** — body metrics must exist for weight trend

## API Contract Impact

### New Endpoints
- `GET /api/v1/progress/metrics?startDate=&endDate=`
- `GET /api/v1/progress/personal-records`
- `GET /api/v1/progress/summary?startDate=&endDate=`

### Sample Response — GET /api/v1/progress/metrics
```json
{
  "periodStart": "2026-03-01",
  "periodEnd": "2026-03-29",
  "workoutCount": 12,
  "activeDays": 10,
  "totalVolumeKg": 4800.0,
  "totalCaloriesLogged": 28000
}
```

### Sample Response — GET /api/v1/progress/personal-records
```json
[
  { "exerciseName": "Bench Press", "maxWeightKg": 100.0, "achievedAt": "2026-03-15" },
  { "exerciseName": "Squat", "maxWeightKg": 140.0, "achievedAt": "2026-03-22" }
]
```

### Sample Response — GET /api/v1/progress/summary
```json
{
  "personalRecords": [
    { "exerciseName": "Bench Press", "maxWeightKg": 100.0 }
  ],
  "activeDays": 10,
  "currentStreak": 4,
  "weightTrend": [
    { "date": "2026-03-01", "weightKg": 84.0 },
    { "date": "2026-03-15", "weightKg": 83.2 },
    { "date": "2026-03-29", "weightKg": 82.5 }
  ]
}
```

## Acceptance Criteria
1. `GET /api/v1/progress/metrics` returns correct workout count, active days, and total volume for the requested date range.
2. `GET /api/v1/progress/personal-records` returns one entry per distinct exercise with the max `weightKg` ever logged by the user.
3. `GET /api/v1/progress/summary` returns a single object containing PRs, active days, current streak, and weight trend; all fields present (empty arrays/zero counts if no data).
4. All endpoints scope results strictly to the authenticated user.
5. All endpoints return `200` with empty/zero data when no logs exist — never `500`.
6. All endpoints verified reachable on Render after deploy.

## Files Likely Affected
- `src/main/java/.../controller/ProgressController.java` (new)
- `src/main/java/.../service/ProgressService.java` (new)
- `src/main/java/.../repository/WorkoutLogRepository.java` (add aggregation query methods)
- `src/main/java/.../repository/BodyMetricsRepository.java` (add trend/streak methods)
- `src/main/java/.../repository/MealLogRepository.java` (add macro aggregation methods)
- `src/main/java/.../dto/ProgressMetricsResponse.java` (new)
- `src/main/java/.../dto/PersonalRecordResponse.java` (new)
- `src/main/java/.../dto/ProgressSummaryResponse.java` (new)
- `src/test/java/.../service/ProgressServiceTest.java` (new)

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Register/authenticate a user and seed workout log entries for multiple exercises over several days, including repeat entries to verify PR logic picks max weight.
3. Seed body metrics entries on different dates.
4. `GET /api/v1/progress/metrics?startDate=2026-03-01&endDate=2026-03-31`; verify workout count, active days, total volume are accurate.
5. `GET /api/v1/progress/personal-records`; verify one record per exercise with correct max weight.
6. `GET /api/v1/progress/summary`; verify all sub-fields are present and consistent with seeded data.
7. Call all endpoints for a user with no data; confirm `200` with empty/zero results (no 500).
8. Deploy to Render and repeat steps 4–6 against live endpoint.

## TDD + Unit Test Coverage (required)
- Write service unit tests first (red → green → refactor)
- Cover: metrics aggregation with data and empty state
- Cover: personal records — max weight per exercise, not latest
- Cover: streak calculation — consecutive days, gap breaks streak, no data returns zero
- Cover: weight trend ordering (ascending by date)
- Target ≥80% unit test coverage for `ProgressService`
- Exclude from strict threshold: DTOs, repository interface methods
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Verify all three endpoints reachable with seeded data
- Verify empty-state handling (all endpoints return 200 with no data)
- Document exact curl commands and seeded data description

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for each endpoint)
- JaCoCo coverage report summary
- Runtime proof block: startup log, curl commands with responses, seeded data description
- Render verification after deploy

## Status
`BACKLOG`

## Notes
- Must not be started until TASK-P1-001, TASK-P1-002, and TASK-P1-003 are DONE
- Streak calculation: count consecutive days with at least one workout log up to and including today
- Personal records look at all-time max weight, not just the requested date range
