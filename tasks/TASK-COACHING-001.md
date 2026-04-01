# TASK-COACHING-001 — Coaching Tier: Backend Infrastructure & Plan Generation Pipeline

## Goal
Introduce a three-tier subscription model (FREE, STANDARD, COACHING), add the Coach entity with its own auth, build the coaching-specific plan generation pipeline (custom prompt templates, per-client directives, GPT-4o), close outstanding 016 gaps (user_week_plan registry, worker trigger endpoint, queue retention), and wire RevenueCat webhook tier sync as the single source of truth for subscription state.

## Priority
High

## Scope
Repo: `byb`
Area: user model, subscription enforcement, coach entity, plan generation pipeline, prompt strategy, queue pipeline, webhook handler

## In Scope

### 1. SubscriptionTier enum + User column
- Create `SubscriptionTier` enum: `FREE`, `STANDARD`, `COACHING`
- Add `subscription_tier` column to `users` table (default `FREE`) via Flyway migration
- Map column to `User` JPA entity
- Existing `Role` values (`USER`/`ADMIN`/`PREMIUM`) remain unchanged — they govern auth/authorization; `subscription_tier` is billing tier only
- Only `STANDARD` and `COACHING` users may enqueue or execute plan generation; `FREE` users receive 403 with message `"Upgrade to Standard or Coaching to unlock AI plan generation"`
- Route all tier checks through a new `SubscriptionAccessService`

### 2. Coach entity + auth
- New `Coach` JPA entity: `id` (UUID PK), `name`, `email` (unique), `hashed_password`, `credentials` (TEXT), `bio` (TEXT), `created_at`, `updated_at`
- Flyway migration: `coaches` table with above columns
- Coach auth is separate from mobile user auth — `Coach` does not extend or share `User`; JWT issuance for coaches is isolated to a `CoachAuthService` (foundation for future web portal)
- Nullable `coach_id` FK on `users` (set only when `subscription_tier = COACHING`); FK constraint + index in migration
- `CoachRepository` and basic `CoachService` (CRUD + lookup by email)

### 3. user_week_plan registry (closes TASK-BE-016C gap)
- New `UserWeekPlan` JPA entity: `id` (UUID PK), `user_id` (FK → users), `week_start` (DATE), `workout_storage_key` (VARCHAR), `diet_storage_key` (VARCHAR), `generated_at` (TIMESTAMP), `generated_by` (enum: `CRON`, `COACH`, `MANUAL`)
- Flyway migration: `user_week_plan` table with unique constraint on `(user_id, week_start)`
- Update `PlanPersistenceService` to write a registry row after each successful plan generation
- Upsert behavior: coach-triggered regeneration for the same `(user_id, week_start)` overwrites the existing row; storage keys are updated, `generated_by` is set to `COACH`
- `UserWeekPlanRepository` with queries for history lookup (by userId, by week range)

### 4. CoachPromptTemplate + CoachDirective entities
- `CoachPromptTemplate` entity: `id` (UUID PK), `coach_id` (FK → coaches), `user_id` (UUID, nullable FK → users), `prompt_content` (TEXT), `created_at`, `updated_at`
  - `user_id = null` → coach's default template (applies to all their clients unless overridden)
  - `user_id` set → per-client custom template (takes precedence over coach default)
  - Unique constraint on `(coach_id, user_id)` (nulls allowed for default)
- `CoachDirective` entity: `id` (UUID PK), `coach_id` (FK → coaches), `user_id` (FK → users), `directive_type` (enum: `WORKOUT`, `DIET`, `GENERAL`), `content` (TEXT), `active` (BOOLEAN default true), `created_at`, `updated_at`
  - Active directives are injected into the prompt at generation time
- Flyway migrations for both tables
- CRUD REST endpoints (coach-authed):
  - `POST /api/v1/coach/prompt-templates` — create or update template (upsert by coach + user)
  - `GET /api/v1/coach/prompt-templates/{userId}` — fetch template for a specific client (falls back to coach default)
  - `POST /api/v1/coach/directives` — add a directive for a client
  - `PUT /api/v1/coach/directives/{id}` — update content or toggle `active`
  - `GET /api/v1/coach/directives/{userId}` — list all directives for a client (filterable by `active`)
  - `DELETE /api/v1/coach/directives/{id}` — soft-delete (set `active = false`) or hard-delete

### 5. PromptStrategy refactor
- Define `PromptStrategy` interface: `resolveModel()`, `resolveSystemPrompt(userId)`, `resolveDirectives(userId)` → returns a `ResolvedPromptContext` record (model string, system prompt, list of directive strings)
- `StandardPromptStrategy`: model = `gpt-4o-mini`, base system prompt (current behavior), no directives
- `CoachingPromptStrategy`: model = `gpt-4o`, resolves coach's custom template for the user (fallback to coach default, then base prompt), injects all active `CoachDirective` entries for the user
- `PlanGenerationExecutorService` resolves the correct strategy based on `user.subscriptionTier` before calling OpenAI
- Refactor `OpenAIService.generateCombinedPlans()` to accept `ResolvedPromptContext` instead of hardcoding model/prompt — no caller-facing contract change
- Remove all `if tier == X` branching from generation logic; strategy pattern is the sole extension point

### 6. Worker trigger endpoint (closes TASK-BE-016A gap)
- `POST /api/v1/plans/generate` — triggers immediate plan generation for a specific user by inserting a `PENDING` entry into `plan_generation_queue`
- Request body: `{ "userId": "<uuid>" }`
- Add `generated_by` column (enum: `CRON`, `COACH`, `MANUAL`) to `plan_generation_queue` table via Flyway migration; default `MANUAL`
- Auth: coach (for their assigned clients) or the authenticated user themselves; reject with 403 otherwise
- During current testing phase, keep endpoint accessible and add:
  - `TODO(PROD-HARDEN): tighten coach auth check before production cutover`
  - Include target date/owner note in code comment

### 7. Queue retention cleanup (closes TASK-BE-016E gap)
- Daily `@Scheduled` job: `QueueRetentionCleanupJob`
- Deletes `COMPLETED` rows older than 30 days
- Deletes `FAILED` rows older than 90 days
- Retention windows configurable via properties:
  - `queue.retention.completed-days=30`
  - `queue.retention.failed-days=90`
- Defaults defined in `application.properties`; overridable per profile
- Structured SLF4J logging: rows deleted per status, execution timestamp

### 8. RevenueCat webhook tier sync
- Extend or create `POST /api/webhooks/revenuecat` webhook handler
- Map RevenueCat entitlement identifiers → `SubscriptionTier`:
  - No active entitlement → `FREE`
  - `standard` entitlement → `STANDARD`
  - `coaching` entitlement → `COACHING`
- Webhook is the **only** code path that writes `subscription_tier` on `User` — no other service or endpoint may directly set this field
- Handle lifecycle events: `INITIAL_PURCHASE`, `RENEWAL`, `CANCELLATION`, `EXPIRATION`, `PRODUCT_CHANGE`
  - `CANCELLATION`: retain current tier until `currentPeriodEnd` (do not downgrade immediately)
  - `EXPIRATION`: downgrade to `FREE`
  - `PRODUCT_CHANGE`: remap entitlement → tier and update immediately
- Reject missing/invalid webhook secret header with 401
- Log all events to `webhook_event_log` (existing table) before processing

## Out of Scope
- Coach web portal UI (Phase 2 — new `byb-coach-portal` Next.js repo)
- Mobile-side subscription purchase flow
- Coach assignment workflow (UI for assigning a coach to a user)
- RefundEvent webhook handling
- Billing enforcement flag changes (already handled in TASK-BILLING-001)

## Dependencies
- TASK-BILLING-001 — `BillingEntitlement`, `PremiumAccessService`, `webhook_event_log` table, and RevenueCat webhook handler foundation must exist
- TASK-BE-016A — `plan_generation_queue` table and worker pipeline must exist
- TASK-BE-016C — `PlanPersistenceService` must be in place (this task extends it with the registry write)

## API Contract Impact

### New Endpoints
- `POST /api/webhooks/revenuecat` — extend existing or create; maps entitlements to `SubscriptionTier`
- `POST /api/v1/plans/generate` — manual/coach trigger; inserts PENDING queue entry
- `POST /api/v1/coach/prompt-templates` — create/update prompt template for a client
- `GET /api/v1/coach/prompt-templates/{userId}` — fetch resolved template for a client
- `POST /api/v1/coach/directives` — add a directive for a client
- `PUT /api/v1/coach/directives/{id}` — update or toggle directive
- `GET /api/v1/coach/directives/{userId}` — list directives for a client
- `DELETE /api/v1/coach/directives/{id}` — deactivate or remove directive

### Changed Endpoints
- `POST /api/v1/plan/generate` — now also 403 for `FREE` tier (via `SubscriptionAccessService`)

### Backward Compatibility
- Existing `Role`-based auth is untouched
- All existing `STANDARD`/`PREMIUM` plan generation flows continue to work; `SubscriptionTier` check is additive
- `generated_by` column on `plan_generation_queue` is nullable/defaulted — no migration-time data loss

### Sample Request — POST /api/v1/plans/generate
```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Sample Response — GET /api/v1/coach/prompt-templates/{userId}
```json
{
  "templateId": "f1e2d3c4-b5a6-7890-1234-abcdef567890",
  "coachId": "c0ach123-0000-0000-0000-000000000001",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "promptContent": "You are a strength coach specializing in hypertrophy. The client is intermediate level...",
  "isCoachDefault": false,
  "updatedAt": "2026-03-15T10:30:00Z"
}
```

### Sample Request — POST /api/v1/coach/directives
```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "directiveType": "WORKOUT",
  "content": "Focus on hypertrophy rep ranges (8-12). Avoid overhead pressing movements due to shoulder history."
}
```

### Sample Response — GET /api/v1/coach/directives/{userId}
```json
[
  {
    "id": "d1r2e3c4-t5i6-7890-abcd-ef1234567890",
    "directiveType": "WORKOUT",
    "content": "Focus on hypertrophy rep ranges (8-12). Avoid overhead pressing movements due to shoulder history.",
    "active": true,
    "createdAt": "2026-03-10T09:00:00Z"
  },
  {
    "id": "d2i3e4t5-0000-0000-0000-000000000002",
    "directiveType": "DIET",
    "content": "Client is lactose intolerant. Do not include dairy-based protein sources.",
    "active": true,
    "createdAt": "2026-03-12T14:00:00Z"
  }
]
```

## Acceptance Criteria
1. `FREE` tier user calling `POST /api/v1/plan/generate` or `POST /api/v1/plans/generate` receives 403 with message `"Upgrade to Standard or Coaching to unlock AI plan generation"`.
2. `STANDARD` tier user's plan generation uses `gpt-4o-mini` and the base system prompt; no directives injected.
3. `COACHING` tier user's plan generation uses `gpt-4o`, the coach's resolved prompt template (per-client → coach default → base), and all active `CoachDirective` entries injected.
4. `PromptStrategy` interface is the sole branching point for model/prompt resolution — no `if tier ==` branches in generation service code.
5. After every successful plan generation, a row is written to `user_week_plan`; subsequent generation for the same `(user_id, week_start)` upserts the row with updated storage keys and `generated_by`.
6. `POST /api/v1/plans/generate` inserts a `PENDING` queue entry; `generated_by` column reflects `COACH` or `MANUAL` based on caller.
7. Coach CRUD endpoints for `CoachPromptTemplate` and `CoachDirective` behave correctly: create, read (with fallback logic), update, delete/deactivate.
8. RevenueCat webhook correctly transitions `subscription_tier` for `INITIAL_PURCHASE`, `RENEWAL`, `CANCELLATION`, `EXPIRATION`, and `PRODUCT_CHANGE` events; `CANCELLATION` does not immediately downgrade.
9. Webhook endpoint rejects requests with a missing or invalid secret header with 401.
10. `QueueRetentionCleanupJob` deletes `COMPLETED` rows older than configured threshold and `FAILED` rows older than configured threshold; retention windows are overridable per profile.
11. All new business logic has ≥80% unit test coverage (service layer); DTO/config/entity POJO classes excluded from strict threshold.
12. Every incoming RevenueCat webhook event is persisted to `webhook_event_log` before processing.

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Confirm clean startup and all Flyway migrations applied (check logs for `Successfully applied N migrations`).
3. Register a new user (default `FREE` tier); call `POST /api/v1/plans/generate` — expect 403 with correct message.
4. Seed a `STANDARD` user; call `POST /api/v1/plans/generate` — expect `PENDING` queue entry with `generated_by = MANUAL`; confirm `gpt-4o-mini` is used in generation logs.
5. Seed a `COACHING` user with an assigned coach; create a `CoachPromptTemplate` and two active `CoachDirective` entries for the user; call `POST /api/v1/plans/generate` as coach — confirm `gpt-4o` is used, custom template is present in the OpenAI request, and directives are injected.
6. After generation, query `user_week_plan` — confirm a row exists with correct `user_id`, `week_start`, storage keys, and `generated_by`.
7. Trigger a second generation for the same user and week — confirm `user_week_plan` row is upserted (not duplicated).
8. Call `GET /api/v1/coach/prompt-templates/{userId}` with no per-client template set — confirm response falls back to coach default template.
9. Call `POST /api/v1/coach/directives`, then `PUT /api/v1/coach/directives/{id}` (toggle `active = false`), then `GET /api/v1/coach/directives/{userId}?active=true` — confirm deactivated directive is excluded.
10. Send test RevenueCat webhook for each event type (`INITIAL_PURCHASE` → `STANDARD`, `PRODUCT_CHANGE` → `COACHING`, `CANCELLATION`, `EXPIRATION`) — verify `subscription_tier` on User updates correctly; verify `webhook_event_log` row is inserted for each.
11. Send a webhook with a missing or wrong secret header — expect 401.
12. Manually set `queue.retention.completed-days=0` and trigger `QueueRetentionCleanupJob`; confirm COMPLETED rows are deleted. Restore default.
13. Verify existing plan generation endpoints still function correctly for `STANDARD` users.

## TDD + Unit Test Coverage (required)
- Write/commit unit tests first for `SubscriptionAccessService` tier enforcement logic (red → green → refactor)
- Cover `PromptStrategy` resolution: `STANDARD` gets mini model + base prompt, `COACHING` gets gpt-4o + resolved template + directives, `FREE` is blocked before resolution
- Cover `CoachingPromptStrategy` fallback chain: per-client template → coach default → base prompt
- Cover `UserWeekPlan` upsert: first write creates row, second write updates row (no duplicate)
- Cover `CoachPromptTemplate` CRUD: create, read with fallback, update, delete
- Cover `CoachDirective` CRUD: add, toggle active, filter by active flag
- Cover RevenueCat webhook tier mapping: each event type → correct `subscription_tier` transition; `CANCELLATION` does not downgrade immediately
- Cover `QueueRetentionCleanupJob`: correct rows deleted per status and configured retention window
- Cover `POST /api/v1/plans/generate` auth: coach for assigned client succeeds, coach for unassigned client receives 403, user for themselves succeeds
- Target ≥80% unit test coverage for `SubscriptionAccessService`, `CoachPromptTemplateService`, `CoachDirectiveService`, `CoachingPromptStrategy`, `UserWeekPlanService`, `QueueRetentionCleanupJob`, and all new controller classes
- Exclude from strict threshold: DTOs, entity POJOs, `CoachRepository`/`UserWeekPlanRepository` interfaces, config/properties binding classes
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup with all Flyway migrations applied
- Verify all new endpoints are reachable: `/api/v1/plans/generate`, `/api/v1/coach/prompt-templates`, `/api/v1/coach/directives`
- Verify tier enforcement: seed FREE, STANDARD, and COACHING users; confirm 403 for FREE and successful queue insertion for STANDARD/COACHING
- Verify PromptStrategy selection via generation logs: confirm model name in OpenAI request log matches expected tier
- Verify user_week_plan registry write: inspect DB after generation for correct row
- Verify coach CRUD flow: create template → create directives → fetch resolved template → deactivate directive → confirm exclusion from next resolution
- Verify webhook tier sync: POST to `/api/webhooks/revenuecat` for each event type; inspect `users.subscription_tier` and `webhook_event_log` after each
- Verify queue retention: seed stale COMPLETED and FAILED rows; trigger cleanup job; confirm correct rows deleted
- Document exact curl commands used

## Files Likely Affected
- `src/main/java/com/workoutplanner/model/SubscriptionTier.java` (new enum)
- `src/main/java/com/workoutplanner/model/User.java` (add subscriptionTier field + coachId FK)
- `src/main/java/com/workoutplanner/model/Coach.java` (new entity)
- `src/main/java/com/workoutplanner/model/UserWeekPlan.java` (new entity)
- `src/main/java/com/workoutplanner/model/CoachPromptTemplate.java` (new entity)
- `src/main/java/com/workoutplanner/model/CoachDirective.java` (new entity)
- `src/main/java/com/workoutplanner/model/DirectiveType.java` (new enum)
- `src/main/java/com/workoutplanner/model/GeneratedBy.java` (new enum)
- `src/main/java/com/workoutplanner/repository/CoachRepository.java` (new)
- `src/main/java/com/workoutplanner/repository/UserWeekPlanRepository.java` (new)
- `src/main/java/com/workoutplanner/repository/CoachPromptTemplateRepository.java` (new)
- `src/main/java/com/workoutplanner/repository/CoachDirectiveRepository.java` (new)
- `src/main/java/com/workoutplanner/service/SubscriptionAccessService.java` (new)
- `src/main/java/com/workoutplanner/service/CoachService.java` (new)
- `src/main/java/com/workoutplanner/service/CoachPromptTemplateService.java` (new)
- `src/main/java/com/workoutplanner/service/CoachDirectiveService.java` (new)
- `src/main/java/com/workoutplanner/service/UserWeekPlanService.java` (new)
- `src/main/java/com/workoutplanner/service/QueueRetentionCleanupJob.java` (new)
- `src/main/java/com/workoutplanner/strategy/PromptStrategy.java` (new interface)
- `src/main/java/com/workoutplanner/strategy/ResolvedPromptContext.java` (new record)
- `src/main/java/com/workoutplanner/strategy/StandardPromptStrategy.java` (new)
- `src/main/java/com/workoutplanner/strategy/CoachingPromptStrategy.java` (new)
- `src/main/java/com/workoutplanner/service/PlanGenerationExecutorService.java` (update — resolve strategy)
- `src/main/java/com/workoutplanner/service/OpenAIService.java` (update — accept ResolvedPromptContext)
- `src/main/java/com/workoutplanner/service/PlanPersistenceService.java` (update — write user_week_plan row)
- `src/main/java/com/workoutplanner/controller/PlanController.java` (update — add /api/v1/plans/generate trigger endpoint + tier check)
- `src/main/java/com/workoutplanner/controller/CoachController.java` (new)
- `src/main/java/com/workoutplanner/service/RevenueCatWebhookService.java` (update or new — tier sync)
- `src/main/resources/application.properties` (add queue retention defaults)
- `src/main/resources/db/migration/V{next}__add_subscription_tier_to_users.sql` (new)
- `src/main/resources/db/migration/V{next+1}__create_coaches.sql` (new)
- `src/main/resources/db/migration/V{next+2}__add_coach_id_to_users.sql` (new)
- `src/main/resources/db/migration/V{next+3}__create_user_week_plan.sql` (new)
- `src/main/resources/db/migration/V{next+4}__create_coach_prompt_template.sql` (new)
- `src/main/resources/db/migration/V{next+5}__create_coach_directive.sql` (new)
- `src/main/resources/db/migration/V{next+6}__add_generated_by_to_plan_generation_queue.sql` (new)
- `src/test/java/com/workoutplanner/service/SubscriptionAccessServiceTest.java` (new)
- `src/test/java/com/workoutplanner/strategy/PromptStrategyTest.java` (new)
- `src/test/java/com/workoutplanner/service/CoachPromptTemplateServiceTest.java` (new)
- `src/test/java/com/workoutplanner/service/CoachDirectiveServiceTest.java` (new)
- `src/test/java/com/workoutplanner/service/UserWeekPlanServiceTest.java` (new)
- `src/test/java/com/workoutplanner/service/QueueRetentionCleanupJobTest.java` (new)
- `src/test/java/com/workoutplanner/controller/CoachControllerTest.java` (new)

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for all new endpoints and the 403 plan generation response)
- JaCoCo coverage report summary
- Runtime proof block: startup log snippet, curl commands for tier enforcement (FREE 403 + STANDARD/COACHING 200), coach CRUD flow, RevenueCat webhook event payloads with resulting `subscription_tier` and `webhook_event_log` state, user_week_plan DB state after generation + upsert, retention cleanup log output

## Status
`READY`

## Notes
- `SubscriptionTier` and `Role` are intentionally separate concerns — do not conflate them; billing tier changes must not affect auth authorization checks
- Webhook is the sole writer of `subscription_tier` — no admin endpoint or seeding script should bypass this in production; test-profile seeding for unit tests is acceptable
- `CoachingPromptStrategy` fallback chain (per-client → coach default → base) must be explicit and tested; an unassigned coach or missing template must never cause a generation failure
- Coach auth (`CoachAuthService`) is intentionally minimal in Phase 1 — it is a foundation for the Phase 2 web portal, not a full auth system; keep it simple and clearly marked for extension
- Flyway migration numbering must be coordinated with the team to avoid gaps or conflicts with concurrent migrations; use the next available version numbers in sequence
- `generated_by` column on `plan_generation_queue` is nullable in migration (existing rows have no value) — service layer defaults it to `MANUAL` when not explicitly set
- All infrastructure choices (DB, storage, queue) remain agnostic to Render; nothing here ties to Render-specific APIs
