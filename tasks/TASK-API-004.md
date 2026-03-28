# TASK-API-004 — Progress & Personal Records Endpoints

## Goal
Provide aggregation endpoints that surface actionable progress insights: personal records per exercise, active training days, weight trend, current streak, and a combined progress summary — all computed from the workout and body metrics logs introduced in prior tasks.

## Priority
Medium

## Scope
Repo: `byb`
Area: aggregation queries, service layer, controller

## In Scope
- `GET /api/v1/progress/personal-records` — return the max weight achieved per exercise (at any rep count) for the authenticated user; optionally filter by exercise name
- `GET /api/v1/progress/active-days?startDate=&endDate=` — return the count of distinct calendar days on which the user logged at least one workout within the given period
- `GET /api/v1/progress/summary?startDate=&endDate=` — return a combined summary object containing:
  - Personal records (exercise → max weight)
  - Active days count
  - Weight trend (list of `{date, weight_kg}` from body metrics)
  - Current workout streak (consecutive days up to today)
- Aggregation queries in `WorkoutLogRepository` and `BodyMetricsRepository` (JPQL or native SQL as appropriate)
- Response DTOs for each endpoint
- Proper structured logging (SLF4J)

## Out of Scope
- Push notifications or goal alerts
- Historical plan comparison
- Leaderboards or social features

## Dependencies
- TASK-API-001 (workout logs must exist)
- TASK-API-002 (meal logs, referenced by summary if daily macros are included later)
- TASK-API-003 (body metrics must exist for weight trend)

## Acceptance Criteria
1. `GET /api/v1/progress/personal-records` returns one entry per distinct exercise name with the max `weight_kg` ever logged by the user.
2. `GET /api/v1/progress/active-days?startDate=&endDate=` returns an accurate count of distinct workout days in the given range.
3. `GET /api/v1/progress/summary` returns a single JSON object containing PRs, active days, weight trend, and current streak; all fields are present (empty arrays/zero counts if no data).
4. All endpoints scope results strictly to the authenticated user.
5. Endpoints return `200` with empty/zero values when no data exists — never `500`.

## Files Likely Affected
- `src/main/java/.../repository/WorkoutLogRepository.java` (new aggregation methods)
- `src/main/java/.../repository/BodyMetricsRepository.java` (new trend/streak methods)
- `src/main/java/.../service/ProgressService.java` (new)
- `src/main/java/.../controller/ProgressController.java` (new)
- `src/main/java/.../dto/PersonalRecordResponse.java` (new)
- `src/main/java/.../dto/ProgressSummaryResponse.java` (new)

## Test Steps
1. Start app with test profile.
2. Register/authenticate a user and seed workout log entries for multiple exercises over several days, including repeat entries to verify PR logic picks the max weight.
3. Seed body metrics entries on different dates.
4. `GET /api/v1/progress/personal-records`; verify one record per exercise with correct max weight.
5. `GET /api/v1/progress/active-days?startDate=2026-03-01&endDate=2026-03-31`; verify count matches distinct workout days seeded.
6. `GET /api/v1/progress/summary`; verify all sub-fields are present and values are consistent with seeded data.
7. Call all endpoints for a user with no data; confirm `200` with empty/zero results.

## Runtime Proof Requirements
- Curl or HTTP request/response examples for each endpoint
- Seeded data description (exercises, dates, weights) used to validate correctness
- Runtime proof block per TESTING_REQUIREMENTS.md

## Deliverables
- Commit hash
- Changed files
- Request/response examples
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
