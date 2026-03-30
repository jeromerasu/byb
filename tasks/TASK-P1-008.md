# TASK-P1-008 — Wire Frontend to Real Backend APIs

## Goal
Replace all mock data usage in `byb-frontend` with real API calls when running in live mode, connecting every feature (plans, logging, metrics, catalogs) to the live Render backend, with proper error handling and loading states throughout.

## Priority
High

## Scope
Repo: `byb-frontend`
Area: all data-fetching screens and services

## In Scope
Replace mock data with real API calls for all of the following features in live mode:

| Feature | Endpoints |
|---|---|
| Plan generation | `POST /api/v1/plan/generate` |
| Current week plan | `GET /api/v1/plan/current-week` |
| Workout logging | `POST /api/v1/workout/logs`, `GET /api/v1/workout/logs`, `PUT/DELETE /api/v1/workout/logs/{id}` |
| Meal logging | `POST /api/v1/diet/logs`, `GET /api/v1/diet/logs`, `PUT/DELETE /api/v1/diet/logs/{id}` |
| Body metrics | `POST /api/v1/progress/body-metrics`, `GET /api/v1/progress/body-metrics`, `GET /api/v1/progress/body-metrics/latest` |
| Progress tracking | `GET /api/v1/progress/metrics`, `GET /api/v1/progress/personal-records`, `GET /api/v1/progress/summary` |
| Exercise catalog | `GET /api/v1/exercises` |
| Food catalog | `GET /api/v1/foods` |

- In **beta/mock mode**: continue using mock data (no API calls made)
- In **live mode**: use real API calls via the adapter from TASK-P1-007
- Error handling for all API calls:
  - Network errors: show user-friendly error message
  - `401 Unauthorized`: clear token and redirect to login (integrate with TASK-P1-006 auth flow)
  - `403 Forbidden`: show appropriate message (e.g., "Upgrade to Premium" for plan generation gating)
  - `500` / other server errors: show generic error with retry option
- Loading states for all data fetches: show spinner/skeleton while request is in flight
- Verify end-to-end with live Render endpoints

## Out of Scope
- New backend endpoints (covered by TASK-P1-001 through TASK-P1-004)
- RevenueCat in-app purchase flow
- Push notifications

## Dependencies
- **TASK-P1-005** — environment config must exist (mock vs live toggle)
- **TASK-P1-006** — secure token storage must be in place (401 handling needs to clear stored token)
- **TASK-P1-007** — correct API paths must be in place
- **Backend TASK-P1-001** — workout logs endpoint must be live on Render
- **Backend TASK-P1-002** — meal logs endpoint must be live on Render
- **Backend TASK-P1-003** — body metrics endpoint must be live on Render
- **Backend TASK-P1-004** — progress aggregation endpoints must be live on Render

## Acceptance Criteria
1. In live mode, no mock data is used for any feature listed in scope — all data comes from API calls to Render.
2. In beta/mock mode, no API calls are made — all data comes from mock sources.
3. Every data-fetching screen shows a loading indicator while the request is in flight.
4. Network errors display a user-friendly message (no unhandled promise rejections / blank screens).
5. `401` responses clear the stored token and redirect to the login screen.
6. `403` responses from plan generation show an appropriate upgrade prompt.
7. End-to-end flow verified: register → generate plan → log workout → log meal → record body weight → view progress summary — all against live Render backend.

## Files Likely Affected
- All screen components that currently use mock data
- `byb-frontend/src/api/` — service files for each feature domain
- `byb-frontend/src/hooks/` — data-fetching hooks (if used)
- `byb-frontend/src/context/` — any context providers holding fetched data
- Error boundary or global error handler (if one exists)

## Test Steps
1. Set environment to Live (from TASK-P1-005 toggle).
2. Register a new user on `https://byb-judc.onrender.com`; verify JWT obtained and stored securely.
3. Complete workout profile and diet profile setup screens; verify API calls succeed.
4. Trigger plan generation; verify `POST /api/v1/plan/generate` is called and plan is displayed.
5. Log a workout entry; verify `POST /api/v1/workout/logs` is called and entry appears in log list.
6. Log a meal entry; verify `POST /api/v1/diet/logs` is called and entry appears.
7. Record body weight; verify `POST /api/v1/progress/body-metrics` is called.
8. View progress screen; verify `GET /api/v1/progress/summary` is called and data is displayed.
9. Disconnect network; verify error messages appear (no blank screens or crashes).
10. Force a `401` (use expired token); verify redirect to login.
11. Switch to Beta/Mock mode; verify no network requests are made and mock data is shown.

## Deliverables
- Commit hash
- Changed files
- End-to-end test evidence: screen recordings or curl traces showing real API calls in live mode
- Error handling screenshots (network error, 401 redirect)
- Confirmation that mock mode still works independently

## Status
`BACKLOG`

## Notes
- Must not be started until TASK-P1-005, P1-006, P1-007 are DONE, and backend P1-001 through P1-004 are deployed to Render
- The 401 interceptor should be implemented at the API client level (not per-screen) so it applies uniformly
- Consider using React Query or a similar library if not already in use, to simplify loading/error state management
