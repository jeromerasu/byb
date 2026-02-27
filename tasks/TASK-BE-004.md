# TASK-BE-004 — Payment Foundation (RevenueCat Entitlements)

## Goal
Implement a production-safe mobile subscription foundation for BYB using RevenueCat + app-store billing as the source of truth, with backend-managed entitlements for premium feature gating.

## Priority
High

## Scope
Repo: `byb`
Area: billing domain models, webhook handlers, entitlement checks, secure subscription-state sync APIs

## In Scope
1. **Provider integration (RevenueCat)**
   - Add RevenueCat configuration via env vars.
   - Add endpoint(s) for app clients to sync/refresh entitlement status by app user id.

2. **Webhook-driven source of truth**
   - Add signed webhook endpoint (signature verification mandatory).
   - Handle subscription lifecycle events at minimum:
     - initial purchase / renewal
     - cancellation
     - expiration
     - billing issue / grace-period transitions (if present)
   - Enforce idempotency for webhook processing.

3. **Billing + entitlement persistence**
   - Add billing/entitlement records tied to `user_id`:
     - `provider_customer_id`
     - `provider_subscription_id`
     - `plan_tier`
     - `subscription_status`
     - `current_period_end`
     - `entitlement_active`
     - timestamps/audit fields
   - Add explicit migration(s), reversible.

4. **Premium access checks**
   - Add backend entitlement check service used by protected premium endpoints.
   - Add API endpoint for mobile/frontend to fetch current entitlement summary.

5. **Contract docs + ops notes**
   - Document required env vars, webhook setup, local testing workflow.
   - Add request/response examples for entitlement sync + entitlement summary.

## Out of Scope
- Stripe/web checkout implementation
- Promotions/coupons/affiliate logic
- Complex billing UI beyond entitlement status display

## Security/Compliance Requirements
- Never trust client-side payment success claims.
- Webhook signature verification is mandatory.
- Store only necessary billing metadata (no raw card data).
- Fail closed for premium gating if entitlement status is unknown.

## API Contract Targets (proposed)
- `POST /api/v1/billing/webhooks/revenuecat`
- `POST /api/v1/billing/entitlements/sync`
- `GET /api/v1/billing/entitlements/me`

## Acceptance Criteria
1. RevenueCat webhook events update entitlement state correctly and idempotently.
2. Entitlement sync endpoint can refresh/confirm user entitlement state.
3. Premium entitlement summary endpoint reflects real subscription state.
4. Protected premium route enforces entitlement checks.
5. Existing non-billing flows remain unaffected.

## Test Steps
1. Trigger RevenueCat sandbox purchase flow for a test user.
2. Replay webhook events and verify idempotent behavior.
3. Cancel/expire subscription and confirm entitlement deactivation.
4. Call protected premium endpoint before and after entitlement activation.

## Deliverables
- Commit hash
- Changed files
- DB migration files
- API examples (request/response)
- Webhook event handling matrix
- Risks + rollback notes

## Status
READY
