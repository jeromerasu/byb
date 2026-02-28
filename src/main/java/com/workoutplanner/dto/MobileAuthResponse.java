package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workoutplanner.model.User;

import java.time.LocalDateTime;

public class MobileAuthResponse {

    private boolean success;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    private User user;

    private String message;

    @JsonProperty("server_time")
    private LocalDateTime serverTime;

    @JsonProperty("requires_email_verification")
    private boolean requiresEmailVerification;

    @JsonProperty("is_first_login")
    private boolean isFirstLogin;

    public MobileAuthResponse() {
        this.serverTime = LocalDateTime.now();
    }

    public MobileAuthResponse(String accessToken, String refreshToken, Long expiresIn, User user) {
        this();
        this.success = true;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.expiresAt = LocalDateTime.now().plusSeconds(expiresIn / 1000);
        this.user = user;
        this.message = "Authentication successful";
        this.requiresEmailVerification = !user.isEmailVerified();
        this.isFirstLogin = user.getLastLogin() == null;
    }

    public MobileAuthResponse(String message) {
        this();
        this.success = false;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public boolean isRequiresEmailVerification() {
        return requiresEmailVerification;
    }

    public void setRequiresEmailVerification(boolean requiresEmailVerification) {
        this.requiresEmailVerification = requiresEmailVerification;
    }

    public boolean isFirstLogin() {
        return isFirstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        isFirstLogin = firstLogin;
    }
}