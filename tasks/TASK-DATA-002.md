# TASK-DATA-002 — Food Catalog

## Goal
Create a global food/meal catalog with admin-managed system entries and user-created custom entries, with macro/calorie data and a FK link to MealLog.

## Priority
Medium

## Scope
Repo: `byb`
Area: migrations, entities/repositories, service, controller, DTOs

## In Scope
- Flyway migration: create `food_catalog` table with all required columns, constraints, and indexes
- Add `food_catalog_id` FK column to `meal_log` table (nullable, migration)
- Java entity `FoodCatalog` (camelCase fields mapping to snake_case DB columns)
- Repository with core query methods (by name, category, is_system, created_by_user_id)
- Service layer enforcing access rules:
  - Admin: create/update any system entry
  - User: create/update only their own custom entries
  - Read: system entries + current user's custom entries
- Controller endpoints (user-facing and admin-facing)
- Request/response DTOs
- Proper structured logging (SLF4J) for catalog operations
- Input validation and clear error responses

## Out of Scope
- Bulk seed data for system foods
- Calorie/macro calculation from serving size changes
- Barcode scanning or third-party food database integration

## Dependencies
None — can run independently and in parallel with TASK-DATA-001

## Data Model

### Table: `food_catalog`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| name | VARCHAR NOT NULL | |
| category | VARCHAR | PROTEIN, CARB, FAT, VEGETABLE, FRUIT, DAIRY, SNACK |
| serving_size | VARCHAR | e.g. "100g", "1 cup" |
| calories | INT | |
| protein_grams | DECIMAL | |
| carbs_grams | DECIMAL | |
| fat_grams | DECIMAL | |
| fiber_grams | DECIMAL nullable | |
| is_system | BOOLEAN default true | true=admin-created, false=user-created |
| created_by_user_id | UUID FK → users nullable | null for system entries |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

Constraints:
- `UNIQUE(name, created_by_user_id)`
- Index on `category`, `is_system`, `created_by_user_id`

### `meal_log` amendment
- Add column `food_catalog_id BIGINT FK → food_catalog nullable`

## API Contract Impact

### User-Facing Endpoints
- `POST /api/v1/foods` — create food; admin creates system entry (is_system=true), user creates custom entry (is_system=false, created_by_user_id set from JWT)
- `PUT /api/v1/foods/{id}` — update food; admin may update any system entry, user may only update their own custom entries
- `GET /api/v1/foods` — list foods (system entries + current user's custom entries); query params: `name`, `category`
- `GET /api/v1/foods/{id}` — get single food (system or owned by current user)

### Admin-Only Endpoints
- `POST /api/v1/admin/foods` — create system food (admin role required)
- `PUT /api/v1/admin/foods/{id}` — update system food (admin role required)

### Sample Request — Create Food (User)
```json
POST /api/v1/foods
{
  "name": "Greek Yogurt",
  "category": "DAIRY",
  "servingSize": "200g",
  "calories": 130,
  "proteinGrams": 17.0,
  "carbsGrams": 9.0,
  "fatGrams": 3.5,
  "fiberGrams": 0.0
}
```

### Sample Response — Food
```json
{
  "id": 7,
  "name": "Greek Yogurt",
  "category": "DAIRY",
  "servingSize": "200g",
  "calories": 130,
  "proteinGrams": 17.0,
  "carbsGrams": 9.0,
  "fatGrams": 3.5,
  "fiberGrams": 0.0,
  "isSystem": false,
  "createdByUserId": "uuid-of-user",
  "createdAt": "2026-03-29T00:00:00Z",
  "updatedAt": "2026-03-29T00:00:00Z"
}
```

Backward compatibility: no existing endpoints changed; meal_log FK column is nullable with no NOT NULL constraint.

## Acceptance Criteria
1. Migration creates `food_catalog` table with all columns, the `UNIQUE(name, created_by_user_id)` constraint, required indexes, and the nullable `food_catalog_id` FK on `meal_log`.
2. Users can create, update, and read only their own custom entries; system entries are read-only for users.
3. Admins can create and update system entries via both user-facing and admin-only endpoints.
4. `GET /api/v1/foods` returns system entries plus only the current user's custom entries; no other users' custom entries are exposed.
5. Duplicate `(name, created_by_user_id)` insert is rejected with a clear 409 error response.
6. Adding `food_catalog_id` to `meal_log` does not break any existing meal log reads or writes.
7. All new business logic has ≥80% unit test coverage (service layer); entity/DTO/config classes excluded from strict threshold.

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Verify migration applied: confirm `food_catalog` table and `meal_log.food_catalog_id` column exist.
3. Register/login as a regular user; call `POST /api/v1/foods` with valid payload — expect 201 with `isSystem=false`.
4. Call `PUT /api/v1/foods/{id}` on own custom entry — expect 200.
5. Attempt `PUT /api/v1/foods/{id}` on another user's entry — expect 403.
6. Attempt to create a duplicate `(name, created_by_user_id)` — expect 409.
7. Call `GET /api/v1/foods` — confirm only system entries and own custom entries are returned.
8. Call `GET /api/v1/foods?category=PROTEIN` — confirm filtering works.
9. As admin, call `POST /api/v1/admin/foods` — expect 201 with `isSystem=true`.
10. Attempt `POST /api/v1/admin/foods` as non-admin — expect 403.
11. Verify existing meal log endpoints still return 200 with no regression.

## TDD + Unit Test Coverage (required)
- Write/commit unit tests first for service layer access-rule logic (red → green → refactor)
- Cover: create (admin vs user path), update (ownership check, admin override), list (filter by user scope), duplicate name conflict
- Target ≥80% unit test coverage for `FoodCatalogService`
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
- `src/main/resources/db/migration/V{next}__create_food_catalog.sql`
- `src/main/resources/db/migration/V{next+1}__add_food_catalog_fk_to_meal_log.sql`
- `src/main/java/com/workoutplanner/model/FoodCatalog.java`
- `src/main/java/com/workoutplanner/model/MealLog.java` (add field)
- `src/main/java/com/workoutplanner/repository/FoodCatalogRepository.java`
- `src/main/java/com/workoutplanner/service/FoodCatalogService.java`
- `src/main/java/com/workoutplanner/controller/FoodCatalogController.java`
- `src/main/java/com/workoutplanner/controller/AdminFoodCatalogController.java`
- `src/main/java/com/workoutplanner/dto/FoodCatalogRequestDto.java`
- `src/main/java/com/workoutplanner/dto/FoodCatalogResponseDto.java`
- `src/test/java/com/workoutplanner/service/FoodCatalogServiceTest.java`

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
- Dependency graph: TASK-DATA-001 and TASK-DATA-002 are fully parallel; both are independent of the TASK-API-xxx series
- `calories` stored as INT; `protein_grams`, `carbs_grams`, `fat_grams`, `fiber_grams` stored as DECIMAL for precision
- `serving_size` is a free-text string (e.g. "100g", "1 cup") — no unit parsing required in this task
