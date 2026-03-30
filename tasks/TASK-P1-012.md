# TASK-P1-012 — Frontend Progress Screen: Wire to Real APIs

## Goal
Update the frontend progress screen to use the redesigned backend endpoints from TASK-P1-004. Replace the single `getProgressMetrics()` call with separate per-chart API functions so each chart fetches and refreshes independently.

## Priority
High

## Scope
Repo: `byb` (frontend)
Area: API layer, hooks, screens, chart components

## In Scope
- Replace single progress API call with seven separate functions
- Parallel data fetching on initial load
- Per-chart independent refresh when date range or filters change
- Exercise filter for ExerciseHistoryChart
- Date range pickers per chart section
- New chart components: MacroAdherenceChart, VolumeTrendChart, MuscleBalanceChart
- Update CalorieIntakeChart to use data from nutrition-adherence endpoint
- Mock data fallback for beta mode

## Out of Scope
- Backend changes (handled by TASK-P1-004)
- Authentication or user management changes
- Non-progress screens

## Dependencies
- **TASK-P1-004** — backend endpoints must be deployed to Render before full integration testing

---

## Changes

### 1. src/lib/api/progress.ts
Replace the single `getProgressMetrics()` function with separate functions, one per endpoint:

| Function | Endpoint |
|---|---|
| `getExerciseHistory(exercise?, from, to, token)` | `GET /api/v1/progress/exercise-history` |
| `getWorkoutHeatmap(from, to, token)` | `GET /api/v1/progress/workout-heatmap` |
| `getBodyweight(from, to, token)` | `GET /api/v1/progress/bodyweight` |
| `getNutritionAdherence(from, to, token)` | `GET /api/v1/progress/nutrition-adherence` |
| `getVolumeTrend(from, to, token)` | `GET /api/v1/progress/volume-trend` |
| `getMuscleBalance(from, to, token)` | `GET /api/v1/progress/muscle-balance` |
| `getWeeklyOverview(token)` | `GET /api/v1/progress/weekly-overview` |

Each function accepts a token and returns typed response data.

---

### 2. src/hooks/useProgressData.ts
- On initial load, call all endpoints in parallel using `Promise.all`
- Each chart can independently re-fetch when the user changes its date range or filter
- Keep mock data fallback for beta mode (when `BETA_MODE=true`)
- Expose per-chart loading and error states so charts can show their own skeletons/errors

---

### 3. ProgressScreen.tsx
- Wire all chart components to their respective data sources from `useProgressData`
- Add a date range picker per chart section (or a global date range picker that broadcasts to all)
- Add exercise name filter input for ExerciseHistoryChart
- Show the weekly-overview card at the top of the screen using `getWeeklyOverview` data

---

### 4. New Components

#### MacroAdherenceChart
- Multi-line chart showing protein, carbs, and fat as % of daily target over time
- Data source: `getNutritionAdherence` response
- Lines: protein%, carbs%, fat% — each as (consumed / target × 100)
- Reference line at 100% target

#### VolumeTrendChart
- Bar chart showing `totalVolume` per day over the selected date range
- Data source: `getVolumeTrend` response
- Secondary axis or tooltip showing `totalSets` and `totalReps`

#### MuscleBalanceChart
- Radar/spider chart showing muscle group distribution
- Data source: `getMuscleBalance` response
- Axes: one per muscle group, value = `totalSets` or `totalVolume` (user-selectable)

---

### 5. CalorieIntakeChart Update
- Migrate data source from old `getProgressMetrics` to `getNutritionAdherence`
- Display `caloriesConsumed` vs `calorieTarget` per day
- Optionally surface `adherenceScore` as a tooltip or annotation

---

## Acceptance Criteria
1. All seven API functions exist in `progress.ts` with correct endpoint URLs and typed return values.
2. Initial progress screen load fires all seven requests in parallel.
3. Changing a chart's date range re-fetches only that chart's data.
4. Exercise filter on ExerciseHistoryChart re-fetches with the updated `exercise` param.
5. Beta mode mock fallback still works when real API is unavailable.
6. MacroAdherenceChart, VolumeTrendChart, and MuscleBalanceChart render with real data from Render.
7. CalorieIntakeChart uses the nutrition-adherence endpoint.
8. All charts display appropriate loading and empty states.

## Testing
- Verify all charts load with real data from Render (after TASK-P1-004 deploy)
- Verify parallel fetch — confirm network tab shows all requests firing simultaneously on screen load
- Verify per-chart refresh — changing date range on one chart does not reload others
- Verify beta mode fallback still works

## Status
`BACKLOG`

## Notes
- Do not start until TASK-P1-004 backend is deployed to Render
- Mock data shapes must match the new API response shapes so the fallback remains useful
- Radar chart library support: confirm chosen charting library supports radar/spider charts before building MuscleBalanceChart; if not, use a horizontal bar chart as fallback
