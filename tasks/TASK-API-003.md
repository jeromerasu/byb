# TASK-API-003 — Body Metrics Logging Endpoints

## Goal
Introduce a new `BodyMetrics` entity and database table for tracking physical measurements over time (weight, body fat, muscle mass, waist circumference), and expose REST endpoints to log entries and query trends.

## Priority
High

## Scope
Repo: `byb`
Area: new entity, Flyway migration, repository, service, controller

## In Scope
- New `body_metrics` table and `BodyMetrics` entity with fields:
  - `id` (PK)
  - `user_id` (FK to users)
  - `weight_kg` (decimal, nullable)
  - `body_fat_pct` (decimal, nullable)
  - `muscle_mass_kg` (decimal, nullable)
  - `waist_cm` (decimal, nullable)
  - `recorded_at` (timestamp, not null)
  - `notes` (text, nullable)
- Flyway migration to create the table with appropriate indexes (`user_id`, `recorded_at`)
- REST endpoints under `/api/v1/metrics/body`:
  - `POST /api/v1/metrics/body` — log a body metrics entry
  - `GET /api/v1/metrics/body?startDate=&endDate=` — get trend data over a date range
  - `GET /api/v1/metrics/body/latest` — get the most recent entry for the authenticated user
  - `PUT /api/v1/metrics/body/{id}` — update an entry
  - `DELETE /api/v1/metrics/body/{id}` — delete an entry
- Repository methods for trend queries (ordered by `recorded_at`) and latest-entry lookup
- Request/response DTOs with validation annotations
- Proper structured logging (SLF4J) for CRUD and trend query operations

## Out of Scope
- Weight trend aggregation or progress summaries (see TASK-API-004)
- Body composition analysis or goal comparison

## Acceptance Criteria
1. `body_metrics` table is created by Flyway migration with correct columns and indexes.
2. `POST /api/v1/metrics/body` persists a new entry and returns the created resource.
3. `GET /api/v1/metrics/body?startDate=&endDate=` returns entries within the given range, ordered by `recorded_at` ascending, for the authenticated user only.
4. `GET /api/v1/metrics/body/latest` returns the single most recent entry; returns `404` if no entries exist.
5. `PUT` and `DELETE` enforce ownership.
6. All decimal fields are nullable so partial measurements can be recorded without errors.

## Files Likely Affected
- `src/main/java/.../model/BodyMetrics.java` (new)
- `src/main/java/.../repository/BodyMetricsRepository.java` (new)
- `src/main/java/.../service/BodyMetricsService.java` (new)
- `src/main/java/.../controller/BodyMetricsController.java` (new)
- `src/main/java/.../dto/BodyMetricsRequest.java` (new)
- `src/main/java/.../dto/BodyMetricsResponse.java` (new)
- `src/main/resources/db/migration/V{n}__create_body_metrics.sql`

## Test Steps
1. Start app with test profile and verify migration creates `body_metrics` table.
2. Register/authenticate a user and obtain a JWT.
3. `POST /api/v1/metrics/body` with weight and body fat values; confirm `201` and persisted fields.
4. Log a second entry on a different date.
5. `GET /api/v1/metrics/body?startDate=2026-03-01&endDate=2026-03-31`; confirm both entries returned in chronological order.
6. `GET /api/v1/metrics/body/latest`; confirm the most recent entry is returned.
7. `PUT /api/v1/metrics/body/{id}` to correct a weight value; confirm persisted.
8. `DELETE /api/v1/metrics/body/{id}`; confirm `204` and entry no longer returned.
9. Attempt operations on another user's entry; confirm `403` or `404`.

## Runtime Proof Requirements
- Curl or HTTP request/response examples for each endpoint
- DB query showing persisted rows in `body_metrics` table
- Runtime proof block per TESTING_REQUIREMENTS.md

## Deliverables
- Commit hash
- Changed files
- Migration SQL
- Request/response examples
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
