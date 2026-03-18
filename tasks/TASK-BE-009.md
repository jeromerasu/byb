# TASK-BE-009 — API - Exercise Catalog List from Generated Plan Responses

## Goal
Create a backend endpoint that returns a normalized list of exercises extracted from stored/generated workout plan responses, formatted for frontend selectors/dropdowns.

## Priority
High

## Scope
Repo: `byb`
Area: plan parsing service, controller endpoint, DTO mapping, object-storage read path

## In Scope
- Add endpoint:
  - `GET /api/v1/plan/exercises`
- Extract exercises from latest generated workout plan response (including nested week/day structures)
- Return normalized exercise list with stable IDs and display fields
- De-duplicate exercises by canonical name/slug
- Include metadata needed by frontend progress dropdowns

## Proposed Response Shape
```json
{
  "source": "current_workout_plan",
  "count": 12,
  "exercises": [
    {
      "exerciseId": "dumbbell-bench-press",
      "name": "Dumbbell Bench Press",
      "muscle": "chest",
      "defaultPrescription": {
        "sets": 4,
        "reps": "6-8",
        "weightLbs": 30
      },
      "lastSeenWeek": "week_1",
      "lastSeenDay": "day_1"
    }
  ]
}
```

## Out of Scope
- Historical analytics aggregation
- UI implementation

## Constraints
- Response must be deterministic JSON
- Endpoint auth-scoped to current user only
- Backward compatible with existing workout plan storage formats

## Acceptance Criteria
1. Endpoint returns non-empty normalized exercise list when workout plan exists.
2. Supports both legacy and new 30-day nested JSON plan structures.
3. Stable `exerciseId` generation for repeated plan loads.
4. Frontend can use response directly for dropdown options.

## Test Steps
1. Generate a workout plan.
2. Call `GET /api/v1/plan/exercises` with bearer token.
3. Verify deduped list and normalized fields.
4. Validate behavior when no plan exists (clear empty response, no 500).

## Deliverables
- Commit hash
- Changed files
- Example request/response
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
