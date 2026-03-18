# BACKEND TASK QUEUE

## Workflow Rules
1. Pick first task with status `READY`.
2. Set to `IN_PROGRESS` before coding.
3. Keep scope tight and contract-safe.
4. Commit with task ID in message.
5. Update task with results.
6. Set status to `DONE` or `BLOCKED`.

## Queue
- [ ] TASK-BE-001 — Unified Plan Contract (Workout + Diet) — **READY**
- [ ] TASK-BE-002 — Storage Metadata Integrity + Lookup APIs — **READY**
- [ ] TASK-BE-004 — Payment Foundation (RevenueCat Entitlements) — **READY**
- [ ] TASK-BE-005 — Progress - Exercise Object Storage History and 30-Day APIs — **READY**
- [ ] TASK-BE-006 — Integration - API Contract Stabilization for Mobile Frontend — **READY**
- [ ] TASK-BE-007 — Integration - Real-Time Metrics Updates + Nightly Snapshot Rebuild — **READY**
- [ ] TASK-BE-008 — Integration - Combined Workout + Diet Plan Generate API — **READY**
- [ ] TASK-BE-009 — API - Exercise Catalog List from Generated Plan Responses — **READY**
- [ ] TASK-BE-010 — API - Diet Food Catalog List from Generated Plan Responses — **READY**
- [ ] TASK-BE-003 — Auth/Profile Unification for Mobile — **BACKLOG**

## Notes
- Use `tasks/TASK_TEMPLATE.md` for all new backend tasks.
- Follow `TESTING_REQUIREMENTS.md` for mandatory local verification before marking DONE.
- No backend task may be marked DONE without runtime proof block (startup, endpoint reachability, auth/path tests, and commands used).
- Preserve backward compatibility where possible.
