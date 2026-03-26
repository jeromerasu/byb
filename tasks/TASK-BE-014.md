# TASK-BE-014 — Plan Generation Pipeline - DB Foundation (Week Registry + Queue)

## Goal
Create the database foundation for subscription-gated weekly plan rollover generation: a week-plan registry and a generation queue with idempotency guarantees.

## Priority
High

## Scope
Repo: `byb`
Area: migrations, entities/repositories, minimal service wiring

## In Scope
- Add `user_week_plan` table (source of truth for generated weeks)
- Add `plan_generation_queue` table (scan->worker handoff)
- Add proper structured logging (SLF4J) for schema init checks and repository-level pipeline operations
- Add constraints/indexes:
  - `UNIQUE(user_id, week_start)` on `user_week_plan`
  - `UNIQUE(user_id, week_start, plan_type)` on queue
  - indexes for pending status + retry windows
- Add status enums and minimal repository methods
- Add migration scripts + rollback notes

## Out of Scope
- Scan job implementation
- Generation worker implementation

## Acceptance Criteria
1. Migrations create both tables with required constraints/indexes.
2. Duplicate enqueue and duplicate week-plan creation are prevented by DB constraints.
3. Repositories support core reads/writes needed by scan/worker stages.
4. Structured logs are emitted for key DB foundation operations.

## Test Steps
1. Run app with test profile and apply migrations.
2. Verify schema/constraints via SQL checks.
3. Attempt duplicate inserts and confirm conflict prevention.

## Deliverables
- Commit hash
- Changed files
- Migration SQL
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
