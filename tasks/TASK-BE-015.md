# TASK-BE-015 — Plan Generation Pipeline - Daily Scan Job (Subscription + Missing Next Week)

## Goal
Implement a daily scan that enqueues generation work for users with valid subscriptions whose next week has not been created.

## Priority
High

## Scope
Repo: `byb`
Area: internal job endpoint, scan service, queue upsert logic

## In Scope
- Add internal endpoint:
  - `POST /internal/jobs/plan-rollover-scan`
- Add proper structured logging (SLF4J) for scan start/end, eligibility decisions, enqueue results, and auth failures
- Secure endpoint with internal token check
- Scan active subscribed users only
- Compute user `nextWeekStart` (timezone-safe strategy)
- If `(user, nextWeekStart)` missing in `user_week_plan`, enqueue queue item
- Idempotent enqueue via unique key/upsert
- Return run summary counts

## Out of Scope
- Actual plan generation execution
- Notification/reminder flows

## Dependencies
- TASK-BE-014

## Acceptance Criteria
1. Scan only enqueues users with active subscription + missing next week.
2. Re-running scan does not duplicate queue records.
3. Internal endpoint is protected and returns deterministic summary JSON.
4. Structured logs capture scan lifecycle and enqueue decisions.

## Test Steps
1. Seed users with mixed subscription states.
2. Run scan endpoint and verify queue inserts.
3. Re-run and confirm no duplicate queue growth.
4. Validate unauthorized access is denied.

## Deliverables
- Commit hash
- Changed files
- Request/response examples
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
