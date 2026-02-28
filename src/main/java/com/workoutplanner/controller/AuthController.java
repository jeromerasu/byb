package com.workoutplanner.controller;

import com.workoutplanner.dto.AuthRequest;
import com.workoutplanner.dto.AuthResponse;
import com.workoutplanner.dto.RegisterRequest;
import com.workoutplanner.dto.MobileLoginRequest;
import com.workoutplanner.dto.MobileAuthResponse;
import com.workoutplanner.dto.SocialLoginRequest;
import com.workoutplanner.model.User;
import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long jwtExpiration;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                         UserService userService,
                         JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setDateOfBirth(registerRequest.getDateOfBirth());

        return userService.registerUser(user)
                .map(savedUser -> {
                    String accessToken = jwtService.generateToken(savedUser);
                    String refreshToken = jwtService.generateRefreshToken(savedUser);

                    AuthResponse response = new AuthResponse(
                        accessToken,
                        refreshToken,
                        jwtExpiration,
                        savedUser
                    );
                    response.setMessage("User registered successfully");

                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> {
                    AuthResponse errorResponse = new AuthResponse(error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        return Mono.fromCallable(() -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        authRequest.getUsernameOrEmail(),
                        authRequest.getPassword()
                    )
                );

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String accessToken = jwtService.generateToken(userDetails);
                String refreshToken = jwtService.generateRefreshToken(userDetails);

                // Update last login
                userService.updateLastLogin(userDetails.getUsername()).subscribe();

                // Get full user details
                User user = (User) userDetails;

                AuthResponse response = new AuthResponse(
                    accessToken,
                    refreshToken,
                    jwtExpiration,
                    user
                );
                response.setMessage("Login successful");

                return ResponseEntity.ok(response);

            } catch (BadCredentialsException e) {
                AuthResponse errorResponse = new AuthResponse("Invalid username/email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            } catch (Exception e) {
                AuthResponse errorResponse = new AuthResponse("Authentication failed: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        return Mono.fromCallable(() -> {
            try {
                String username = jwtService.extractUsername(refreshToken);
                UserDetails userDetails = userService.loadUserByUsername(username);

                if (jwtService.isTokenValid(refreshToken, userDetails)) {
                    String newAccessToken = jwtService.generateToken(userDetails);
                    String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                    AuthResponse response = new AuthResponse(
                        newAccessToken,
                        newRefreshToken,
                        jwtExpiration,
                        (User) userDetails
                    );
                    response.setMessage("Token refreshed successfully");

                    return ResponseEntity.ok(response);
                } else {
                    AuthResponse errorResponse = new AuthResponse("Invalid refresh token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
            } catch (Exception e) {
                AuthResponse errorResponse = new AuthResponse("Token refresh failed: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout() {
        return Mono.fromCallable(() -> {
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        });
    }

    @PostMapping("/verify-email")
    public Mono<ResponseEntity<Map<String, String>>> verifyEmail(@RequestBody Map<String, String> request) {
        String verificationToken = request.get("verificationToken");

        return userService.verifyEmail(verificationToken)
                .map(verified -> {
                    if (verified) {
                        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "Invalid or expired verification token"));
                    }
                });
    }

    @PostMapping("/forgot-password")
    public Mono<ResponseEntity<Map<String, String>>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        return userService.generatePasswordResetToken(email)
                .map(resetToken -> {
                    if (resetToken != null) {
                        // In a real application, you would send this token via email
                        return ResponseEntity.ok(Map.of(
                            "message", "Password reset token generated",
                            "resetToken", resetToken // Remove this in production
                        ));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "User not found with this email"));
                    }
                })
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest()
                        .body(Map.of("message", "User not found with this email"))));
    }

    @PostMapping("/reset-password")
    public Mono<ResponseEntity<Map<String, String>>> resetPassword(@RequestBody Map<String, String> request) {
        String resetToken = request.get("resetToken");
        String newPassword = request.get("newPassword");

        return userService.resetPassword(resetToken, newPassword)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "Invalid or expired reset token"));
                    }
                });
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<User>> getCurrentUser(Authentication authentication) {
        return Mono.fromCallable(() -> {
            if (authentication != null && authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        });
    }

    @GetMapping("/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateToken(Authentication authentication) {
        return Mono.fromCallable(() -> {
            if (authentication != null && authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "message", "Invalid or expired token"));
            }
        });
    }

    @PostMapping("/check-availability")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkAvailability(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");

        Mono<Boolean> usernameExists = username != null ?
            userService.existsByUsername(username) : Mono.just(false);
        Mono<Boolean> emailExists = email != null ?
            userService.existsByEmail(email) : Mono.just(false);

        return Mono.zip(usernameExists, emailExists)
                .map(tuple -> {
                    Map<String, Boolean> availability = Map.of(
                        "usernameAvailable", !tuple.getT1(),
                        "emailAvailable", !tuple.getT2()
                    );
                    return ResponseEntity.ok(availability);
                });
    }

    // ===== MOBILE-SPECIFIC ENDPOINTS =====

    @PostMapping("/mobile/login")
    public Mono<ResponseEntity<MobileAuthResponse>> mobileLogin(@Valid @RequestBody MobileLoginRequest loginRequest) {
        return Mono.fromCallable(() -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                    )
                );

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = (User) userDetails;

                // Generate tokens with extended expiration for "remember me"
                Long expiration = loginRequest.isRememberMe() ? jwtExpiration * 7 : jwtExpiration; // 7 days if remember me
                String accessToken = jwtService.generateTokenWithExpiration(userDetails, expiration);
                String refreshToken = jwtService.generateRefreshToken(userDetails);

                // Update last login
                userService.updateLastLogin(userDetails.getUsername()).subscribe();

                // Log device info (for security/analytics)
                if (loginRequest.getDeviceId() != null) {
                    System.out.println("Mobile login - User: " + user.getUsername() +
                        ", Device: " + loginRequest.getDeviceType() +
                        " (" + loginRequest.getDeviceName() + ")" +
                        ", App Version: " + loginRequest.getAppVersion());
                }

                MobileAuthResponse response = new MobileAuthResponse(accessToken, refreshToken, expiration, user);
                response.setMessage("Mobile login successful");

                return ResponseEntity.ok(response);

            } catch (BadCredentialsException e) {
                MobileAuthResponse errorResponse = new MobileAuthResponse("Invalid username/email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            } catch (Exception e) {
                MobileAuthResponse errorResponse = new MobileAuthResponse("Authentication failed: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        });
    }

    @PostMapping("/mobile/register")
    public Mono<ResponseEntity<MobileAuthResponse>> mobileRegister(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setDateOfBirth(registerRequest.getDateOfBirth());

        return userService.registerUser(user)
                .map(savedUser -> {
                    String accessToken = jwtService.generateToken(savedUser);
                    String refreshToken = jwtService.generateRefreshToken(savedUser);

                    MobileAuthResponse response = new MobileAuthResponse(accessToken, refreshToken, jwtExpiration, savedUser);
                    response.setMessage("Registration successful! Please verify your email to access all features.");

                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .onErrorResume(error -> {
                    MobileAuthResponse errorResponse = new MobileAuthResponse(error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    @PostMapping("/mobile/refresh")
    public Mono<ResponseEntity<MobileAuthResponse>> mobileRefreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        return Mono.fromCallable(() -> {
            try {
                String username = jwtService.extractUsername(refreshToken);
                UserDetails userDetails = userService.loadUserByUsername(username);

                if (jwtService.isTokenValid(refreshToken, userDetails)) {
                    String newAccessToken = jwtService.generateToken(userDetails);
                    String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                    MobileAuthResponse response = new MobileAuthResponse(
                        newAccessToken,
                        newRefreshToken,
                        jwtExpiration,
                        (User) userDetails
                    );
                    response.setMessage("Token refreshed successfully");

                    return ResponseEntity.ok(response);
                } else {
                    MobileAuthResponse errorResponse = new MobileAuthResponse("Invalid refresh token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
            } catch (Exception e) {
                MobileAuthResponse errorResponse = new MobileAuthResponse("Token refresh failed: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        });
    }

    @GetMapping("/mobile/profile")
    public Mono<ResponseEntity<Map<String, Object>>> getMobileProfile(Authentication authentication) {
        return Mono.fromCallable(() -> {
            if (authentication != null && authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();

                Map<String, Object> profile = Map.of(
                    "user", user,
                    "server_time", java.time.LocalDateTime.now(),
                    "account_status", Map.of(
                        "is_active", user.isActive(),
                        "email_verified", user.isEmailVerified(),
                        "role", user.getRole(),
                        "member_since", user.getCreatedAt(),
                        "last_login", user.getLastLogin()
                    )
                );

                return ResponseEntity.ok(profile);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
            }
        });
    }

    @PostMapping("/mobile/quick-validate")
    public Mono<ResponseEntity<Map<String, Object>>> quickValidateToken(
            @RequestHeader("Authorization") String authHeader) {

        return Mono.fromCallable(() -> {
            try {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    String username = jwtService.extractUsername(token);
                    UserDetails userDetails = userService.loadUserByUsername(username);

                    if (jwtService.isTokenValid(token, userDetails)) {
                        User user = (User) userDetails;
                        return ResponseEntity.ok(Map.of(
                            "valid", true,
                            "user_id", user.getId(),
                            "username", user.getUsername(),
                            "email_verified", user.isEmailVerified(),
                            "expires_at", jwtService.extractExpiration(token)
                        ));
                    }
                }

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Invalid token"));

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Token validation failed"));
            }
        });
    }

    // ===== SOCIAL LOGIN ENDPOINTS (Framework Ready) =====

    @PostMapping("/social/login")
    public Mono<ResponseEntity<MobileAuthResponse>> socialLogin(@Valid @RequestBody SocialLoginRequest socialRequest) {
        return Mono.fromCallable(() -> {
            // TODO: Implement social login verification
            // 1. Verify the access token with the social provider (Google, Apple, Facebook)
            // 2. Extract user information from the provider
            // 3. Check if user exists in database by provider ID or email
            // 4. Create user if doesn't exist, or update existing user
            // 5. Generate JWT tokens and return

            MobileAuthResponse errorResponse = new MobileAuthResponse(
                "Social login is not yet implemented. Coming soon!"
            );
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
        });
    }

    @PostMapping("/social/link")
    public Mono<ResponseEntity<Map<String, Object>>> linkSocialAccount(
            Authentication authentication,
            @Valid @RequestBody SocialLoginRequest socialRequest) {

        return Mono.fromCallable(() -> {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
            }

            // TODO: Implement social account linking
            // 1. Verify the social provider token
            // 2. Link the social account to the current user
            // 3. Store provider information

            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                    "success", false,
                    "message", "Social account linking is not yet implemented. Coming soon!"
                ));
        });
    }

    @DeleteMapping("/social/unlink/{provider}")
    public Mono<ResponseEntity<Map<String, Object>>> unlinkSocialAccount(
            Authentication authentication,
            @PathVariable String provider) {

        return Mono.fromCallable(() -> {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
            }

            // TODO: Implement social account unlinking
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                    "success", false,
                    "message", "Social account unlinking is not yet implemented. Coming soon!"
                ));
        });
    }
}