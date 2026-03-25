# Billing API Documentation

## Overview

This document describes the RevenueCat-based billing and entitlement system for the BYB (Build Your Body) application. The system provides secure subscription management, premium feature gating, and webhook-driven entitlement updates.

## Environment Variables

### Required Configuration

```bash
# RevenueCat Configuration
REVENUECAT_API_KEY=your_revenuecat_api_key
REVENUECAT_WEBHOOK_SECRET=your_webhook_secret_for_signature_verification
REVENUECAT_APP_ID=your_app_id
REVENUECAT_ENVIRONMENT=development  # or 'production'

# Billing Configuration
BILLING_PREMIUM_ENABLED=true
BILLING_FREE_PLANS_LIMIT=3
BILLING_FREE_EXPORTS_LIMIT=1
```

### Optional Configuration

```bash
# Database configuration (if different from defaults)
DATABASE_URL=postgresql://localhost:5432/workoutdb
FLYWAY_ENABLED=true  # Enable database migrations
```

## API Endpoints

### 1. RevenueCat Webhook Handler

**Endpoint:** `POST /api/v1/billing/webhooks/revenuecat`

**Purpose:** Receives subscription lifecycle events from RevenueCat

**Headers:**
- `Authorization`: Bearer token (optional)
- `X-RevenueCat-Signature`: HMAC signature for verification

**Request Body:** RevenueCat webhook payload (JSON)

**Response:**
```json
{
  "status": "success",
  "message": "Webhook processed successfully"
}
```

**Error Response:**
```json
{
  "error": "Invalid signature"
}
```

**Supported Events:**
- `INITIAL_PURCHASE`: First subscription purchase
- `RENEWAL`: Subscription renewal
- `CANCELLATION`: Subscription cancelled
- `EXPIRATION`: Subscription expired
- `BILLING_ISSUE`: Payment failed
- `PRODUCT_CHANGE`: Plan tier changed

### 2. Sync User Entitlements

**Endpoint:** `POST /api/v1/billing/entitlements/sync`

**Purpose:** Manually sync entitlement status for the authenticated user

**Authentication:** Required (JWT token)

**Request:** No body required

**Response:**
```json
{
  "status": "success",
  "entitlement": {
    "planTier": "PREMIUM",
    "subscriptionStatus": "ACTIVE",
    "entitlementActive": true,
    "hasActivePremiumEntitlement": true,
    "currentPeriodEnd": "2024-04-01T10:30:00",
    "isSubscriptionExpired": false,
    "isInGracePeriod": false,
    "updatedAt": "2024-03-01T10:30:00"
  }
}
```

### 3. Get Current User Entitlements

**Endpoint:** `GET /api/v1/billing/entitlements/me`

**Purpose:** Retrieve current entitlement status for authenticated user

**Authentication:** Required (JWT token)

**Response:**
```json
{
  "status": "success",
  "entitlement": {
    "planTier": "FREE",
    "subscriptionStatus": "ACTIVE",
    "entitlementActive": true,
    "hasActivePremiumEntitlement": false,
    "currentPeriodEnd": null,
    "isSubscriptionExpired": false,
    "isInGracePeriod": false
  }
}
```

### 4. Check Feature Access

**Endpoint:** `GET /api/v1/billing/entitlements/check/{feature}`

**Purpose:** Check if user has access to a specific premium feature

**Authentication:** Required (JWT token)

**Path Parameters:**
- `feature`: Feature name to check (see Feature List below)

**Response:**
```json
{
  "status": "success",
  "feature": "unlimited_plans",
  "hasAccess": false,
  "userId": "user123"
}
```

## Feature List

### Free Tier Features
- `basic_workouts`: Basic workout plan generation
- `basic_nutrition`: Basic nutrition plan generation

### Premium Features
- `unlimited_plans`: Generate unlimited workout/diet plans
- `custom_workouts`: Create custom workout routines
- `nutrition_tracking`: Advanced nutrition tracking
- `progress_analytics`: Detailed progress analytics

## Plan Tiers

| Tier | Features | Limits |
|------|----------|---------|
| **FREE** | Basic workouts, Basic nutrition | 3 plans total, 1 export |
| **PREMIUM** | All basic + Premium features | Unlimited plans, exports |
| **PRO** | All features + Early access | All premium + priority support |

## Integration Workflow

### 1. RevenueCat Webhook Setup

1. Configure webhook URL in RevenueCat dashboard:
   ```
   https://your-domain.com/api/v1/billing/webhooks/revenuecat
   ```

2. Set webhook secret in environment variables

3. RevenueCat will send events for subscription changes

### 2. Mobile App Integration

```swift
// Check feature access before allowing premium features
func checkPremiumAccess() async {
    let response = await apiClient.get("/api/v1/billing/entitlements/me")
    if response.entitlement.hasActivePremiumEntitlement {
        // Enable premium features
    } else {
        // Show upgrade prompt
    }
}
```

### 3. Backend Feature Gating

```java
@RestController
public class WorkoutController {

    @Autowired
    private PremiumAccessService premiumAccessService;

    @PostMapping("/workouts/unlimited")
    public ResponseEntity<?> generateUnlimitedPlan(Authentication auth) {
        String userId = auth.getName();

        if (!premiumAccessService.hasFeatureAccess(userId, PremiumFeature.UNLIMITED_PLANS)) {
            return ResponseEntity.status(402) // Payment Required
                .body("Premium subscription required");
        }

        // Generate unlimited plan
        return ResponseEntity.ok(plan);
    }
}
```

## Webhook Event Examples

### Initial Purchase Event
```json
{
  "event": {
    "type": "INITIAL_PURCHASE",
    "app_user_id": "user123",
    "product_id": "premium_monthly",
    "purchased_at_ms": 1640995200000,
    "expiration_at_ms": 1643673600000,
    "environment": "production"
  },
  "api_version": "1.0"
}
```

### Cancellation Event
```json
{
  "event": {
    "type": "CANCELLATION",
    "app_user_id": "user123",
    "expiration_at_ms": 1643673600000
  }
}
```

## Security Considerations

### Webhook Security
- All webhooks MUST verify HMAC signatures
- Webhook secret should be stored securely
- Failed signature verification results in HTTP 401

### Access Control
- All billing endpoints require authentication
- Feature access defaults to "denied" on errors
- Entitlement checks fail closed for security

### Data Protection
- No sensitive payment data stored locally
- Only RevenueCat IDs and subscription metadata retained
- All billing queries are audited

## Testing

### Local Testing Workflow

1. Start application with test profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"
   ```

2. Create test user entitlement:
   ```bash
   curl -X POST http://localhost:8080/api/v1/billing/entitlements/sync \
        -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```

3. Test feature access:
   ```bash
   curl http://localhost:8080/api/v1/billing/entitlements/check/unlimited_plans \
        -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```

### RevenueCat Sandbox Testing

1. Configure sandbox environment:
   ```bash
   REVENUECAT_ENVIRONMENT=development
   ```

2. Use RevenueCat test webhooks to simulate subscription events

3. Verify entitlement updates in database

## Error Handling

| Error Code | Description | Resolution |
|------------|-------------|------------|
| 401 | Unauthorized | Check JWT token validity |
| 402 | Payment Required | User needs premium subscription |
| 500 | Internal Error | Check logs, database connectivity |

## Database Schema

### billing_entitlements Table

```sql
CREATE TABLE billing_entitlements (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    provider_customer_id VARCHAR(255),
    provider_subscription_id VARCHAR(255) UNIQUE,
    plan_tier VARCHAR(50) NOT NULL,
    subscription_status VARCHAR(50) NOT NULL,
    current_period_end TIMESTAMP,
    entitlement_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_webhook_event VARCHAR(255),
    webhook_event_timestamp TIMESTAMP
);
```

## Rollback Procedure

1. Disable billing features:
   ```bash
   BILLING_PREMIUM_ENABLED=false
   ```

2. Revert database migration:
   ```bash
   flyway undo
   ```

3. Remove billing endpoints from load balancer

4. All users default to free tier access

## Monitoring

### Key Metrics
- Webhook processing success rate
- Entitlement sync frequency
- Feature access denial rate
- Subscription status distribution

### Alerts
- Failed webhook signature verification
- Database connection errors during billing checks
- High rate of premium feature denials

## Support

### Common Issues

1. **Webhook signature failures**: Verify REVENUECAT_WEBHOOK_SECRET matches RevenueCat dashboard
2. **Feature access denied**: Check user subscription status and plan tier
3. **Database migration errors**: Ensure PostgreSQL has proper permissions

### RevenueCat Dashboard Links
- Production: https://app.revenuecat.com
- Sandbox testing: Use development environment settings