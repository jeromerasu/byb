# TASK-P1-004 — Progress Aggregation Endpoints

## Goal
Provide separate, independently filterable progress endpoints — one per chart — so each frontend component can fetch and refresh its own data with its own date range and filters.

## Priority
High

## Scope
Repo: `byb`
Area: aggregation queries, service layer, controller
Implements: TASK-API-004

## In Scope
- `ProgressController` with seven endpoints (detailed below)
- `ProgressService` implementing all aggregation logic
- Response DTOs per endpoint
- Structured SLF4J logging
- All endpoints return `200` with empty arrays/zero values when no data exists — never `500`
- Unit tests for each aggregation method (≥80% coverage on business logic)
- Integration tests with seeded test data
- Render deploy verification

## Out of Scope
- Push notifications or goal alerts
- Historical plan comparison
- Leaderboards or social features

## Dependencies
- **TASK-P1-001** (WorkoutLog) — done
- **TASK-P1-002** (MealLog) — done
- **TASK-P1-003** (BodyMetrics) — done

---

## Endpoints

### 1. GET /api/v1/progress/exercise-history
**Query params**: `exercise` (optional), `from` (date), `to` (date)
**Powers**: ExerciseHistoryChart

Returns an array of individual workout log entries for one or all exercises. `isPersonalRecord` is `true` when this entry represents the highest weight at this rep count for this exercise (all-time, not just the requested range).

```
GET /api/v1/progress/exercise-history?exercise=Bench+Press&from=2026-01-01&to=2026-03-30
```

**Response**:
```json
[
  {
    "exerciseName": "Bench Press",
    "date": "2026-03-15",
    "sets": 4,
    "reps": 8,
    "weight": 100.0,
    "unit": "kg",
    "isPersonalRecord": true
  }
]
```

If `exercise` is omitted, returns entries for all exercises.

---

### 2. GET /api/v1/progress/workout-heatmap
**Query params**: `from` (date), `to` (date)
**Powers**: WorkoutHeatmap

Returns one entry per day in the requested range, aggregated from WorkoutLog.

- `totalVolume` = sum of (sets × reps × weight) across all entries for that day
- Intensity is derived from the data — not stored — and may be computed by the frontend:
  - light: < 30 min or < 5 sets
  - moderate: 30–60 min or 5–15 sets
  - high: > 60 min or > 15 sets

```
GET /api/v1/progress/workout-heatmap?from=2026-01-01&to=2026-03-30
```

**Response**:
```json
[
  {
    "date": "2026-03-15",
    "workoutCount": 1,
    "totalSets": 12,
    "totalDuration": 55,
    "totalVolume": 4800.0
  }
]
```

---

### 3. GET /api/v1/progress/bodyweight
**Query params**: `from` (date), `to` (date)
**Powers**: BodyweightChart

Alias or thin wrapper over the existing `GET /api/v1/progress/body-metrics` endpoint. Returns body weight entries filtered by date range.

```
GET /api/v1/progress/bodyweight?from=2026-01-01&to=2026-03-30
```

**Response**:
```json
[
  { "date": "2026-03-01", "weight": 84.0, "unit": "kg" },
  { "date": "2026-03-15", "weight": 83.2, "unit": "kg" }
]
```

---

### 4. GET /api/v1/progress/nutrition-adherence
**Query params**: `from` (date), `to` (date)
**Powers**: CalorieIntakeChart + MacroAdherenceChart

Returns one entry per day with consumed values aggregated from MealLog and targets pulled from the user's DietProfile (`dailyCalorieGoal`, `proteinGoalGrams`, `carbGoalGrams`, `fatGoalGrams`).

`adherenceScore` = average of (calories%, protein%, carbs%, fat% of target), each capped at 100% before averaging.

```
GET /api/v1/progress/nutrition-adherence?from=2026-03-01&to=2026-03-30
```

**Response**:
```json
[
  {
    "date": "2026-03-15",
    "caloriesConsumed": 2100,
    "calorieTarget": 2400,
    "proteinConsumed": 155,
    "proteinTarget": 180,
    "carbsConsumed": 220,
    "carbsTarget": 250,
    "fatConsumed": 65,
    "fatTarget": 70,
    "adherenceScore": 88.5
  }
]
```

---

### 5. GET /api/v1/progress/volume-trend
**Query params**: `from` (date), `to` (date)
**Powers**: VolumeTrendChart (progressive overload tracking)

Returns one entry per day aggregated from WorkoutLog.

```
GET /api/v1/progress/volume-trend?from=2026-03-01&to=2026-03-30
```

**Response**:
```json
[
  {
    "date": "2026-03-15",
    "totalVolume": 4800.0,
    "totalSets": 20,
    "totalReps": 160
  }
]
```

---

### 6. GET /api/v1/progress/muscle-balance
**Query params**: `from` (date), `to` (date)
**Powers**: MuscleBalanceChart (radar/spider chart)

Returns one entry per muscle group, aggregated from WorkoutLog joined to ExerciseCatalog. Only works for logged exercises linked to catalog entries; unlinked entries are excluded.

```
GET /api/v1/progress/muscle-balance?from=2026-03-01&to=2026-03-30
```

**Response**:
```json
[
  { "muscleGroup": "Chest", "workoutCount": 8, "totalSets": 32, "totalVolume": 9600.0 },
  { "muscleGroup": "Back", "workoutCount": 6, "totalSets": 24, "totalVolume": 7200.0 }
]
```

---

### 7. GET /api/v1/progress/weekly-overview
**Query params**: none
**Powers**: Weekly overview summary card

Lightweight summary covering the last 7 days and the current state of the user's account.

- `workoutsPlanned` comes from `WorkoutProfile.workoutFrequency`
- `activeStreak` = consecutive days with at least one WorkoutLog entry up to and including today
- `nutritionAdherence` = average `adherenceScore` over the last 7 days

```
GET /api/v1/progress/weekly-overview
```

**Response**:
```json
{
  "workoutsCompleted": 4,
  "workoutsPlanned": 5,
  "consistencyScore": 80.0,
  "activeStreak": 3,
  "nutritionAdherence": 85.5,
  "currentWeight": 83.2,
  "weightChange7d": -0.8
}
```

---

## Files Likely Affected
- `src/main/java/.../controller/ProgressController.java` (new)
- `src/main/java/.../service/ProgressService.java` (new)
- `src/main/java/.../repository/WorkoutLogRepository.java` (add aggregation queries)
- `src/main/java/.../repository/MealLogRepository.java` (add daily macro aggregation)
- `src/main/java/.../repository/BodyMetricsRepository.java` (reuse/extend)
- `src/main/java/.../dto/ExerciseHistoryResponse.java` (new)
- `src/main/java/.../dto/WorkoutHeatmapResponse.java` (new)
- `src/main/java/.../dto/BodyweightResponse.java` (new)
- `src/main/java/.../dto/NutritionAdherenceResponse.java` (new)
- `src/main/java/.../dto/VolumeTrendResponse.java` (new)
- `src/main/java/.../dto/MuscleBalanceResponse.java` (new)
- `src/main/java/.../dto/WeeklyOverviewResponse.java` (new)
- `src/test/java/.../service/ProgressServiceTest.java` (new)

## Acceptance Criteria
1. Each endpoint returns correctly aggregated data scoped strictly to the authenticated user.
2. `isPersonalRecord` in exercise-history is `true` only for the all-time highest weight at that rep count for that exercise.
3. `adherenceScore` in nutrition-adherence is capped at 100% per macro before averaging.
4. `weekly-overview` streak resets when there is a gap day with no workout log.
5. All endpoints return `200` with empty arrays or zero values when no data exists — never `500`.
6. All endpoints verified reachable on Render after deploy.

## TDD + Unit Test Coverage (required)
- Write service unit tests first (red → green → refactor)
- Cover: each aggregation with data and empty state
- Cover: `isPersonalRecord` — max weight per rep count, not just global max
- Cover: `adherenceScore` — correct capping and averaging logic
- Cover: streak calculation — consecutive days, gap breaks streak, no data returns zero
- Cover: `weightChange7d` — correct delta over last 7 days
- Target ≥80% unit test coverage for `ProgressService`
- Exclude from strict threshold: DTOs, repository interface methods
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Verify all seven endpoints reachable with seeded data
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
- Each endpoint is designed to be called independently by a specific frontend chart component
- `muscle-balance` depends on exercises being linked to ExerciseCatalog entries; unlinked entries are silently excluded
- `bodyweight` may be a simple alias over the existing body-metrics endpoint — avoid duplicating logic
- Do not start until TASK-P1-001, P1-002, and P1-003 are all DONE (they are)
