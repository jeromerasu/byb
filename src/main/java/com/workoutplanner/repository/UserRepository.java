package com.workoutplanner.repository;

import com.workoutplanner.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByVerificationToken(String verificationToken);

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.createdAt DESC")
    List<User> findActiveUsers();

    @Query("SELECT u FROM User u WHERE u.createdAt >= :fromDate ORDER BY u.createdAt DESC")
    List<User> findUsersRegisteredSince(LocalDateTime fromDate);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :fromDate")
    long countUsersRegisteredSince(LocalDateTime fromDate);

    @Query("SELECT u FROM User u WHERE u.lastLogin >= :fromDate ORDER BY u.lastLogin DESC")
    List<User> findUsersActiveAfter(LocalDateTime fromDate);

    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt DESC")
    List<User> findByRole(User.Role role);

    // Basic profile queries
    List<User> findByWorkoutProfileIdIsNotNull();
    List<User> findByDietProfileIdIsNotNull();
    List<User> findByWorkoutProfileIdIsNotNullAndDietProfileIdIsNotNull();
}