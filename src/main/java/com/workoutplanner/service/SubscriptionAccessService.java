package com.workoutplanner.service;

import com.workoutplanner.model.SubscriptionTier;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Single gatekeeper for subscription tier enforcement.
 *
 * Design decision: all tier checks route through here so that enforcement
 * logic lives in one place and is independently testable. No {@code if tier ==}
 * branches exist in generation service code — those use PromptStrategy instead.
 */
@Service
public class SubscriptionAccessService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAccessService.class);

    static final String FREE_BLOCKED_MESSAGE =
            "Upgrade to Standard or Coaching to unlock AI plan generation";

    private final UserRepository userRepository;

    public SubscriptionAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns true if the user may enqueue or execute plan generation.
     * FREE tier is blocked; STANDARD and COACHING are allowed.
     */
    public boolean canGeneratePlan(String userId) {
        SubscriptionTier tier = resolveTier(userId);
        boolean allowed = tier == SubscriptionTier.STANDARD || tier == SubscriptionTier.COACHING;
        if (!allowed) {
            log.info("subscription.access.blocked userId={} tier={}", userId, tier);
        }
        return allowed;
    }

    /**
     * Throws an {@link AccessDeniedException} with the standard message if the user
     * is on the FREE tier. Callers should use this to guard generation endpoints.
     */
    public void assertCanGeneratePlan(String userId) {
        if (!canGeneratePlan(userId)) {
            throw new AccessDeniedException(FREE_BLOCKED_MESSAGE);
        }
    }

    public SubscriptionTier resolveTier(String userId) {
        return userRepository.findById(userId)
                .map(User::getSubscriptionTier)
                .orElse(SubscriptionTier.FREE);
    }

    /**
     * Thrown when a FREE-tier user attempts to access a paid feature.
     */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
