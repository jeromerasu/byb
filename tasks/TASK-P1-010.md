# TASK-P1-010 — Fix Webhook URL Mismatch + Add Secret Validation

## Goal
Resolve the discrepancy between the documented webhook URL (`/api/v1/billing/webhook`) and the actual code path (`/api/v1/billing/webhooks/revenuecat`), and add proper HMAC/header-based webhook secret validation.

## Priority
High

## Scope
Repo: `byb`
Area: billing controller, webhook handler, documentation

## In Scope

### 1. Resolve URL mismatch
- Audit: confirm the actual mapped path in `BillingController` (or equivalent) and the path documented in `ARCHITECTURE.md`
- Decision: pick one canonical path and update the other to match
  - Recommended: keep the more descriptive code path (`/api/v1/billing/webhooks/revenuecat`) and update the docs — or align docs to code
- Update `ARCHITECTURE.md` to reflect the correct path
- Update any other docs, comments, or configuration that references the old path

### 2. Add webhook secret validation
- Read the secret from a configurable property: `revenuecat.webhook.secret` (set via `REVENUECAT_WEBHOOK_SECRET` environment variable)
- Validate the `Authorization` header (or RevenueCat-specific secret header) on every incoming webhook request
- Reject with `401 Unauthorized` if the header is missing or the value does not match the configured secret
- If `revenuecat.webhook.secret` is not configured (empty/null), log a warning and reject all requests to prevent accidental open exposure

### 3. Tests
- Unit test: valid secret in header → request proceeds (returns `200`)
- Unit test: invalid secret in header → returns `401`
- Unit test: missing secret header → returns `401`
- Unit test: secret not configured → returns `401` (no accidental open endpoint)

### 4. Render environment variable documentation
- Document `REVENUECAT_WEBHOOK_SECRET` in `ARCHITECTURE.md` or a deployment notes section
- Note which Render services/environments need this variable set

## Out of Scope
- Full webhook event handling improvements (covered by TASK-BILLING-001)
- RevenueCat SDK integration

## Dependencies
- None — independent backend task

## API Contract Impact

### Changed Endpoints
- Webhook path is canonicalized — one of the two paths (`/api/v1/billing/webhook` or `/api/v1/billing/webhooks/revenuecat`) is updated to match the other
- All existing webhook callers (RevenueCat dashboard config) must be updated to the canonical path

### Backward Compatibility
- Path change is a breaking change for the RevenueCat webhook URL configured in the RevenueCat dashboard — document the required update
- Secret validation is additive but will reject previously unauthenticated requests — document this behavior

## Acceptance Criteria
1. `ARCHITECTURE.md` and the controller mapping agree on a single canonical webhook path.
2. Webhook endpoint rejects requests with a missing secret header with `401`.
3. Webhook endpoint rejects requests with an incorrect secret header with `401`.
4. Webhook endpoint accepts requests with the correct secret header.
5. If `REVENUECAT_WEBHOOK_SECRET` is not set, all webhook requests return `401` and a warning is logged.
6. Unit tests pass for all four secret validation scenarios.
7. `REVENUECAT_WEBHOOK_SECRET` is documented as a required Render environment variable.

## Files Likely Affected
- `src/main/java/.../controller/BillingController.java` (update `@RequestMapping` or `@PostMapping` path if changing code side)
- `src/main/java/.../service/WebhookValidationService.java` (new — or inline in controller)
- `src/main/resources/application.properties` (or `application-beta.properties`, `application-prod.properties`) — add `revenuecat.webhook.secret` property
- `ARCHITECTURE.md` — update documented webhook URL
- `src/test/java/.../controller/BillingControllerTest.java` (add secret validation tests)

## Test Steps
1. Start app with test profile.
2. Configure `revenuecat.webhook.secret=test-secret` in test properties.
3. `POST` to the canonical webhook path with `Authorization: test-secret` header; confirm `200` (or expected webhook processing response).
4. `POST` with `Authorization: wrong-secret`; confirm `401`.
5. `POST` with no `Authorization` header; confirm `401`.
6. Remove `revenuecat.webhook.secret` from properties; confirm all webhook requests return `401` and a warning is logged.
7. Verify `ARCHITECTURE.md` and controller path agree.

## TDD + Unit Test Coverage (required)
- Write secret validation tests first (red → green → refactor)
- Cover all four scenarios: correct secret, incorrect secret, missing header, secret not configured
- Target ≥80% coverage on the webhook validation logic
- Exclude from strict threshold: controller `@RequestMapping` wiring, property binding class

## Mandatory Local Testing Verification (required)
- Start app with test profile and confirm clean startup
- Verify canonical webhook path is reachable
- Run all four secret validation test cases via curl
- Document exact curl commands used

## Deliverables
- Commit hash
- Changed files
- Documented canonical webhook URL (what it is after the fix)
- Curl examples for all four secret validation scenarios
- Note on required RevenueCat dashboard configuration update
- `REVENUECAT_WEBHOOK_SECRET` Render env var documentation

## Status
`READY`

## Notes
- Independent of all other P1 tasks — can run in parallel
- The RevenueCat dashboard webhook URL will need to be updated to the canonical path — flag this explicitly in deliverables so the team knows to update the dashboard config
- If RevenueCat uses a specific header name (not `Authorization`), use that header — check RevenueCat webhook documentation for the correct header
