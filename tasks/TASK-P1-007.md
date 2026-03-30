# TASK-P1-007 — Fix Frontend API Path Mismatches

## Goal
Audit and fix all API path mismatches between the frontend client and the backend, creating a centralized API adapter layer so every frontend call maps to the correct backend endpoint.

## Priority
High

## Scope
Repo: `byb-frontend`
Area: API client layer, endpoint mapping

## In Scope
Fix all known path mismatches:

| Frontend (current) | Backend (correct) | Notes |
|---|---|---|
| `POST /api/v1/workout-plans/generate` | `POST /api/v1/plan/generate` | Combined plan generation |
| `POST /api/v1/diet-plans/generate` | `POST /api/v1/plan/generate` | Same endpoint, combined |
| `POST /api/v1/progress/metrics` | `GET /api/v1/progress/metrics` | Wrong method + path (needs TASK-P1-004) |
| `POST /api/v1/progress/log-session` | `POST /api/v1/workout/logs` | Maps to workout log create (needs TASK-P1-001) |
| `POST /api/v1/progress/log-bodyweight` | `POST /api/v1/progress/body-metrics` | Maps to body metrics create (needs TASK-P1-003) |
| `POST /api/v1/exercises/catalog` | `GET /api/v1/exercises` | Wrong method + path |

- Create or update a central API adapter/client layer (e.g., `apiClient.ts`, `api/index.ts`) that:
  - Defines all backend endpoint paths as named constants
  - Routes each frontend call to the correct backend path and HTTP method
  - Uses the base URL from the environment config (from TASK-P1-005)
- All API calls in the frontend go through this adapter — no hard-coded paths scattered across screens/components
- Document the full mapping in comments within the adapter file

## Out of Scope
- Environment switching setup (TASK-P1-005)
- Auth token storage (TASK-P1-006)
- Implementing the missing backend endpoints (TASK-P1-001 through TASK-P1-004)
- Replacing mock data with real calls (TASK-P1-008)

## Dependencies
- **TASK-P1-005** recommended first (provides environment config), but the path fixes can be done independently

## Acceptance Criteria
1. No frontend file contains the stale paths (`/workout-plans/generate`, `/diet-plans/generate`, `/progress/log-session`, `/progress/log-bodyweight`, `/exercises/catalog` with POST).
2. A central adapter layer defines all endpoint paths as named constants.
3. All plan generation calls route to `POST /api/v1/plan/generate`.
4. All workout log calls route to `POST /api/v1/workout/logs`.
5. All body metrics calls route to `POST /api/v1/progress/body-metrics`.
6. Exercise catalog call uses `GET /api/v1/exercises`.
7. App builds without errors after changes.

## Files Likely Affected
- `byb-frontend/src/api/apiClient.ts` (new or refactor — central adapter)
- `byb-frontend/src/api/endpoints.ts` (new — endpoint path constants)
- Any screen or service file containing hard-coded API paths
- Plan generation screen/service
- Progress screen/service
- Exercise catalog screen/service

## Test Steps
1. Search entire frontend codebase for old path strings (`workout-plans/generate`, `diet-plans/generate`, `log-session`, `log-bodyweight`, `exercises/catalog`); confirm zero occurrences after fix.
2. Inspect network requests in the app (or trace through adapter code) to confirm correct paths are assembled for each call type.
3. In live environment config (from TASK-P1-005), verify plan generation call targets `POST /api/v1/plan/generate`.
4. Build the app; confirm no errors.

## Deliverables
- Commit hash
- Changed files
- Full mapping table in the adapter file (old path → new path, with HTTP method)
- Build output confirming clean compile

## Status
`READY`

## Notes
- TASK-P1-008 depends on this task
- Progress metrics endpoint (`GET /api/v1/progress/metrics`) and summary endpoint will not have a live backend until TASK-P1-004 is done — map the path correctly now and the calls will work once the backend is live
- Keep all endpoint path strings in one file to make future updates easy
