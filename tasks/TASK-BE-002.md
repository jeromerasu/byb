# TASK-BE-002 — Storage Metadata Integrity + Lookup APIs

## Goal
Guarantee generated plan artifacts are traceable and retrievable via metadata records, with stable storage-key handling.

## Priority
High

## Scope
Repo: `byb`
Area: storage service + metadata persistence + lookup endpoints

## In Scope
1. Ensure stored artifact key returned by storage service is always source-of-truth.
2. Add metadata model/table (artifact id, storage key, type, size, user id, created at).
3. Add lookup endpoints for current user artifacts.
4. Add validation/guards for missing object references.

## Out of Scope
- Frontend changes
- Signed URL auth hardening beyond basic endpoint support

## Acceptance Criteria
1. Every generated plan has a metadata record.
2. Current plan references always resolve.
3. Artifact list endpoint returns consistent records.

## Test Steps
1. Generate workout + diet plans.
2. Verify metadata row creation.
3. Retrieve artifacts by lookup endpoint.

## Deliverables
- Commit hash
- Changed files
- API examples

## Status
IN_PROGRESS (started: 2026-03-02 14:08)
