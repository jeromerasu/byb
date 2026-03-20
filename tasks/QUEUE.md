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
- [ ] TASK-BE-012 — API/DB - Persist User Theme Preferences (Accent + Panel Style) — **READY**
- [ ] TASK-BE-003 — Auth/Profile Unification for Mobile — **BACKLOG**

## Notes
- Use `tasks/TASK_TEMPLATE.md` for all new backend tasks.
- Follow `TESTING_REQUIREMENTS.md` for mandatory local verification before marking DONE.
- No backend task may be marked DONE without runtime proof block (startup, endpoint reachability, auth/path tests, and commands used).
- Preserve backward compatibility where possible.
