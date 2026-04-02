# TASK-ATHLETIC-001 — Day Category Labels + Athletic Performance Goals

## Goal
Add a `category` field to each workout day in the AI-generated plan JSON so the mobile frontend can display a contextual split label under each day tab (e.g., "Push (Chest/Shoulders/Triceps)", "Full Body", "Speed & Agility"); extend the workout profile onboarding questionnaire with a structured athletic performance goal category and subcategory selection; and update the OpenAI prompt to generate split-appropriate exercises for all goal types including athletic performance.

## Priority
High

## Scope
Repo: `byb`
Area: workout profile model, onboarding questionnaire API, OpenAI prompt construction, plan generation response, Flyway migration

## In Scope

### 1. `AthleticGoalCategory` enum + `athleticGoalSubcategory` field on WorkoutProfile
- Create `AthleticGoalCategory` enum with values: `VERTICAL_EXPLOSIVENESS`, `SPEED_AGILITY`, `ENDURANCE`, `SPORT_SPECIFIC`
- Add `athletic_goal_category` column (nullable `VARCHAR`) to `workout_profile` table via Flyway migration; mapped as `@Enumerated(EnumType.STRING)` on `WorkoutProfile`
- Add `athletic_goal_subcategory` column (nullable `VARCHAR`) to store free-form or structured subcategory text (e.g., `"Basketball"`, `"Soccer"` for `SPORT_SPECIFIC`; `"40-yard dash"` for `SPEED_AGILITY`)
- Both columns are nullable — they are only populated when the user selects `"Athletic Performance"` as their primary goal during onboarding
- Expose both fields in the `WorkoutProfile` JSON response (`athletic_goal_category`, `athletic_goal_subcategory`)

### 2. Onboarding questionnaire API changes
- `POST /api/v1/workout/profile` — accept two new optional fields in the request body:
  - `athletic_goal_category` (string, one of `VERTICAL_EXPLOSIVENESS`, `SPEED_AGILITY`, `ENDURANCE`, `SPORT_SPECIFIC`) — required only when `target_goals` contains `"ATHLETIC_PERFORMANCE"`
  - `athletic_goal_subcategory` (string, free text, max 100 chars) — optional additional detail
- Validate: if `target_goals` contains `"ATHLETIC_PERFORMANCE"` and `athletic_goal_category` is absent, return 400 with message `"athletic_goal_category is required when target_goals includes ATHLETIC_PERFORMANCE"`
- `GET /api/v1/workout/profile` — include `athletic_goal_category` and `athletic_goal_subcategory` in the response
- `PUT /api/v1/workout/profile` (if it exists) — allow updating both new fields

### 3. Day split category logic (`DayCategoryResolver`)
- Create a new `DayCategoryResolver` service/component with a single responsibility: given `workoutFrequency` (int) and `athleticGoalCategory` (nullable enum), return an ordered list of day category label strings for the week
- Frequency-based split rules (applied when `athletic_goal_category` is null or goal is not `"ATHLETIC_PERFORMANCE"`):
  - 3 days → Full Body splits: `["Full Body A", "Full Body B", "Full Body C"]`
  - 4 days → Upper/Lower splits: `["Upper Body", "Lower Body", "Upper Body", "Lower Body"]`
  - 5 days → Push/Pull/Legs + extras: `["Push (Chest/Shoulders/Triceps)", "Pull (Back/Biceps)", "Legs & Glutes", "Push (Chest/Shoulders/Triceps)", "Pull (Back/Biceps)"]`
  - 6 days → Push/Pull/Legs full rotation: `["Push (Chest/Shoulders/Triceps)", "Pull (Back/Biceps)", "Legs & Glutes", "Push (Chest/Shoulders/Triceps)", "Pull (Back/Biceps)", "Legs & Glutes"]`
  - 1-2 days → `["Full Body"]` (repeated as needed)
  - Other → `["Full Body"]` (default fallback)
- Athletic goal overrides (applied when `target_goals` includes `"ATHLETIC_PERFORMANCE"`):
  - `VERTICAL_EXPLOSIVENESS` → labels rotate through: `["Plyometrics & Explosiveness", "Lower Body Strength", "Power & Speed", ...]` (repeat/cycle as needed for frequency)
  - `SPEED_AGILITY` → `["Sprint Mechanics & Agility", "Lower Body Strength", "Lateral Quickness", ...]`
  - `ENDURANCE` → `["Aerobic Base", "Tempo Run / Steady State", "Interval Training", ...]`
  - `SPORT_SPECIFIC` → `["Sport-Specific Conditioning", "Strength & Power", "Agility & Reaction", ...]`
  - For all athletic categories, cycle through the label set if `workoutFrequency` exceeds the defined rotation length
- `DayCategoryResolver` is pure logic (no JPA, no I/O) — easy to unit test

### 4. `category` field in generated plan JSON
- Update `OpenAIService.buildCombinedPrompt()` to instruct the model to include a `"category"` string field on each workout day object in the generated JSON, using the labels produced by `DayCategoryResolver`
- The prompt must enumerate the ordered day labels explicitly so the model assigns them in sequence (Day 1 → label[0], Day 2 → label[1], etc.)
- The generated JSON structure per day must include at minimum:
  ```json
  {
    "day": 1,
    "category": "Push (Chest/Shoulders/Triceps)",
    "exercises": [...]
  }
  ```
- The `category` field must also be surfaced in the stored plan JSON written to object storage
- No changes to the top-level plan storage key path or object structure; `category` is additive within each day object

### 5. Prompt changes for athletic performance goal
- When `athleticGoalCategory` is non-null, append a structured athletic performance directive block to the system prompt in `buildCombinedPrompt()`:
  - Directive must specify: goal subcategory (e.g., "Basketball" for `SPORT_SPECIFIC`), training emphasis (e.g., "prioritize plyometric volume, reactive strength, and single-leg power work"), and exercise selection constraints (e.g., "include box jumps, depth drops, banded hip work, and sprint drills; avoid pure bodybuilding isolation movements")
  - Each `AthleticGoalCategory` value maps to a distinct directive template string; templates are constants in a new `AthleticPromptDirectives` utility class (not hardcoded inline in the service)
- For non-athletic goals, prompt behavior is unchanged

### 6. Flyway migration
- Single migration: `V{next}__add_athletic_goal_to_workout_profile.sql`
  - `ALTER TABLE workout_profile ADD COLUMN athletic_goal_category VARCHAR(50);`
  - `ALTER TABLE workout_profile ADD COLUMN athletic_goal_subcategory VARCHAR(100);`
- Both columns nullable with no default — backward compatible with all existing rows

## Out of Scope
- Mobile frontend implementation of the day category label UI (mobile team consumes `category` field from the API response; no frontend code lives in this repo)
- Diet plan changes — only the workout side of the combined plan is affected
- Coach-specific prompt overrides for athletic goals (out of scope; handled in TASK-COACHING-001 via `CoachDirective`)
- Multi-sport selection (only one `athletic_goal_category` per profile; subcategory is free text)
- Changing existing `targetGoals` string array values or their validation beyond the new `ATHLETIC_PERFORMANCE` check

## Dependencies
- None blocking — `WorkoutProfile`, `OpenAIService.buildCombinedPrompt()`, and `WorkoutController` all exist and are stable
- TASK-COACHING-001 is independent; no sequencing required (athletic prompt directives live in `AthleticPromptDirectives`, not in `PromptStrategy`)

## API Contract Impact

### Changed Endpoints
- `POST /api/v1/workout/profile` — accepts two new optional fields; 400 when `ATHLETIC_PERFORMANCE` goal is present without `athletic_goal_category`
- `GET /api/v1/workout/profile` — response includes `athletic_goal_category` and `athletic_goal_subcategory` (null for existing users)

### New Fields in Generated Plan JSON
Each workout day object gains a `"category"` field. Existing consumers that ignore unknown fields are unaffected. Consumers that parse day objects strictly must be updated to handle the new field.

### Backward Compatibility
- `athletic_goal_category` and `athletic_goal_subcategory` are nullable columns — existing `WorkoutProfile` rows are unaffected; new fields are `null` for users who do not select `ATHLETIC_PERFORMANCE`
- `category` is additive in the plan JSON; no existing day fields are removed or renamed
- Existing `target_goals` string array contract is unchanged; `"ATHLETIC_PERFORMANCE"` is a new valid value, not a replacement

### Sample Request — POST /api/v1/workout/profile (with athletic goal)
```json
{
  "fitness_level": "INTERMEDIATE",
  "workout_frequency": 5,
  "session_duration": 60,
  "preferred_workout_types": ["STRENGTH", "PLYOMETRICS"],
  "available_equipment": ["BARBELL", "BOX", "RESISTANCE_BANDS"],
  "target_goals": ["ATHLETIC_PERFORMANCE"],
  "athletic_goal_category": "VERTICAL_EXPLOSIVENESS",
  "athletic_goal_subcategory": "Basketball"
}
```

### Sample Response — GET /api/v1/workout/profile
```json
{
  "user_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fitness_level": "INTERMEDIATE",
  "workout_frequency": 5,
  "target_goals": ["ATHLETIC_PERFORMANCE"],
  "athletic_goal_category": "VERTICAL_EXPLOSIVENESS",
  "athletic_goal_subcategory": "Basketball",
  "created_at": "2026-04-01T10:00:00Z",
  "updated_at": "2026-04-01T10:00:00Z"
}
```

### Sample Generated Plan Day Object
```json
{
  "day": 1,
  "category": "Plyometrics & Explosiveness",
  "focus": "Lower body power and reactive strength",
  "exercises": [
    { "name": "Box Jumps", "sets": 4, "reps": "6", "muscle": "Glutes/Quads" },
    { "name": "Depth Drops", "sets": 3, "reps": "5", "muscle": "Hamstrings" },
    { "name": "Banded Hip Thrusts", "sets": 3, "reps": "12", "muscle": "Glutes" }
  ]
}
```

### Sample Generated Plan Day Object (standard 4-day Upper/Lower)
```json
{
  "day": 2,
  "category": "Lower Body",
  "exercises": [
    { "name": "Barbell Back Squat", "sets": 4, "reps": "8", "muscle": "Quads/Glutes" },
    { "name": "Romanian Deadlift", "sets": 3, "reps": "10", "muscle": "Hamstrings" }
  ]
}
```

## Acceptance Criteria
1. `POST /api/v1/workout/profile` with `target_goals: ["ATHLETIC_PERFORMANCE"]` and no `athletic_goal_category` returns 400 with message `"athletic_goal_category is required when target_goals includes ATHLETIC_PERFORMANCE"`.
2. `POST /api/v1/workout/profile` with `athletic_goal_category: "VERTICAL_EXPLOSIVENESS"` and `athletic_goal_subcategory: "Basketball"` saves both fields; `GET /api/v1/workout/profile` returns them correctly.
3. `DayCategoryResolver` returns the correct ordered label list for each supported frequency (1–6 days) when no athletic goal is set.
4. `DayCategoryResolver` returns the correct athletic-goal-specific label list for each `AthleticGoalCategory` value, cycling for frequencies that exceed the rotation length.
5. Each workout day object in the generated plan JSON contains a `"category"` field matching the label assigned by `DayCategoryResolver` for that day index.
6. For a user with `VERTICAL_EXPLOSIVENESS` goal, the generated plan exercises include plyometric movements (e.g., box jumps, depth drops) as confirmed by reviewing the OpenAI request payload in logs.
7. For a user with `SPORT_SPECIFIC` goal and subcategory `"Soccer"`, the athletic directive injected into the prompt references the subcategory text.
8. Existing workout profiles with no `athletic_goal_category` continue to generate plans successfully; `category` field reflects standard frequency-based splits.
9. Flyway migration applies cleanly on a fresh schema and on a schema with existing `workout_profile` rows.
10. All new business logic has ≥80% unit test coverage (service/resolver layer); entity POJOs and config classes excluded from strict threshold.

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Confirm clean startup and Flyway migration applied (check logs for `Successfully applied N migrations`; confirm `athletic_goal_category` and `athletic_goal_subcategory` columns exist in H2).
3. Register a new user; create a workout profile without `athletic_goal_category` (standard 4-day) — expect 200; `GET /api/v1/workout/profile` returns both new fields as `null`.
4. Generate a plan for the 4-day user; inspect the stored JSON — confirm each of the 4 day objects has `"category"` set to `"Upper Body"` or `"Lower Body"` in alternating order.
5. Create a new user; submit workout profile with `target_goals: ["ATHLETIC_PERFORMANCE"]` and no `athletic_goal_category` — expect 400 with correct message.
6. Submit workout profile with `athletic_goal_category: "VERTICAL_EXPLOSIVENESS"` and `athletic_goal_subcategory: "Basketball"` (5-day frequency) — expect 200.
7. Generate plan for that user; inspect OpenAI request log — confirm the athletic directive block referencing "Basketball" and "vertical explosiveness" is present in the prompt.
8. Inspect the stored plan JSON — confirm day 1 `"category"` is `"Plyometrics & Explosiveness"` and exercises include plyometric movements.
9. Repeat test steps 6–8 for `SPEED_AGILITY`, `ENDURANCE`, and `SPORT_SPECIFIC` goal categories; confirm correct labels and directive blocks for each.
10. Verify existing workout profile update flow (`PUT /api/v1/workout/profile` if applicable) preserves `athletic_goal_category` when not included in the request body.

## TDD + Unit Test Coverage (required)
- Write/commit unit tests first for `DayCategoryResolver` (red → green → refactor)
- Cover all frequency values (1–6 days) for standard splits; verify exact label ordering
- Cover all four `AthleticGoalCategory` values; verify cycling behavior when frequency exceeds rotation length
- Cover `DayCategoryResolver` null/missing athletic goal: falls back to frequency-based split
- Cover `WorkoutController` or `WorkoutProfileService` validation: `ATHLETIC_PERFORMANCE` goal without `athletic_goal_category` → 400; with valid category → 200
- Cover `AthleticPromptDirectives` directive resolution: each enum value returns a non-empty, category-appropriate directive string; `SPORT_SPECIFIC` with subcategory injects subcategory text
- Target ≥80% unit test coverage for `DayCategoryResolver`, `AthleticPromptDirectives`, and the validation logic in `WorkoutProfileService` or `WorkoutController`
- Exclude from strict threshold: `AthleticGoalCategory` enum, `WorkoutProfile` entity POJO, Flyway migration SQL, `WorkoutProfileRepository` interface
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup with all Flyway migrations applied
- Verify `athletic_goal_category` and `athletic_goal_subcategory` columns exist in H2 (inspect via `/h2-console` or startup DDL log)
- Verify 400 validation: POST workout profile with `ATHLETIC_PERFORMANCE` and missing `athletic_goal_category`
- Verify 200 happy path: POST workout profile with each `AthleticGoalCategory` value; confirm DB row reflects correct values
- Verify plan generation: generate plan for a standard frequency user and for an athletic-goal user; inspect stored JSON for `"category"` field on each day
- Verify OpenAI prompt log: confirm athletic directive block is present for athletic-goal user and absent for standard user
- Document exact curl commands used

## Files Likely Affected
- `src/main/java/com/workoutplanner/model/enums/AthleticGoalCategory.java` (new enum)
- `src/main/java/com/workoutplanner/model/WorkoutProfile.java` (add `athleticGoalCategory`, `athleticGoalSubcategory` fields)
- `src/main/java/com/workoutplanner/service/DayCategoryResolver.java` (new — pure logic, no JPA)
- `src/main/java/com/workoutplanner/service/AthleticPromptDirectives.java` (new — directive template constants + resolver)
- `src/main/java/com/workoutplanner/service/OpenAIService.java` (update `buildCombinedPrompt()` — inject day category labels and athletic directive block)
- `src/main/java/com/workoutplanner/controller/WorkoutController.java` (update `POST /api/v1/workout/profile` to accept and validate new fields)
- `src/main/java/com/workoutplanner/service/WorkoutProfileService.java` (if present — add `athletic_goal_category` validation and persistence logic)
- `src/main/resources/db/migration/V{next}__add_athletic_goal_to_workout_profile.sql` (new)
- `src/test/java/com/workoutplanner/service/DayCategoryResolverTest.java` (new)
- `src/test/java/com/workoutplanner/service/AthleticPromptDirectivesTest.java` (new)
- `src/test/java/com/workoutplanner/controller/WorkoutControllerTest.java` (update — add validation coverage for new fields)

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for `POST /api/v1/workout/profile` with athletic goal; sample generated plan day object with `category` field)
- JaCoCo coverage report summary
- Runtime proof block: startup log snippet, curl commands for 400 validation case and all four `AthleticGoalCategory` happy paths, stored plan JSON snippet showing `"category"` on each day, OpenAI prompt log snippet confirming athletic directive injection

## Status
`READY`

## Notes
- `DayCategoryResolver` is intentionally kept as a pure stateless component with no JPA dependencies — this makes it trivially testable and avoids coupling the prompt-building pipeline to the persistence layer
- The `"category"` field is additive in the plan JSON; the mobile frontend reads it to display the day label under each day tab — no breaking change to existing consumers that ignore unknown fields
- `AthleticPromptDirectives` should define directive templates as constants (not database-driven) in Phase 1; moving them to coach-editable templates is a Phase 2 concern (handled in TASK-COACHING-001 via `CoachDirective`)
- The `athletic_goal_subcategory` field is free text (VARCHAR 100) deliberately — enforcing a fixed enum for subcategories (e.g., sport names) would require frequent updates as new sports are added; the prompt handles the text as-is
- Frequency-based split labels must exactly match what the mobile frontend expects for tab display — coordinate label strings with the mobile team before finalizing the `DayCategoryResolver` constants
- `ATHLETIC_PERFORMANCE` is a new valid value for the existing `target_goals` string array; no migration or backfill is needed for existing rows since the array is stored as-is
