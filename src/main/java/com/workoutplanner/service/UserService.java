package com.workoutplanner.service;

import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));
    }

    public Mono<User> registerUser(User user) {
        return Mono.fromCallable(() -> {
            // Check if username already exists
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new RuntimeException("Username is already taken");
            }

            // Check if email already exists
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email is already registered");
            }

            // Encode password
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // Generate verification token
            user.setVerificationToken(UUID.randomUUID().toString());

            // Set default values
            user.setActive(true);
            user.setEmailVerified(false);
            user.setRole(User.Role.USER);

            return userRepository.save(user);
        });
    }

    public Mono<User> findByUsername(String username) {
        return Mono.fromCallable(() -> userRepository.findByUsername(username).orElse(null));
    }

    public Mono<User> findByEmail(String email) {
        return Mono.fromCallable(() -> userRepository.findByEmail(email).orElse(null));
    }

    public Mono<User> findById(String id) {
        return Mono.fromCallable(() -> userRepository.findById(id).orElse(null));
    }

    public Optional<User> findByIdSync(String id) {
        return userRepository.findById(id);
    }

    public Mono<User> updateUser(User user) {
        return Mono.fromCallable(() -> {
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    public Mono<User> updateLastLogin(String username) {
        return findByUsername(username)
                .filter(user -> user != null)
                .flatMap(user -> {
                    user.setLastLogin(LocalDateTime.now());
                    return updateUser(user);
                });
    }

    public Mono<Boolean> existsByUsername(String username) {
        return Mono.fromCallable(() -> userRepository.existsByUsername(username));
    }

    public Mono<Boolean> existsByEmail(String email) {
        return Mono.fromCallable(() -> userRepository.existsByEmail(email));
    }

    public Mono<Boolean> verifyEmail(String verificationToken) {
        return Mono.fromCallable(() -> {
            Optional<User> userOpt = userRepository.findByVerificationToken(verificationToken);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setEmailVerified(true);
                user.setVerificationToken(null);
                userRepository.save(user);
                return true;
            }
            return false;
        });
    }

    public Mono<String> generatePasswordResetToken(String email) {
        return findByEmail(email)
                .filter(user -> user != null)
                .map(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    user.setResetToken(resetToken);
                    user.setResetTokenExpiry(LocalDateTime.now().plusHours(24)); // Token expires in 24 hours
                    userRepository.save(user);
                    return resetToken;
                });
    }

    public Mono<Boolean> resetPassword(String resetToken, String newPassword) {
        return Mono.fromCallable(() -> {
            Optional<User> userOpt = userRepository.findByResetToken(resetToken);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Check if token is not expired
                if (user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetToken(null);
                    user.setResetTokenExpiry(null);
                    userRepository.save(user);
                    return true;
                }
            }
            return false;
        });
    }

    public Flux<User> findAllActiveUsers() {
        return Mono.fromCallable(() -> userRepository.findActiveUsers())
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<User> findRecentUsers(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return Mono.fromCallable(() -> userRepository.findUsersRegisteredSince(fromDate))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Long> getUserRegistrationCount(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return Mono.fromCallable(() -> userRepository.countUsersRegisteredSince(fromDate));
    }

    public Flux<User> findActiveUsers(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return Mono.fromCallable(() -> userRepository.findUsersActiveAfter(fromDate))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<User> updateProfile(String userId, User profileUpdate) {
        return findById(userId)
                .filter(user -> user != null)
                .map(user -> {
                    if (profileUpdate.getFirstName() != null) {
                        user.setFirstName(profileUpdate.getFirstName());
                    }
                    if (profileUpdate.getLastName() != null) {
                        user.setLastName(profileUpdate.getLastName());
                    }
                    if (profileUpdate.getPhoneNumber() != null) {
                        user.setPhoneNumber(profileUpdate.getPhoneNumber());
                    }
                    if (profileUpdate.getDateOfBirth() != null) {
                        user.setDateOfBirth(profileUpdate.getDateOfBirth());
                    }
                    if (profileUpdate.getProfileImageUrl() != null) {
                        user.setProfileImageUrl(profileUpdate.getProfileImageUrl());
                    }
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                });
    }

    public Mono<Boolean> changePassword(String userId, String oldPassword, String newPassword) {
        return findById(userId)
                .filter(user -> user != null)
                .map(user -> {
                    if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                        user.setPassword(passwordEncoder.encode(newPassword));
                        user.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(user);
                        return true;
                    }
                    return false;
                });
    }

    public Mono<Boolean> deactivateUser(String userId) {
        return findById(userId)
                .filter(user -> user != null)
                .map(user -> {
                    user.setActive(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                    return true;
                });
    }
}