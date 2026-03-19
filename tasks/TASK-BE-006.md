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
- Enforce structured JSON generation from AI for 30-day plans (no freeform text payloads)
- Require 30-day schema shape for workout plan output, e.g.:
  - `weeks.week_1.day_1.exercises[]` ... through week_4/day_7
  - each week object includes `done` boolean
  - each day includes `done` boolean to track completion state
  - exercise entries include `name`, `sets`, `reps`, `weight_lbs` (or canonical weight field), and optional rest metadata
- Require object-storage persisted response format to preserve week/day hierarchy and completion state (`done`) for direct frontend consumption
- Add validation/repair path: if AI output is non-JSON or schema-invalid, regenerate or fallback to safe template

## Out of Scope
- New feature domains (billing/progress analytics)

## Acceptance Criteria
1. Frontend can call canonical endpoints without path mismatches.
2. Legacy routes (if still used) are handled safely during migration.
3. Contract doc provides exact request/response examples.
4. Workout plan generation returns strict JSON following 30-day schema (week/day nesting with exercises array).
5. Non-JSON or schema-invalid AI output is rejected/recovered automatically (no malformed payloads returned to client).

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
