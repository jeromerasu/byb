# TASK-BE-016D — Plan Generation Worker - Retry, Backoff, and Failure Policy

## Goal
Implement robust retry/backoff behavior for failed queue items without blocking healthy throughput.

## Priority
High

## Scope
Repo: `byb`
Area: queue retry policy, failure classification, terminal state handling

## In Scope
- Retry transient failures with backoff (e.g., 1m/5m/15m)
- Add proper structured logging (SLF4J) for retry scheduling decisions, attempt counts, and terminal failure transitions
- Track attempts and `next_retry_at`
- Move permanent failures to terminal `failed`
- Store `last_error` and classification metadata

## Out of Scope
- Alerting integrations
- Reconciliation repair pass

## Dependencies
- TASK-BE-016C

## Acceptance Criteria
1. Transient failures are retried automatically with backoff.
2. Permanent failures are marked terminal with reason.
3. Retry behavior is deterministic and auditable in queue rows.
4. Structured logs capture retry scheduling and terminal failure transitions.

## Test Steps
1. Simulate transient failure and verify scheduled retry progression.
2. Simulate permanent failure and verify terminal failed state.
3. Ensure successful retries transition to `done` cleanly.

## Deliverables
- Commit hash
- Changed files
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
