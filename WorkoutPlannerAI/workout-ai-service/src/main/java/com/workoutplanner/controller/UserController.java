package com.workoutplanner.controller;

import com.workoutplanner.model.User;
import com.workoutplanner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Mono<ResponseEntity<User>> getProfile(Authentication authentication) {
        return Mono.fromCallable(() -> {
            if (authentication != null && authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();
                return ResponseEntity.ok(user);
            }
            return ResponseEntity.notFound().<User>build();
        });
    }

    @PutMapping("/profile")
    public Mono<ResponseEntity<User>> updateProfile(
            @RequestBody User profileUpdate,
            Authentication authentication) {

        return Mono.fromCallable(() -> {
            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = (User) authentication.getPrincipal();
                return currentUser.getId();
            }
            return null;
        })
        .flatMap(userId -> {
            if (userId != null) {
                return userService.updateProfile(userId, profileUpdate)
                        .map(updatedUser -> {
                            if (updatedUser != null) {
                                return ResponseEntity.ok(updatedUser);
                            }
                            return ResponseEntity.notFound().<User>build();
                        });
            }
            return Mono.just(ResponseEntity.notFound().<User>build());
        });
    }

    @PostMapping("/change-password")
    public Mono<ResponseEntity<Map<String, String>>> changePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        return Mono.fromCallable(() -> {
            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = (User) authentication.getPrincipal();
                return currentUser.getId();
            }
            return null;
        })
        .flatMap(userId -> {
            if (userId != null) {
                return userService.changePassword(userId, oldPassword, newPassword)
                        .map(success -> {
                            if (success) {
                                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
                            }
                            return ResponseEntity.badRequest()
                                    .body(Map.of("message", "Current password is incorrect"));
                        });
            }
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("message", "Authentication required")));
        });
    }

    @DeleteMapping("/account")
    public Mono<ResponseEntity<Map<String, String>>> deactivateAccount(Authentication authentication) {
        return Mono.fromCallable(() -> {
            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = (User) authentication.getPrincipal();
                return currentUser.getId();
            }
            return null;
        })
        .flatMap(userId -> {
            if (userId != null) {
                return userService.deactivateUser(userId)
                        .map(success -> {
                            if (success) {
                                return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
                            }
                            return ResponseEntity.badRequest()
                                    .body(Map.of("message", "Failed to deactivate account"));
                        });
            }
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("message", "Authentication required")));
        });
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<User> getAllUsers() {
        return userService.findAllActiveUsers();
    }

    @GetMapping("/admin/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<User> getRecentUsers(@RequestParam(defaultValue = "30") int days) {
        return userService.findRecentUsers(days);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> getUserStats(
            @RequestParam(defaultValue = "30") int days) {

        Mono<Long> registrationCount = userService.getUserRegistrationCount(days);
        Mono<Long> activeUserCount = userService.findActiveUsers(days)
                .count();

        return Mono.zip(registrationCount, activeUserCount)
                .map(tuple -> ResponseEntity.ok(Map.of(
                    "totalRegistrations", tuple.getT1(),
                    "activeUsers", tuple.getT2(),
                    "periodDays", days
                )));
    }

    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<User>> getUserById(@PathVariable String userId) {
        return userService.findById(userId)
                .map(user -> {
                    if (user != null) {
                        return ResponseEntity.ok(user);
                    }
                    return ResponseEntity.notFound().<User>build();
                });
    }

    @PutMapping("/admin/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<User>> updateUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String roleString = request.get("role");
        User.Role role;

        try {
            role = User.Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().<User>build());
        }

        return userService.findById(userId)
                .filter(user -> user != null)
                .map(user -> {
                    user.setRole(role);
                    return user;
                })
                .flatMap(userService::updateUser)
                .map(updatedUser -> ResponseEntity.ok(updatedUser))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().<User>build()));
    }

    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> deactivateUser(@PathVariable String userId) {
        return userService.deactivateUser(userId)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
                    }
                    return ResponseEntity.<Map<String, String>>notFound()
                            .build();
                });
    }
}