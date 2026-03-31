package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @JsonIgnore
    @Column(name = "password_hash")
    private String password;

    @JsonProperty("first_name")
    @Column(name = "first_name")
    private String firstName;

    @JsonProperty("last_name")
    @Column(name = "last_name")
    private String lastName;

    @JsonProperty("phone_number")
    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonProperty("date_of_birth")
    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @JsonProperty("is_active")
    @Column(name = "is_active")
    private boolean isActive = true;

    @JsonProperty("email_verified")
    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @JsonProperty("created_at")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("last_login")
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @JsonProperty("profile_image_url")
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "reset_token")
    @JsonIgnore
    private String resetToken;

    @Column(name = "reset_token_expiry")
    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    @Column(name = "verification_token")
    @JsonIgnore
    private String verificationToken;

    // Physical profile (shared between workout and diet)
    @Column(name = "height_cm")
    @JsonProperty("height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    @JsonProperty("weight_kg")
    private BigDecimal weightKg;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private WorkoutProfile.Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    @JsonProperty("activity_level")
    private WorkoutProfile.ActivityLevel activityLevel;

    // Profile references
    @Column(name = "workout_profile_id")
    @JsonProperty("workout_profile_id")
    private String workoutProfileId;

    @Column(name = "diet_profile_id")
    @JsonProperty("diet_profile_id")
    private String dietProfileId;

    // Lazy-loaded profile relationships - temporarily disabled
    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "workout_profile_id", insertable = false, updatable = false)
    // @JsonIgnore
    // private WorkoutProfile workoutProfile;

    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "diet_profile_id", insertable = false, updatable = false)
    // @JsonIgnore
    // private DietProfile dietProfile;

    public enum Role {
        USER, ADMIN, PREMIUM
    }

    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(String username, String email, String password) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // UserDetails interface methods
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return isActive;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return isActive;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return isActive && emailVerified;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    // Getters and setters for physical profile
    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public WorkoutProfile.Gender getGender() {
        return gender;
    }

    public void setGender(WorkoutProfile.Gender gender) {
        this.gender = gender;
    }

    public WorkoutProfile.ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(WorkoutProfile.ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    // Getters and setters for profile references
    public String getWorkoutProfileId() {
        return workoutProfileId;
    }

    public void setWorkoutProfileId(String workoutProfileId) {
        this.workoutProfileId = workoutProfileId;
    }

    public String getDietProfileId() {
        return dietProfileId;
    }

    public void setDietProfileId(String dietProfileId) {
        this.dietProfileId = dietProfileId;
    }

    // public WorkoutProfile getWorkoutProfile() {
    //     return workoutProfile;
    // }

    // public void setWorkoutProfile(WorkoutProfile workoutProfile) {
    //     this.workoutProfile = workoutProfile;
    // }

    // public DietProfile getDietProfile() {
    //     return dietProfile;
    // }

    // public void setDietProfile(DietProfile dietProfile) {
    //     this.dietProfile = dietProfile;
    // }

    // Helper methods
    public boolean hasWorkoutProfile() {
        return workoutProfileId != null;
    }

    public boolean hasDietProfile() {
        return dietProfileId != null;
    }

    public boolean hasCompleteProfile() {
        return hasWorkoutProfile() && hasDietProfile();
    }
}