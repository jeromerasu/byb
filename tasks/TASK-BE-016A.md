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
- Claim rows atomically (`pending -> processing`)
- Batch size controls and runtime budget guards
- Persist processing metadata (`started_at`, worker marker)

## Out of Scope
- OpenAI generation logic
- Object storage write logic

## Dependencies
- TASK-BE-014, TASK-BE-015

## Acceptance Criteria
1. Worker claims queue rows without double-processing.
2. Batch and timeout guards prevent long-run timeouts.
3. Queue state transitions are auditable.

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
