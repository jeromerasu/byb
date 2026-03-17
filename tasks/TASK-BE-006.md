# TASK-BE-006 — Integration - API Contract Stabilization for Mobile Frontend

## Goal
Stabilize backend endpoint contracts used by mobile frontend and provide migration-safe compatibility where route drift exists.

## Priority
High

## Scope
Repo: `byb`
Area: controller routes, DTO consistency, compatibility aliases, contract docs

## In Scope
- Ensure canonical endpoints exist and are documented for:
  - workout plan generate/current
  - diet plan generate/current
  - profile read/update surfaces used by mobile account tab
- Add compatibility aliases/redirect handling for legacy paths during migration window
- Ensure response envelopes are deterministic and frontend-safe
- Publish concise contract mapping doc for frontend team

## Out of Scope
- New feature domains (billing/progress analytics)

## Acceptance Criteria
1. Frontend can call canonical endpoints without path mismatches.
2. Legacy routes (if still used) are handled safely during migration.
3. Contract doc provides exact request/response examples.

## Test Steps
1. Hit canonical and legacy routes in test environment.
2. Validate response schema parity.
3. Verify no auth regressions on protected routes.

## Deliverables
- Commit hash
- Changed files
- Contract mapping doc

## Status
READY
