# TASK-BE-016B — Plan Generation Worker - Workout+Diet Generation Execution Path

## Goal
Connect claimed queue items to combined workout/diet generation workflow and produce next-week plans.

## Priority
High

## Scope
Repo: `byb`
Area: worker execution service, combined plan generation integration

## In Scope
- For each claimed queue item, call combined generation flow
- Ensure strict JSON schema handling for generated output
- Write generated week metadata for downstream persistence
- Handle generation errors cleanly and return to queue state handling

## Out of Scope
- Retry scheduler policy implementation
- Reconciliation job

## Dependencies
- TASK-BE-016A

## Acceptance Criteria
1. Claimed queue items generate next-week workout + diet plans successfully.
2. Failures are surfaced with actionable error state.
3. Output remains schema-valid for storage and frontend retrieval.

## Test Steps
1. Process sample queue row end-to-end through generation call.
2. Validate generated payload schema.
3. Simulate generation failure and verify error state.

## Deliverables
- Commit hash
- Changed files
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY
