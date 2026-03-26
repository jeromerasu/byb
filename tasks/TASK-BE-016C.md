# TASK-BE-016C — Plan Generation Worker - Persistence to Object Storage + Week Registry Update

## Goal
Persist generated plans to object storage and update `user_week_plan` as generated source of truth.

## Priority
High

## Scope
Repo: `byb`
Area: storage integration, week registry writes

## In Scope
- Save generated workout/diet artifacts to object storage
- Add proper structured logging (SLF4J) for object-storage write lifecycle, registry upserts, and idempotency skips
- Capture and persist storage keys in `user_week_plan`
- Mark queue status `done` on success
- Enforce idempotent writes keyed by `(user_id, week_start)`

## Out of Scope
- Retry scheduling policy
- Reconciliation backfill

## Dependencies
- TASK-BE-016B

## Acceptance Criteria
1. Generated plans are stored and keys are saved in `user_week_plan`.
2. Queue rows transition to `done` only after successful persistence.
3. Duplicate writes for same user/week are prevented.
4. Structured logs capture storage writes, registry updates, and idempotent skips.

## Test Steps
1. Run worker path with valid generated payload.
2. Verify object storage keys exist and DB row updated.
3. Re-run same queue intent and verify idempotent no-dup behavior.

## Deliverables
- Commit hash
- Changed files
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
