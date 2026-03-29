# TASK-DATA-001 — Exercise Catalog

## Goal
Create a global exercise catalog with admin-managed system entries and user-created custom entries, including video/thumbnail URL support (MinIO keys, populated later) and a FK link to WorkoutLog.

## Priority
Medium

## Scope
Repo: `byb`
Area: migrations, entities/repositories, service, controller, DTOs

## In Scope
- Flyway migration: create `exercise_catalog` table with all required columns, constraints, and indexes
- Add `exercise_catalog_id` FK column to `workout_log` table (nullable, migration)
- Java entity `ExerciseCatalog` (camelCase fields mapping to snake_case DB columns)
- Repository with core query methods (by name, type, muscle group, equipment, is_system, created_by_user_id)
- Service layer enforcing access rules:
  - Admin: create/update any system entry
  - User: create/update only their own custom entries
  - Read: system entries + current user's custom entries
- Controller endpoints (user-facing and admin-facing)
- Request/response DTOs
- Proper structured logging (SLF4J) for catalog operations
- Input validation and clear error responses

## Out of Scope
- Video/thumbnail file upload or MinIO pre-signed URL generation (video_url/thumbnail_url are stored as strings, populated later)
- WorkoutLog service/controller changes beyond adding the nullable FK column
- Bulk seed data for system exercises

## Dependencies
None — can run independently and in parallel with TASK-DATA-002

## Data Model

### Table: `exercise_catalog`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| name | VARCHAR NOT NULL | |
| exercise_type | VARCHAR | STRENGTH, CARDIO, FLEXIBILITY, PLYOMETRIC |
| muscle_groups | TEXT[] | e.g. ["CHEST","TRICEPS","SHOULDERS"] |
| equipment_required | TEXT[] | e.g. ["BARBELL","BENCH"] |
| difficulty_level | VARCHAR | BEGINNER, INTERMEDIATE, ADVANCED |
| video_url | VARCHAR nullable | MinIO storage key, populated later |
| thumbnail_url | VARCHAR nullable | MinIO storage key, populated later |
| instructions | TEXT nullable | |
| is_system | BOOLEAN default true | true=admin-created, false=user-created |
| created_by_user_id | UUID FK → users nullable | null for system entries |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

Constraints:
- `UNIQUE(name, created_by_user_id)`
- Index on `exercise_type`, `is_system`, `created_by_user_id`

### `workout_log` amendment
- Add column `exercise_catalog_id BIGINT FK → exercise_catalog nullable`

## API Contract Impact

### User-Facing Endpoints
- `POST /api/v1/exercises` — create exercise; admin creates system entry (is_system=true), user creates custom entry (is_system=false, created_by_user_id set from JWT)
- `PUT /api/v1/exercises/{id}` — update exercise; admin may update any system entry, user may only update their own custom entries
- `GET /api/v1/exercises` — list exercises (system entries + current user's custom entries); query params: `name`, `type`, `muscleGroup`, `equipment`
- `GET /api/v1/exercises/{id}` — get single exercise (system or owned by current user)

### Admin-Only Endpoints
- `POST /api/v1/admin/exercises` — create system exercise (admin role required)
- `PUT /api/v1/admin/exercises/{id}` — update system exercise (admin role required)

### Sample Request — Create Exercise (User)
```json
POST /api/v1/exercises
{
  "name": "Incline Dumbbell Press",
  "exerciseType": "STRENGTH",
  "muscleGroups": ["CHEST", "SHOULDERS"],
  "equipmentRequired": ["DUMBBELL", "BENCH"],
  "difficultyLevel": "INTERMEDIATE",
  "instructions": "Lie on an incline bench..."
}
```

### Sample Response — Exercise
```json
{
  "id": 42,
  "name": "Incline Dumbbell Press",
  "exerciseType": "STRENGTH",
  "muscleGroups": ["CHEST", "SHOULDERS"],
  "equipmentRequired": ["DUMBBELL", "BENCH"],
  "difficultyLevel": "INTERMEDIATE",
  "videoUrl": null,
  "thumbnailUrl": null,
  "instructions": "Lie on an incline bench...",
  "isSystem": false,
  "createdByUserId": "uuid-of-user",
  "createdAt": "2026-03-29T00:00:00Z",
  "updatedAt": "2026-03-29T00:00:00Z"
}
```

Backward compatibility: no existing endpoints changed; workout_log FK column is nullable with no NOT NULL constraint.

## Acceptance Criteria
1. Migration creates `exercise_catalog` table with all columns, the `UNIQUE(name, created_by_user_id)` constraint, required indexes, and the nullable `exercise_catalog_id` FK on `workout_log`.
2. Users can create, update, and read only their own custom entries; system entries are read-only for users.
3. Admins can create and update system entries via both user-facing and admin-only endpoints.
4. `GET /api/v1/exercises` returns system entries plus only the current user's custom entries; no other users' custom entries are exposed.
5. Duplicate `(name, created_by_user_id)` insert is rejected with a clear 409 error response.
6. Adding `exercise_catalog_id` to `workout_log` does not break any existing workout log reads or writes.
7. All new business logic has ≥80% unit test coverage (service layer); entity/DTO/config classes excluded from strict threshold.

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Verify migration applied: confirm `exercise_catalog` table and `workout_log.exercise_catalog_id` column exist.
3. Register/login as a regular user; call `POST /api/v1/exercises` with valid payload — expect 201 with `isSystem=false`.
4. Call `PUT /api/v1/exercises/{id}` on own custom entry — expect 200.
5. Attempt `PUT /api/v1/exercises/{id}` on another user's entry — expect 403.
6. Attempt to create a duplicate `(name, created_by_user_id)` — expect 409.
7. Call `GET /api/v1/exercises` — confirm only system entries and own custom entries are returned.
8. Call `GET /api/v1/exercises?type=STRENGTH` — confirm filtering works.
9. As admin, call `POST /api/v1/admin/exercises` — expect 201 with `isSystem=true`.
10. Attempt `POST /api/v1/admin/exercises` as non-admin — expect 403.
11. Verify existing workout log endpoints still return 200 with no regression.

## TDD + Unit Test Coverage (required)
- Write/commit unit tests first for service layer access-rule logic (red → green → refactor)
- Cover: create (admin vs user path), update (ownership check, admin override), list (filter by user scope), duplicate name conflict
- Target ≥80% unit test coverage for `ExerciseCatalogService`
- Exclude from strict threshold: entity POJO, DTO, repository interface, migration SQL
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup (no migration errors)
- Verify all six endpoints are reachable (no 404)
- Verify authenticated flow: register user → obtain JWT → call create/read/update endpoints
- Verify admin flow: use admin credentials → call admin-only endpoints
- Verify access control: non-admin 403 on admin endpoints, user 403 on another user's entry
- Document exact curl commands used

## Files Likely Affected
- `src/main/resources/db/migration/V{next}__create_exercise_catalog.sql`
- `src/main/resources/db/migration/V{next+1}__add_exercise_catalog_fk_to_workout_log.sql`
- `src/main/java/com/workoutplanner/model/ExerciseCatalog.java`
- `src/main/java/com/workoutplanner/model/WorkoutLog.java` (add field)
- `src/main/java/com/workoutplanner/repository/ExerciseCatalogRepository.java`
- `src/main/java/com/workoutplanner/service/ExerciseCatalogService.java`
- `src/main/java/com/workoutplanner/controller/ExerciseCatalogController.java`
- `src/main/java/com/workoutplanner/controller/AdminExerciseCatalogController.java`
- `src/main/java/com/workoutplanner/dto/ExerciseCatalogRequestDto.java`
- `src/main/java/com/workoutplanner/dto/ExerciseCatalogResponseDto.java`
- `src/test/java/com/workoutplanner/service/ExerciseCatalogServiceTest.java`

## Deliverables
- Commit hash
- Changed files
- Migration SQL
- Contract examples (request/response)
- JaCoCo coverage report summary
- Runtime proof block: startup log snippet, curl commands, response bodies, access-control verification

## Status
`READY`

## Notes
- `muscle_groups` and `equipment_required` stored as PostgreSQL `TEXT[]`; for H2 test profile use JSON string or comma-delimited text with converter
- `video_url` / `thumbnail_url` are plain VARCHAR storage keys — MinIO upload integration is out of scope for this task
- Dependency graph: TASK-DATA-001 and TASK-DATA-002 are fully parallel; both are independent of the TASK-API-xxx series
