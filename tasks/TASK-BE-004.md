# TASK-BE-004 — Payment Foundation (Stripe + Mobile Entitlements)

## Goal
Implement a production-safe payment foundation for BYB using Stripe as the source of truth, with backend-managed entitlements for premium feature gating.

## Priority
High

## Scope
Repo: `byb`
Area: billing domain models, webhook handlers, entitlement checks, secure checkout/session APIs

## In Scope
1. **Provider integration (Stripe)**
   - Add Stripe client configuration via env vars.
   - Create endpoint to generate checkout session for selected plan tier.
   - Create endpoint to open billing portal session (for existing customers).

2. **Webhook-driven source of truth**
   - Add signed webhook endpoint (signature verification mandatory).
   - Handle at minimum:
     - `checkout.session.completed`
     - `invoice.paid`
     - `customer.subscription.updated`
     - `customer.subscription.deleted`
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
   - Add request/response examples for checkout + entitlement summary.

## Out of Scope
- Full in-app purchase receipt validation (RevenueCat integration)
- Promotions/coupons/affiliate logic
- Invoicing UI beyond basic portal redirect support

## Security/Compliance Requirements
- Never trust client-side payment success claims.
- Webhook signature verification is mandatory.
- Store only necessary billing metadata (no raw card data).
- Fail closed for premium gating if entitlement status is unknown.

## API Contract Targets (proposed)
- `POST /api/v1/billing/checkout-session`
- `POST /api/v1/billing/portal-session`
- `POST /api/v1/billing/webhooks/stripe`
- `GET /api/v1/billing/entitlements/me`

## Acceptance Criteria
1. Checkout session can be created and completes successfully in test mode.
2. Webhook events update entitlement state correctly and idempotently.
3. Premium entitlement summary endpoint reflects real subscription state.
4. Protected premium route enforces entitlement checks.
5. Existing non-billing flows remain unaffected.

## Test Steps
1. Create checkout session for a test user and complete payment in Stripe test mode.
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
