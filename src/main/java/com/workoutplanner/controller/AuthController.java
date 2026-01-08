package com.workoutplanner.controller;

import com.workoutplanner.dto.AuthRequest;
import com.workoutplanner.dto.AuthResponse;
import com.workoutplanner.dto.RegisterRequest;
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
}