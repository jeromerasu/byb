# TASK-BE-007 — Integration - Real-Time Metrics Updates + Nightly Snapshot Rebuild

## Goal
Implement a hybrid metrics pipeline: update user metrics on each log submission and run nightly rebuild for consistency/backfill.

## Priority
High

## Scope
Repo: `byb`
Area: progress service, object-storage reads/writes, snapshot generation, cron-safe rebuild endpoint

## In Scope
- On submission (weight/exercise log), update per-user metrics snapshot immediately
- Build/refresh snapshot payloads for 7d/30d/1y (+ month filter support)
- Add secure rebuild endpoint for scheduled nightly recomputation
- Ensure idempotent processing and safe reruns
- Emit basic run metadata (`lastUpdatedAt`, counts)

## Out of Scope
- UI changes
- External analytics systems

## Depends On
- TASK-BE-005 object storage progress APIs

## Acceptance Criteria
1. Metrics update instantly after user input.
2. Nightly rebuild can recompute snapshots from source events.
3. Snapshot output is stable and supports frontend graph ranges.
4. Rebuild endpoint is protected and idempotent.

## Test Steps
1. Submit log entries and verify snapshot delta.
2. Run rebuild endpoint and verify consistency.
3. Re-run rebuild to confirm idempotent behavior.

## Deliverables
- Commit hash
- Changed files
- Snapshot schema + rebuild runbook

## Status
READY
