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

## Notes
- Use `tasks/TASK_TEMPLATE.md` for all new backend tasks.
- Follow `TESTING_REQUIREMENTS.md` for mandatory local verification before marking DONE.
- TDD is required for backend tasks: write unit tests first, then implement.
- Coverage target: >=80% unit test coverage for changed task scope (attach coverage evidence).
- No backend task may be marked DONE without runtime proof block (startup, endpoint reachability, auth/path tests, and commands used).
- Testing-phase open endpoint policy: TASK-BE-015 and TASK-BE-016A may remain open temporarily, but must include `TODO(PROD-HARDEN)` comments.
- Pre-production gate (required before launch): re-enable internal auth protection for scan/worker endpoints and verify access control in runtime proof.
- Preserve backward compatibility where possible.
