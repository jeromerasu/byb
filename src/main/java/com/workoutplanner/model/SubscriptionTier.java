package com.workoutplanner.model;

/**
 * Billing subscription tier for a user.
 * Intentionally separate from User.Role — Role governs auth/authorization;
 * SubscriptionTier governs feature access gated by billing.
 *
 * The RevenueCat webhook is the sole writer of this field on User.
 */
public enum SubscriptionTier {
    FREE,
    STANDARD,
    COACHING
}
