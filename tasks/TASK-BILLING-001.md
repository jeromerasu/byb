# TASK-BILLING-001 — Backend Payment Enforcement

## Goal
Make the payment system functional end-to-end: enforce subscription tier checks on plan generation, add a billing status endpoint, improve webhook lifecycle handling, and link RevenueCat customer IDs to users.

## Priority
High

## Scope
Repo: `byb`
Area: billing enforcement, plan generation gating, webhook handling, billing controller

## In Scope

### 1. Enforce tier checks on plan generation
- `PlanController` (`POST /api/v1/plan/generate`): check active Premium entitlement via `PremiumAccessService` before allowing generation; return 403 with body `"Upgrade to Premium to unlock AI plan generation"` for Free tier users
- `PlanScanJobService`: skip users without an active Premium entitlement when scanning for plan generation candidates
- `QueueOrchestrator`: re-validate entitlement before executing a claimed queue entry; if entitlement expired between enqueue and execution, mark the entry as failed with a clear message
- All checks routed through `PremiumAccessService`

### 2. Billing status endpoint
- `GET /api/v1/billing/status` — returns current user's subscription info (requires Bearer token):
  - `planTier` (FREE/PREMIUM)
  - `subscriptionStatus` (ACTIVE/CANCELLED/EXPIRED/BILLING_ISSUE/etc.)
  - `entitlementActive` (boolean)
  - `currentPeriodEnd` (ISO timestamp, nullable)
  - `canGeneratePlans` (boolean — computed live from entitlement check)

### 3. Link RevenueCat customer ID to user
- `POST /api/v1/billing/link-customer` — accepts a RevenueCat customer ID in the request body, associates it with the authenticated user
- Update `BillingEntitlement.provider_customer_id` with the supplied value
- Called by the mobile app after a successful purchase

### 4. Webhook event handling improvements
Ensure correct handling of all lifecycle event types:
- `INITIAL_PURCHASE` → create/update entitlement, set `planTier=PREMIUM`, `entitlementActive=true`
- `RENEWAL` → extend `currentPeriodEnd`, ensure `entitlementActive=true`
- `CANCELLATION` → set `subscriptionStatus=CANCELLED` (entitlement remains active until `currentPeriodEnd`)
- `EXPIRATION` → set `entitlementActive=false`, `subscriptionStatus=EXPIRED`
- `BILLING_ISSUE` → set `subscriptionStatus=BILLING_ISSUE`, keep entitlement active during grace period
- `PRODUCT_CHANGE` → update `planTier` accordingly

Additional webhook requirements:
- Add structured SLF4J logging for each event type (event received, outcome, user affected)
- Validate the RevenueCat webhook secret header; reject with 401 if missing or invalid

### 5. Billing enforcement flag (independent of beta.mode)
- Add property `billing.enforcement.enabled` (boolean)
- When `true`, plan generation enforces entitlement checks regardless of `beta.mode`
- Default: `false` in `application-test.properties`, `true` in `application-beta.properties` and `application-prod.properties`
- Allows auth to remain relaxed in test while billing is enforced in beta/prod

### 6. PlanUsageTracker integration
- On successful plan generation, increment `plans_generated` count for the current billing period via `PlanUsageTracker`
- `GET /api/v1/billing/usage` — returns plans generated in the current billing period

### 7. Webhook Event Log table (Flyway migration)
- Flyway migration: create `webhook_event_log` table:

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| user_id | UUID FK → users, nullable | Nullable — user may not be resolvable for every event |
| provider_customer_id | VARCHAR | RevenueCat customer ID from event payload |
| event_type | VARCHAR | INITIAL_PURCHASE, RENEWAL, CANCELLATION, EXPIRATION, BILLING_ISSUE, PRODUCT_CHANGE |
| event_payload | TEXT | Raw JSON from RevenueCat |
| processed_successfully | BOOLEAN | |
| error_message | TEXT, nullable | Populated when processing fails |
| received_at | TIMESTAMP | |

- JPA entity `WebhookEventLog` mapping to above schema
- Repository `WebhookEventLogRepository` with basic query methods
- Log every incoming webhook event to this table before processing begins
- On processing failure, persist the error message; on success, set `processed_successfully=true`
- Useful for debugging billing issues and customer support

## Out of Scope
- Mobile/frontend RevenueCat SDK integration
- Subscription plan management or pricing changes
- RefundEvent handling

## Dependencies
None — `BillingEntitlement`, `PremiumAccessService`, and `PlanUsageTracker` already exist

## API Contract Impact

### New Endpoints
- `GET /api/v1/billing/status` — subscription status for authenticated user
- `POST /api/v1/billing/link-customer` — associate RevenueCat customer ID
- `GET /api/v1/billing/usage` — plan generation usage for current billing period

### Changed Endpoints
- `POST /api/v1/plan/generate` — may now return 403 when `billing.enforcement.enabled=true` and user has no active entitlement

### Backward Compatibility
- No existing response shapes changed
- Plan generation 403 only triggers when `billing.enforcement.enabled=true`; test profile defaults to `false`, so existing test flows are unaffected

### Sample Response — GET /api/v1/billing/status
```json
{
  "planTier": "PREMIUM",
  "subscriptionStatus": "ACTIVE",
  "entitlementActive": true,
  "currentPeriodEnd": "2026-04-28T23:59:59Z",
  "canGeneratePlans": true
}
```

### Sample Request — POST /api/v1/billing/link-customer
```json
{
  "revenueCatCustomerId": "rc_customer_abc123"
}
```

### Sample Response — GET /api/v1/billing/usage
```json
{
  "plansGeneratedThisPeriod": 2,
  "billingPeriodStart": "2026-03-01T00:00:00Z",
  "billingPeriodEnd": "2026-03-31T23:59:59Z"
}
```

## Acceptance Criteria
1. Free tier user calling `POST /api/v1/plan/generate` when `billing.enforcement.enabled=true` receives 403 with message `"Upgrade to Premium to unlock AI plan generation"`.
2. Premium user with active entitlement calling `POST /api/v1/plan/generate` proceeds to generation without error.
3. `PlanScanJobService` skips users without active Premium entitlement; no plan generation is queued for Free tier users.
4. `QueueOrchestrator` marks a queue entry as failed with a clear message when entitlement expires between enqueue and execution.
5. `GET /api/v1/billing/status` returns correct `planTier`, `subscriptionStatus`, `entitlementActive`, `currentPeriodEnd`, and `canGeneratePlans` for the authenticated user.
6. `POST /api/v1/billing/link-customer` updates `provider_customer_id` on the user's `BillingEntitlement` record and returns 200.
7. Each webhook event type (`INITIAL_PURCHASE`, `RENEWAL`, `CANCELLATION`, `EXPIRATION`, `BILLING_ISSUE`, `PRODUCT_CHANGE`) updates `BillingEntitlement` correctly.
8. Webhook endpoint rejects requests with a missing or invalid secret header with 401.
9. `billing.enforcement.enabled=false` in test profile; plan generation proceeds without entitlement check in test environment.
10. Successful plan generation increments `plans_generated` in `PlanUsageTracker`; `GET /api/v1/billing/usage` reflects the updated count.
11. All new business logic has ≥80% unit test coverage (service layer); DTO/config classes excluded from strict threshold.
12. Every incoming webhook request is persisted to `webhook_event_log` before processing; `processed_successfully` and `error_message` reflect the outcome.

## Test Steps
1. Start app with test profile: `mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true`
2. Verify `billing.enforcement.enabled=false` in test profile — confirm plan generation proceeds without entitlement check.
3. Set `billing.enforcement.enabled=true` temporarily; register a new user (Free tier); call `POST /api/v1/plan/generate` — expect 403 with correct message.
4. Simulate a Premium user (seed or webhook); call `POST /api/v1/plan/generate` — expect plan generation to proceed.
5. Call `GET /api/v1/billing/status` as the Premium user — verify all fields are present and accurate.
6. Call `POST /api/v1/billing/link-customer` with a test RevenueCat customer ID — expect 200 and confirm DB update.
7. Call `GET /api/v1/billing/status` again — confirm `provider_customer_id` is linked (or surface via status if included).
8. Send a test webhook payload for each event type (`INITIAL_PURCHASE`, `RENEWAL`, `CANCELLATION`, `EXPIRATION`, `BILLING_ISSUE`, `PRODUCT_CHANGE`) — verify correct `BillingEntitlement` state after each.
9. Send a webhook with a missing or wrong secret header — expect 401.
10. After a successful plan generation, call `GET /api/v1/billing/usage` — confirm `plansGeneratedThisPeriod` incremented.
11. Verify existing plan generation endpoints still return 200 in test profile with `billing.enforcement.enabled=false`.
12. Send a test webhook payload — query `webhook_event_log` table and confirm a row was inserted with correct `event_type`, `provider_customer_id`, `processed_successfully=true`, and raw `event_payload`.
13. Send a webhook that triggers a processing error — confirm row is inserted with `processed_successfully=false` and a non-null `error_message`.

## TDD + Unit Test Coverage (required)
- Write/commit unit tests first for entitlement enforcement logic (red → green → refactor)
- Cover: Free tier blocked, Premium tier allowed, expired entitlement at execution time (QueueOrchestrator path), billing flag off bypasses check
- Cover all six webhook event types: correct field transitions for each
- Cover billing status response shape and `canGeneratePlans` computation
- Cover link-customer: successful update, unauthenticated rejection
- Cover webhook event log: row inserted before processing, `processed_successfully=true` on success, `processed_successfully=false` and error message populated on failure
- Target ≥80% unit test coverage for `BillingEntitlementService`, `PremiumAccessService` (new logic), and any new controller/service classes
- Exclude from strict threshold: DTOs, config/properties binding classes, `WebhookEventLog` entity POJO, `WebhookEventLogRepository` interface
- Include JaCoCo report summary in deliverables

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Verify all new endpoints are reachable: `/api/v1/billing/status`, `/api/v1/billing/link-customer`, `/api/v1/billing/usage`
- Verify authenticated flow: register → obtain JWT → call all three billing endpoints
- Verify enforcement flag: toggle `billing.enforcement.enabled` and confirm Free tier 403 / bypass behavior
- Verify webhook flow locally: send POST to webhook endpoint with correct and incorrect secret headers
- Verify webhook event log: confirm rows are inserted into `webhook_event_log` for each test webhook call
- Document exact curl commands used

## Files Likely Affected
- `src/main/java/com/workoutplanner/controller/PlanController.java`
- `src/main/java/com/workoutplanner/service/PlanScanJobService.java`
- `src/main/java/com/workoutplanner/service/QueueOrchestrator.java`
- `src/main/java/com/workoutplanner/service/BillingEntitlementService.java`
- `src/main/java/com/workoutplanner/service/PremiumAccessService.java`
- `src/main/java/com/workoutplanner/controller/BillingController.java` (new or update existing)
- `src/main/java/com/workoutplanner/dto/BillingStatusDto.java` (new)
- `src/main/java/com/workoutplanner/dto/LinkCustomerRequestDto.java` (new)
- `src/main/java/com/workoutplanner/dto/BillingUsageDto.java` (new)
- `src/main/resources/application-test.properties`
- `src/main/resources/application-beta.properties`
- `src/main/resources/application-prod.properties`
- `src/main/resources/db/migration/V{next}__create_webhook_event_log.sql` (new)
- `src/main/java/com/workoutplanner/model/WebhookEventLog.java` (new)
- `src/main/java/com/workoutplanner/repository/WebhookEventLogRepository.java` (new)
- `src/test/java/com/workoutplanner/service/BillingEntitlementServiceTest.java`
- `src/test/java/com/workoutplanner/controller/BillingControllerTest.java`

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response for all three new endpoints and the 403 plan generation response)
- JaCoCo coverage report summary
- Runtime proof block: startup log snippet, curl commands with responses for all billing endpoints, webhook event test payloads and resulting DB state (including `webhook_event_log` rows), enforcement flag toggle demonstration

## Status
`READY`

## Notes
- `billing.enforcement.enabled` is intentionally decoupled from `beta.mode` — this allows auth to stay relaxed during testing while billing enforcement can be toggled independently per environment
- Webhook secret validation should read from a configurable property (e.g. `revenuecat.webhook.secret`) set via environment variable in beta/prod
- `CANCELLATION` must not immediately deactivate the entitlement — the user retains access until `currentPeriodEnd`; only `EXPIRATION` sets `entitlementActive=false`
- `PlanUsageTracker` increment should be idempotent if possible, or clearly documented if not
