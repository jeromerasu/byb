# TASK-BE-015 — Plan Generation Pipeline - Daily Scan Job (Subscription + Missing Next Week)

## Goal
Implement a daily scan that enqueues generation work for users with valid subscriptions whose next week has not been created.

## Priority
High

## Scope
Repo: `byb`
Area: internal job endpoint, scan service, queue upsert logic

## In Scope
- Add endpoint for scan stage:
  - `POST /internal/jobs/plan-rollover-scan`
- During current testing phase, keep endpoint open by default and add clear TODO comments in controller/config:
  - `TODO(PROD-HARDEN): re-enable internal auth token before production cutover`
  - include target date/owner note in code comment
- Keep auth toggle scaffolding in place for later hardening (`jobs.internal.auth.required`)
- Add proper structured logging (SLF4J) for scan start/end, eligibility decisions, enqueue results, auth failures, and auth-bypass mode warnings
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
3. Endpoint is open for testing phase and includes explicit `TODO(PROD-HARDEN)` comments for auth re-enable before production.
4. Endpoint returns deterministic summary JSON.
5. Structured logs capture scan lifecycle, enqueue decisions, and current auth mode state.

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
