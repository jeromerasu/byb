# BACKEND TASK QUEUE

## Workflow Rules
1. Pick first task with status `READY`.
2. Set to `IN_PROGRESS` before coding.
3. Keep scope tight and contract-safe.
4. Commit with task ID in message.
5. Update task with results.
6. Set status to `DONE` or `BLOCKED`.

## Queue
- [ ] TASK-BE-001 — Unified Plan Contract (Workout + Diet) — **AWAITING_APPROVAL**
- [ ] TASK-BE-002 — Storage Metadata Integrity + Lookup APIs — **AWAITING_APPROVAL**
- [ ] TASK-BE-004 — Payment Foundation (RevenueCat Entitlements) — **AWAITING_APPROVAL**
- [ ] TASK-BE-005 — Test Structured Storage Implementation — **AWAITING_APPROVAL**
- [ ] TASK-BE-006 — Integration - API Contract Stabilization for Mobile Frontend — **AWAITING_APPROVAL**
- [ ] TASK-BE-007 — Integration - Real-Time Metrics Updates + Nightly Snapshot Rebuild — **READY**
- [ ] TASK-BE-008 — Integration - Combined Workout + Diet Plan Generate API — **AWAITING_APPROVAL**
- [ ] TASK-BE-009 — API - Current Week Workout + Diet Plan (User Scoped) — **READY**
- [ ] TASK-BE-010 — API - Diet Food Catalog List from Generated Plan Responses — **READY**
- [ ] TASK-BE-011 — API - Persist First-Time Tutorial Completion State — **READY**
- [ ] TASK-BE-012 — API/DB - Persist User Theme Preferences (Colorway/Accent) — **READY**
- [ ] TASK-BE-014 — Plan Generation Pipeline - DB Foundation (Week Registry + Queue) — **READY**
- [ ] TASK-BE-015 — Plan Generation Pipeline - Daily Scan Job (Subscription + Missing Next Week) — **READY**
- [ ] TASK-BE-016A — Plan Generation Worker - Queue Claiming, Locking, and Batching — **READY**
- [ ] TASK-BE-016B — Plan Generation Worker - Workout+Diet Generation Execution Path — **READY**
- [ ] TASK-BE-016C — Plan Generation Worker - Persistence to Object Storage + Week Registry Update — **READY**
- [ ] TASK-BE-016D — Plan Generation Worker - Retry, Backoff, and Failure Policy — **READY**
- [ ] TASK-BE-016E — Plan Generation Worker - Operational Controls, Metrics, and Cleanup — **READY**
- [ ] TASK-BE-003 — Auth/Profile Unification for Mobile — **BACKLOG**

## Data Tasks (parallel, independent of TASK-API-xxx series)
- [ ] TASK-DATA-001 — Exercise Catalog (system + custom entries, WorkoutLog FK) — **READY**
- [ ] TASK-DATA-002 — Food Catalog (system + custom entries, MealLog FK) — **READY**

## P1 Tasks — Core Feature Gaps (priority implementation wave)

### Backend P1 (parallel group 1 — no interdependencies)
- [ ] TASK-P1-001 — WorkoutLog REST Layer (CRUD endpoints) — **READY**
- [ ] TASK-P1-002 — MealLog REST Layer (CRUD endpoints) — **READY**
- [ ] TASK-P1-003 — BodyMetrics REST Layer — **READY**
- [ ] TASK-P1-009 — Fix/Delete 8 Excluded Broken Test Files — **READY**
- [ ] TASK-P1-010 — Fix Webhook URL Mismatch + Add Secret Validation — **READY**

### Backend P1 (depends on P1-001, P1-002, P1-003)
- [ ] TASK-P1-004 — Progress Aggregation Endpoints — **BACKLOG**

### Backend P1 (depends on P1-001, P1-002)
- [ ] TASK-P1-011 — Feedback REST Endpoints + AI Prompt Integration — **BACKLOG**

### Frontend P1 (parallel, independent)
- [ ] TASK-P1-005 — Frontend Mock vs Live Environment Setup — **READY**
- [ ] TASK-P1-006 — Frontend Auth Token Secure Storage — **READY**
- [ ] TASK-P1-007 — Fix Frontend API Path Mismatches — **READY**

### Frontend P1 (depends on P1-005, P1-006, P1-007, and backend P1-001–P1-004)
- [ ] TASK-P1-008 — Wire Frontend to Real Backend APIs — **BACKLOG**

## Notes
- Use `tasks/TASK_TEMPLATE.md` for all new backend tasks.
- Follow `TESTING_REQUIREMENTS.md` for mandatory local verification before marking DONE.
- TDD is required for backend tasks: write unit tests first, then implement.
- Coverage target: >=80% unit test coverage for changed, testable business-logic scope (not strict on simple model/config/wiring classes).
- No backend task may be marked DONE without runtime proof block (startup, endpoint reachability, auth/path tests, and commands used).
- Testing-phase open endpoint policy: TASK-BE-015 and TASK-BE-016A may remain open temporarily, but must include `TODO(PROD-HARDEN)` comments.
- Pre-production gate (required before launch): re-enable internal auth protection for scan/worker endpoints and verify access control in runtime proof.
- Preserve backward compatibility where possible.
