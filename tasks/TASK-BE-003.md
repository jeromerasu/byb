# TASK-BE-003 — Auth/Profile Unification for Mobile

## Goal
Unify auth/profile responses so mobile uses a single, predictable envelope and profile source of truth.

## Priority
Medium

## Scope
Repo: `byb`
Area: auth controller + profile endpoints + response DTO consistency

## In Scope
1. Consolidate duplicate auth response shapes (`/auth/*` and `/auth/mobile/*`).
2. Add/standardize profile endpoints:
   - `GET /api/v1/profile/me`
   - `PATCH /api/v1/profile/me`
   - `GET /api/v1/profile/summary`
3. Ensure refresh/login/register return consistent token+user envelope.
4. Add integration tests for login/register/refresh/me.

## Out of Scope
- Social login full implementation
- Billing role gates

## Acceptance Criteria
1. Mobile clients can use one auth/profile contract path.
2. Token refresh/login/register envelope is consistent.
3. Profile summary includes readiness flags for workout/diet flow.

## Test Steps
1. Register/login/refresh flow end-to-end.
2. Fetch profile and summary with bearer token.
3. Validate unchanged behavior for existing endpoints during migration.

## Deliverables
- Commit hash
- Changed files
- Contract examples

## Status
BACKLOG
