# TASK-P1-005 — Frontend Mock vs Live Environment Setup

## Goal
Create two distinct environment configurations in `byb-frontend` — a Beta/Mock environment (fully offline, no backend required) and a Live environment (hits real Render endpoints) — with a clear toggle mechanism and documented switching process.

## Priority
High

## Scope
Repo: `byb-frontend`
Area: environment configuration, API client layer

## In Scope
- `env.beta.ts` (or equivalent): Beta/Mock environment config
  - `DEV_BYPASS_AUTH=true`
  - `DEV_USE_MOCK_PLANS=true`
  - All API calls return mock data
  - Intended for development and testing without a running backend
- `env.live.ts` (or equivalent): Live environment config
  - `DEV_BYPASS_AUTH=false`
  - `DEV_USE_MOCK_PLANS=false`
  - Base URL: `https://byb-judc.onrender.com`
  - Intended for integration testing and production use
- Central environment selector (`env.ts` or build config) that imports one of the above based on a toggle or build flag
- Toggle mechanism: environment variable, build script flag, or a single constant to flip (documented clearly)
- API path mapping layer (or fix) to reconcile frontend path mismatches with backend paths (see TASK-P1-007 for full path audit, but address the critical ones here):
  - Frontend `POST /api/v1/workout-plans/generate` → backend `POST /api/v1/plan/generate`
  - Frontend `POST /api/v1/diet-plans/generate` → backend `POST /api/v1/plan/generate`
- Documentation (inline comments or a short README section) explaining how to switch environments

## Out of Scope
- Full API path audit and adapter layer (TASK-P1-007)
- Wiring frontend to real API calls (TASK-P1-008)
- Auth token secure storage (TASK-P1-006)

## Dependencies
- None — fully independent frontend task

## Acceptance Criteria
1. `env.beta.ts` exists with `DEV_BYPASS_AUTH=true`, `DEV_USE_MOCK_PLANS=true`, and mock data sources configured.
2. `env.live.ts` exists with `DEV_BYPASS_AUTH=false`, `DEV_USE_MOCK_PLANS=false`, and `BASE_URL=https://byb-judc.onrender.com`.
3. A single change (env var, flag, or constant) switches the active environment used across the app.
4. The API mapping layer routes the plan generation calls to the correct backend path in live mode.
5. Switching instructions are documented clearly (in comments or README).
6. App builds successfully in both configurations without errors.

## Files Likely Affected
- `byb-frontend/src/config/env.beta.ts` (new)
- `byb-frontend/src/config/env.live.ts` (new)
- `byb-frontend/src/config/env.ts` (new or update — central selector)
- `byb-frontend/src/api/apiClient.ts` (or equivalent — add base URL from env config)
- `byb-frontend/src/api/planApi.ts` (or equivalent — fix generate endpoint paths)
- `byb-frontend/README.md` or inline comments (switching instructions)

## Test Steps
1. Set environment to Beta/Mock; start the app; confirm it loads without backend connection.
2. Verify mock data is returned for plan generation flows.
3. Switch to Live environment (via toggle/flag).
4. Verify `BASE_URL` resolves to `https://byb-judc.onrender.com` in API calls.
5. Verify `POST /api/v1/plan/generate` is the path used for plan generation in live mode (not the old mismatched paths).
6. Build the app in both configurations; confirm no build errors.

## Deliverables
- Commit hash
- Changed files
- Description of how to switch environments (copy-paste instructions)
- Build output confirming both configurations compile cleanly

## Status
`READY`

## Notes
- Independent of all backend P1 tasks — can be developed in parallel
- TASK-P1-008 depends on this task
- Keep the toggle mechanism simple: a single constant or `.env` file switch is preferred over complex build pipeline changes
- Mock data can reference existing mock files already in the codebase; do not create new mock data sets unless needed
