package com.workoutplanner.service;

import com.workoutplanner.model.SubscriptionTier;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionAccessService service;

    private User makeUser(SubscriptionTier tier) {
        User u = new User();
        u.setSubscriptionTier(tier);
        return u;
    }

    @Test
    void freeUser_cannotGenerate() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(makeUser(SubscriptionTier.FREE)));
        assertThat(service.canGeneratePlan("u1")).isFalse();
    }

    @Test
    void standardUser_canGenerate() {
        when(userRepository.findById("u2")).thenReturn(Optional.of(makeUser(SubscriptionTier.STANDARD)));
        assertThat(service.canGeneratePlan("u2")).isTrue();
    }

    @Test
    void coachingUser_canGenerate() {
        when(userRepository.findById("u3")).thenReturn(Optional.of(makeUser(SubscriptionTier.COACHING)));
        assertThat(service.canGeneratePlan("u3")).isTrue();
    }

    @Test
    void unknownUser_defaultsToFreeBlocked() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThat(service.canGeneratePlan("unknown")).isFalse();
    }

    @Test
    void assertCanGenerate_freeUser_throwsWithCorrectMessage() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(makeUser(SubscriptionTier.FREE)));
        assertThatThrownBy(() -> service.assertCanGeneratePlan("u1"))
                .isInstanceOf(SubscriptionAccessService.AccessDeniedException.class)
                .hasMessage(SubscriptionAccessService.FREE_BLOCKED_MESSAGE);
    }

    @Test
    void assertCanGenerate_standardUser_doesNotThrow() {
        when(userRepository.findById("u2")).thenReturn(Optional.of(makeUser(SubscriptionTier.STANDARD)));
        assertThatCode(() -> service.assertCanGeneratePlan("u2")).doesNotThrowAnyException();
    }
}
