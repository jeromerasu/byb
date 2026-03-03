# TASK-BE-005 — Progress - Exercise Object Storage History and 30-Day APIs

## Goal
Create backend APIs and storage contracts for per-user exercise history (sets/reps/weight) with rolling 30-day retention and heatmap-ready aggregation.

## Priority
High

## Scope
Repo: `byb`
Area: progress controllers/services/DTOs, object storage integration, metadata/indexing, auth-scoped access

## In Scope
1. **Object storage contract**
   - Per-user exercise history objects in bucket (auth-scoped keying)
   - Suggested key shape: `users/{userId}/exercises/{exerciseId}.json`
   - Object payload stores daily entries with:
     - date
     - sets
     - reps
     - weight
     - optional sessionId
   - Enforce rolling 30-day retention on write/update

2. **Progress logging API**
   - `POST /api/v1/progress/exercises/{exerciseId}/log`
   - Validates and appends/merges day entry
   - Returns updated exercise progress summary

3. **Exercise history API**
   - `GET /api/v1/progress/exercises/{exerciseId}?window=30d`
   - Returns chronologically sorted entries + max/pr info for charting

4. **Heatmap API**
   - `GET /api/v1/progress/heatmap?window=30d`
   - Returns daily workout activity counts/intensity for last 30 days

5. **Exercise list API (optional but recommended)**
   - `GET /api/v1/progress/exercises`
   - Returns tracked exercises with latest entry + max weight

6. **Safety + integrity**
   - User can access only own progress objects
   - Idempotent handling for repeated log submits
   - Graceful behavior when no history exists

## Out of Scope
- Historical windows beyond 30 days
- ML predictions/recommendation engine
- Social comparisons/leaderboards

## Acceptance Criteria
1. Logging endpoint stores and returns valid per-exercise entries.
2. History endpoint returns sorted 30-day data suitable for line chart rendering.
3. Heatmap endpoint returns 30-day activity map with consistent schema.
4. Retention cap trims data beyond 30 days.
5. Existing workout/diet APIs remain unaffected.

## Test Steps
1. Submit multiple logs across dates for same exercise.
2. Verify object storage structure and 30-day trim behavior.
3. Fetch exercise history and confirm chronological sorting.
4. Fetch heatmap and confirm workout-day counts.
5. Verify unauthorized user cannot read another user's data.

## Deliverables
- Commit hash
- Changed files
- API request/response examples
- Object schema example JSON
- Risks and rollback notes

## Status
READY
