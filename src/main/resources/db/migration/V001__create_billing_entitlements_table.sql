-- Create billing_entitlements table for RevenueCat subscription tracking
CREATE TABLE billing_entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    provider_customer_id VARCHAR(255),
    provider_subscription_id VARCHAR(255),
    plan_tier VARCHAR(50) NOT NULL CHECK (plan_tier IN ('FREE', 'PREMIUM', 'PRO')),
    subscription_status VARCHAR(50) NOT NULL CHECK (subscription_status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'GRACE_PERIOD', 'BILLING_ISSUE', 'PAUSED', 'PENDING')),
    current_period_end TIMESTAMP,
    entitlement_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_webhook_event VARCHAR(255),
    webhook_event_timestamp TIMESTAMP,

    -- Indexes for performance
    CONSTRAINT idx_billing_entitlements_user_id UNIQUE (user_id),
    CONSTRAINT idx_billing_entitlements_provider_subscription_id UNIQUE (provider_subscription_id),
    CONSTRAINT idx_billing_entitlements_provider_customer_id UNIQUE (provider_customer_id)
);

-- Create indexes for common queries
CREATE INDEX idx_billing_entitlements_entitlement_active ON billing_entitlements(entitlement_active);
CREATE INDEX idx_billing_entitlements_subscription_status ON billing_entitlements(subscription_status);
CREATE INDEX idx_billing_entitlements_current_period_end ON billing_entitlements(current_period_end);
CREATE INDEX idx_billing_entitlements_updated_at ON billing_entitlements(updated_at);

-- Add foreign key constraint to users table (assuming users table exists)
ALTER TABLE billing_entitlements
    ADD CONSTRAINT fk_billing_entitlements_user_id
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add comments for documentation
COMMENT ON TABLE billing_entitlements IS 'Tracks subscription entitlements and billing status for users via RevenueCat';
COMMENT ON COLUMN billing_entitlements.user_id IS 'References the user ID from the users table';
COMMENT ON COLUMN billing_entitlements.provider_customer_id IS 'RevenueCat customer ID';
COMMENT ON COLUMN billing_entitlements.provider_subscription_id IS 'RevenueCat subscription ID';
COMMENT ON COLUMN billing_entitlements.plan_tier IS 'Subscription tier: FREE, PREMIUM, or PRO';
COMMENT ON COLUMN billing_entitlements.subscription_status IS 'Current subscription status from RevenueCat';
COMMENT ON COLUMN billing_entitlements.current_period_end IS 'When the current subscription period ends';
COMMENT ON COLUMN billing_entitlements.entitlement_active IS 'Whether user currently has access to premium features';
COMMENT ON COLUMN billing_entitlements.last_webhook_event IS 'Last webhook event type received from RevenueCat';
COMMENT ON COLUMN billing_entitlements.webhook_event_timestamp IS 'Timestamp of last webhook event';