package com.workoutplanner.security;

import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for Issue 2: the JWT filter must populate SecurityContext even when
 * beta mode's permitAll() is active — the filter itself is always applied.
 */
@ExtendWith(MockitoExtension.class)
class JwtFilterBetaModeTest {

    @Mock private JwtService jwtService;
    @Mock private UserService userService;
    @Mock private UserDetails userDetails;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_PopulatesSecurityContext() throws Exception {
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtService.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(userDetails);
    }

    @Test
    void noToken_DoesNotSetSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No Authorization header

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidToken_DoesNotSetSecurityContext() throws Exception {
        when(jwtService.extractUsername("bad.token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtService.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad.token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenExtractionException_FilterContinues() throws Exception {
        when(jwtService.extractUsername(anyString())).thenThrow(new RuntimeException("malformed token"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed");

        // Should not throw — filter swallows auth errors and continues chain
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
