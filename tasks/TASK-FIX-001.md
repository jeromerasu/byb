# TASK-FIX-001 — Fix Current Week Diet Parsing

## Goal
Fix two bugs in the current-week diet response: macro fields (protein, carbs, fat) always return `0`, and all meal types default to `"snack"` instead of reflecting the correct values (breakfast, lunch, dinner). The raw plan data in MinIO/storage is correct — the defect is in the parsing or DTO mapping layer.

## Priority
High

## Scope
Repo: `byb`
Area: parsing service, DTO mapping, current-week endpoint

## In Scope
- Locate the parsing/mapping code responsible for deserializing stored diet plan JSON into the response DTOs for `GET /api/v1/plan/current-week`
- Fix the macro field mapping so `protein_g`, `carbs_g`, and `fat_g` are read from the correct JSON keys in the stored plan
- Fix the meal-type mapping so values are read and normalized correctly (BREAKFAST, LUNCH, DINNER, SNACK) rather than all defaulting to SNACK
- Add or improve unit tests covering macro field and meal-type parsing with a realistic stored-plan JSON fixture
- Verify the fix against a real stored plan in the test environment

## Out of Scope
- Changes to the OpenAI prompt or plan generation logic
- Changes to the storage/retrieval layer (MinIO or local file storage)
- New fields beyond what is already in the stored plan

## Acceptance Criteria
1. `GET /api/v1/plan/current-week` returns non-zero `protein_g`, `carbs_g`, and `fat_g` values for meals that have macros in the stored plan.
2. `GET /api/v1/plan/current-week` returns the correct `meal_type` for each meal (BREAKFAST, LUNCH, DINNER, SNACK) as it appears in the stored plan.
3. A unit test exercises the parser with a sample stored-plan JSON and asserts correct macro and meal-type values.
4. No regression in workout plan fields returned by the same endpoint.

## Files Likely Affected
- `src/main/java/.../service/PlanParsingService.java` (or equivalent parsing class)
- `src/main/java/.../dto/DietDayResponse.java` / `MealResponse.java` (or equivalent response DTOs)
- DTO mapper classes that convert stored plan domain objects to response objects
- `src/test/java/.../service/PlanParsingServiceTest.java` (new or existing)

## Test Steps
1. Start app with test profile.
2. Ensure a plan has been generated and stored (or use an existing stored plan).
3. `GET /api/v1/plan/current-week`; inspect the `dietPlan` section of the response.
4. Confirm `protein_g`, `carbs_g`, and `fat_g` are non-zero for meals that have macro data in storage.
5. Confirm `meal_type` reflects the correct values (breakfast, lunch, dinner entries are not all showing as snack).
6. Run unit tests for the parsing service and confirm they pass.

## Runtime Proof Requirements
- `GET /api/v1/plan/current-week` response snippet showing corrected macro and meal-type fields
- Side-by-side comparison of stored plan JSON vs. response DTO to demonstrate correct mapping
- Runtime proof block per TESTING_REQUIREMENTS.md

## Deliverables
- Commit hash
- Changed files
- Before/after response examples showing the fix
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
