# TASK-BE-016A — Plan Generation Worker - Queue Claiming, Locking, and Batching

## Goal
Build the worker foundation to claim queued generation items safely, process in batches, and avoid duplicate concurrent processing.

## Priority
High

## Scope
Repo: `byb`
Area: worker service, queue status transitions, batch controls

## In Scope
- Poll queue (`pending`, `next_retry_at <= now`)
- Add proper structured logging (SLF4J) for batch polling, claim outcomes, lock contention, and runtime budget exits
- Claim rows atomically (`pending -> processing`)
- Batch size controls and runtime budget guards
- Persist processing metadata (`started_at`, worker marker)
- Add worker trigger endpoint for testing execution (e.g. `POST /internal/jobs/plan-rollover-worker/run-once`)
- Add environment-gated public testing mode for worker trigger endpoint:
  - public in `test/beta` when `jobs.internal.auth.required=false`
  - auth required in `prod` (default)

## Out of Scope
- OpenAI generation logic
- Object storage write logic

## Dependencies
- TASK-BE-014, TASK-BE-015

## Acceptance Criteria
1. Worker claims queue rows without double-processing.
2. Batch and timeout guards prevent long-run timeouts.
3. Queue state transitions are auditable.
4. Worker trigger endpoint supports env-gated public testing mode and remains protected in prod.
5. Structured logs capture claims, batch limits, contention outcomes, and auth mode state.

## Test Steps
1. Seed pending queue rows.
2. Run worker and verify atomic claim behavior.
3. Simulate parallel worker run and verify no duplicate claims.

## Deliverables
- Commit hash
- Changed files
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
