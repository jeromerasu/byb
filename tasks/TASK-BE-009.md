# TASK-BE-009 — API - Current Week Workout + Diet Plan (User Scoped)

## Goal
Create a backend endpoint that returns the authenticated user’s current week slice for both workout and diet plans in one deterministic JSON response, and include normalized lists needed by frontend selectors.

## Priority
High

## Scope
Repo: `byb`
Area: plan parsing service, controller endpoint, DTO mapping, object-storage read path

## In Scope
- Add endpoint:
  - `GET /api/v1/plan/current-week`
- Resolve current week from generated plan artifacts for the authenticated user
- Return both:
  - `workoutWeek` (current week days/exercises)
  - `dietWeek` (current week days/meals/foods)
- Include normalized selector payloads derived from the same response:
  - `exerciseCatalog[]`
  - `dietFoodCatalog[]`
- Support legacy and new 30-day nested plan shapes
- Auth-scope response strictly to current user

## Proposed Response Shape
```json
{
  "source": "current_combined_plan",
  "weekIndex": 1,
  "weekKey": "week_1",
  "workoutWeek": {
    "done": false,
    "days": {
      "day_1": {
        "done": false,
        "focus": "upper_body",
        "exercises": [
          {
            "name": "Dumbbell Bench Press",
            "sets": 4,
            "reps": "6-8",
            "weightLbs": 30
          }
        ]
      }
    }
  },
  "dietWeek": {
    "done": false,
    "days": {
      "day_1": {
        "done": false,
        "meals": [
          {
            "name": "Greek Yogurt",
            "mealType": "breakfast",
            "calories": 180,
            "proteinGrams": 20
          }
        ]
      }
    }
  },
  "exerciseCatalog": [
    {
      "exerciseId": "dumbbell-bench-press",
      "name": "Dumbbell Bench Press"
    }
  ],
  "dietFoodCatalog": [
    {
      "foodId": "greek-yogurt",
      "name": "Greek Yogurt",
      "mealType": "breakfast"
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
- Backward compatible with existing workout/diet plan storage formats

## Acceptance Criteria
1. Endpoint returns current week workout + diet for authenticated user when plans exist.
2. Supports both legacy and new 30-day nested JSON plan structures.
3. Week objects and day objects include `done` boolean for workout and diet.
4. Includes normalized selector catalogs for exercise and diet food.
5. Clear empty-state response when plans are missing (no 500s).

## Test Steps
1. Generate combined workout+diet plan.
2. Call `GET /api/v1/plan/current-week` with bearer token.
3. Verify week resolution and both payloads are present.
4. Verify selector catalogs are deduped and stable.
5. Validate no-plan behavior (empty/clear response, no 500).

## Deliverables
- Commit hash
- Changed files
- Example request/response
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
