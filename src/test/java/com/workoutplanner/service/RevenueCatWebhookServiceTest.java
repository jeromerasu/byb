package com.workoutplanner.service;

import com.workoutplanner.dto.RevenueCatWebhookDto;
import com.workoutplanner.model.SubscriptionTier;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WebhookEventLog;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WebhookEventLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueCatWebhookServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WebhookEventLogRepository webhookEventLogRepository;

    @InjectMocks
    private RevenueCatWebhookService service;

    private RevenueCatWebhookDto makeWebhook(String eventType, String userId, String entitlementId) {
        RevenueCatWebhookDto dto = new RevenueCatWebhookDto();
        RevenueCatWebhookDto.Event event = new RevenueCatWebhookDto.Event();
        event.setType(eventType);
        event.setAppUserId(userId);
        event.setEntitlementId(entitlementId);
        dto.setEvent(event);
        return dto;
    }

    private User makeUser(SubscriptionTier tier) {
        User u = new User();
        u.setSubscriptionTier(tier);
        return u;
    }

    @Test
    void initialPurchase_standard_setsStandardTier() {
        User user = makeUser(SubscriptionTier.FREE);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(webhookEventLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhookEvent(makeWebhook("INITIAL_PURCHASE", "u1", "standard"), "{}");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getSubscriptionTier()).isEqualTo(SubscriptionTier.STANDARD);
    }

    @Test
    void initialPurchase_coaching_setsCoachingTier() {
        User user = makeUser(SubscriptionTier.FREE);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(webhookEventLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhookEvent(makeWebhook("INITIAL_PURCHASE", "u1", "coaching"), "{}");

        verify(userRepository).save(argThat(u -> u.getSubscriptionTier() == SubscriptionTier.COACHING));
    }

    @Test
    void expiration_downgradesToFree() {
        User user = makeUser(SubscriptionTier.STANDARD);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(webhookEventLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhookEvent(makeWebhook("EXPIRATION", "u1", null), "{}");

        verify(userRepository).save(argThat(u -> u.getSubscriptionTier() == SubscriptionTier.FREE));
    }

    @Test
    void cancellation_doesNotImmediatelyDowngrade() {
        User user = makeUser(SubscriptionTier.STANDARD);
        lenient().when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(webhookEventLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhookEvent(makeWebhook("CANCELLATION", "u1", null), "{}");

        // User should NOT have been updated (tier retained until period end)
        verify(userRepository, never()).save(any());
        assertThat(user.getSubscriptionTier()).isEqualTo(SubscriptionTier.STANDARD);
    }

    @Test
    void productChange_updatesToNewTier() {
        User user = makeUser(SubscriptionTier.STANDARD);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(webhookEventLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhookEvent(makeWebhook("PRODUCT_CHANGE", "u1", "coaching"), "{}");

        verify(userRepository).save(argThat(u -> u.getSubscriptionTier() == SubscriptionTier.COACHING));
    }

    @Test
    void allEvents_persistToWebhookEventLogBeforeProcessing() {
        User user = makeUser(SubscriptionTier.FREE);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(webhookEventLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhookEvent(makeWebhook("INITIAL_PURCHASE", "u1", "standard"), "raw");

        // Log should be saved at least twice: once before (pending) and once after (success)
        verify(webhookEventLogRepository, atLeast(2)).save(any(WebhookEventLog.class));
    }

    @Test
    void entitlementTierResolution_unknownEntitlement_resolvesFree() {
        RevenueCatWebhookDto.Event event = new RevenueCatWebhookDto.Event();
        event.setEntitlementId("unknown_tier");
        assertThat(service.resolveEntitlementTier(event)).isEqualTo(SubscriptionTier.FREE);
    }
}
