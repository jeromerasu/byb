# TASK-BE-010 — API - Diet Food Catalog List from Generated Plan Responses

## Goal
Create a backend endpoint that returns a normalized list of foods/meals extracted from stored/generated diet plan responses, formatted for frontend selectors and diet progress views.

## Priority
High

## Scope
Repo: `byb`
Area: diet plan parsing service, controller endpoint, DTO mapping, object-storage read path

## In Scope
- Add endpoint:
  - `GET /api/v1/plan/diet-foods`
- Extract diet items from latest generated diet plan response (supports nested week/day/meal structures)
- Return normalized list with stable IDs and display fields
- De-duplicate foods by canonical name/slug
- Include metadata needed by frontend diet filters/dropdowns

## Proposed Response Shape
```json
{
  "source": "current_diet_plan",
  "count": 18,
  "foods": [
    {
      "foodId": "greek-yogurt",
      "name": "Greek Yogurt",
      "mealType": "breakfast",
      "calories": 180,
      "proteinGrams": 20,
      "carbsGrams": 8,
      "fatGrams": 4,
      "lastSeenWeek": "week_1",
      "lastSeenDay": "day_1"
    }
  ]
}
```

## Out of Scope
- Nutrition recommendation engine changes
- UI implementation

## Constraints
- Response must be deterministic JSON
- Endpoint auth-scoped to current user only
- Backward compatible with existing diet plan storage formats

## Acceptance Criteria
1. Endpoint returns normalized food list when diet plan exists.
2. Supports legacy and new 30-day nested JSON diet structures.
3. Stable `foodId` generation for repeated loads.
4. Frontend can use response directly for selectors/filters.

## Test Steps
1. Generate a diet plan.
2. Call `GET /api/v1/plan/diet-foods` with bearer token.
3. Verify deduped list and normalized fields.
4. Validate no-plan behavior (clear empty response, no 500).

## Deliverables
- Commit hash
- Changed files
- Example request/response
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
