# TASK-BE-001 — Unified Plan Contract (Workout + Diet)

## Goal
Define and enforce stable, typed response contracts for generated/current workout and diet plans so frontend parsing is deterministic.

## Priority
High

## Scope
Repo: `byb`
Area: controllers + DTOs + mapping/normalization layer

## In Scope
1. Standardize response DTOs for:
   - `POST /api/v1/workout/plan/generate`
   - `GET /api/v1/workout/plan/current`
   - `POST /api/v1/diet/plan/generate`
   - `GET /api/v1/diet/plan/current`
2. Ensure stable top-level fields (example):
   - `title`, `phaseLabel`, `durationMin`, `calories`, `exercises[]` (workout)
   - `calories`, `proteinG`, `carbsG`, `fatsG`, `mealsPerDay`, `summary` (diet)
3. Keep legacy payload field (`plan`) for compatibility during migration.
4. Add fallback mapping when generated data is sparse/unstructured.
5. Add README docs with sample payloads.

## Out of Scope
- New endpoints for weekly plan
- Billing/entitlements

## Acceptance Criteria
1. Frontend can parse responses without conditional hacks.
2. Both generate/current endpoints return consistent structure.
3. Existing clients are not broken.

## Test Steps
1. Run generate endpoints and inspect payload shape.
2. Run current endpoints and verify same schema.
3. Verify legacy `plan` field still present.

## Deliverables
- Commit hash
- Changed files
- Sample JSON responses

## Status
READY
