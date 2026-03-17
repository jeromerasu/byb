# TASK-BE-008 — Integration - Combined Workout + Diet Plan Generate API

## Goal
Provide a single backend endpoint that generates and returns both workout and diet plans in one response envelope for mobile onboarding.

## Priority
High

## Scope
Repo: `byb`
Area: plan orchestration endpoint, DTO composition, storage updates, compatibility behavior

## In Scope
- Add combined endpoint:
  - `POST /api/v1/plan/generate`
- Endpoint orchestrates workout + diet generation in one request cycle
- Return one deterministic JSON envelope containing:
  - `plan_meta` (duration, generated_at, version)
  - `workout` plan payload
  - `diet` plan payload
- Persist response artifacts to object storage under required prefix format:
  - `plans/{userGuid}/{yyyy-MM}/...`
  - where `userGuid` is authenticated user id/guid
  - where `yyyy-MM` is month-year partition for generated date
- Store combined artifact + optional split artifacts (workout/diet) and return storage keys in response metadata
- Keep existing separate endpoints operational for backward compatibility
- Ensure auth, error handling, and partial-failure strategy are defined

## Out of Scope
- Frontend UI redesign
- Subscription/billing logic

## Constraints
- Preserve compatibility for `/api/v1/workout/plan/generate` and `/api/v1/diet/plan/generate`
- Response must be strict JSON and schema-valid

## Acceptance Criteria
1. Combined endpoint returns workout + diet in one JSON response.
2. Existing separate generate endpoints continue to work.
3. Generated artifacts are stored under `plans/{userGuid}/{yyyy-MM}/...` object-storage pathing.
4. Response includes storage key metadata for persisted artifacts.
5. Storage metadata for both plan types is updated consistently.
6. Clear error semantics for partial generation failures.

## Test Steps
1. Call combined generate endpoint for authenticated user.
2. Validate response schema and presence of both plans.
3. Validate plan persistence and current-plan retrieval.
4. Verify legacy endpoints still pass regression checks.

## Deliverables
- Commit hash
- Changed files
- Request/response examples
- Backward compatibility notes

## Status
READY
