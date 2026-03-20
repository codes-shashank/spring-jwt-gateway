package com.example.service;

import com.example.client.AuthClient;
import com.example.client.UserClient;
import com.example.config.TokenStore;
import com.example.model.AuthResponse;
import com.example.model.LoginRequest;
import com.example.model.RegisterRequest;
import com.example.model.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * DemoService orchestrates calls to the API Gateway via Feign clients.
 *
 * Demonstrates the full lifecycle:
 *   1. Register a new user
 *   2. Login (admin + regular user)
 *   3. Call user endpoints with a valid token
 *   4. Call admin endpoints with admin token
 *   5. Attempt admin endpoint with user token (expect 403)
 *   6. Validate a token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoService {

    private final AuthClient authClient;
    private final UserClient userClient;
    private final TokenStore tokenStore;

    // ── Auth Operations ───────────────────────────────────────────────────────

    /**
     * Register a brand new user via the auth service.
     */
    public AuthResponse register(String username, String password, String role) {
        log.info("=== Registering user: {} with role: {}", username, role);
        RegisterRequest request = new RegisterRequest(username, password, role);
        AuthResponse response = authClient.register(request);
        log.info("=== Registered: {} | token starts with: {}...",
                response.getUsername(), response.getToken().substring(0, 20));
        return response;
    }

    /**
     * Login and store the token in TokenStore for subsequent calls.
     */
    public AuthResponse login(String username, String password) {
        log.info("=== Logging in as: {}", username);
        AuthResponse response = authClient.login(new LoginRequest(username, password));
        tokenStore.store(response.getToken());
        log.info("=== Login successful: {} | role: {}", response.getUsername(), response.getRole());
        return response;
    }

    /**
     * Validate the currently stored token.
     */
    public Map<String, Object> validateCurrentToken() {
        log.info("=== Validating current token...");
        Map<String, Object> result = authClient.validateToken(tokenStore.getToken());
        log.info("=== Token valid: {}", result.get("valid"));
        return result;
    }

    // ── User Operations ───────────────────────────────────────────────────────

    /**
     * Get the profile of whoever is currently logged in.
     * Uses token automatically via FeignAuthInterceptor.
     */
    public Map<String, Object> getMyProfile() {
        log.info("=== Fetching my profile...");
        Map<String, Object> profile = userClient.getMyProfile(tokenStore.getBearerToken());
        log.info("=== Profile: {}", profile);
        return profile;
    }

    /**
     * Get all user profiles (any authenticated user).
     */
    public List<UserProfile> getAllUsers() {
        log.info("=== Fetching all users...");
        List<UserProfile> users = userClient.getAllUsers(tokenStore.getBearerToken());
        log.info("=== Found {} users", users.size());
        users.forEach(u -> log.info("    - {} | {} | {}", u.getUsername(), u.getRole(), u.getDepartment()));
        return users;
    }

    /**
     * Get a specific user by ID.
     */
    public UserProfile getUserById(Long id) {
        log.info("=== Fetching user with id: {}", id);
        UserProfile user = userClient.getUserById(tokenStore.getBearerToken(), id);
        log.info("=== Found: {} ({})", user.getUsername(), user.getEmail());
        return user;
    }

    // ── Admin Operations ──────────────────────────────────────────────────────

    /**
     * Get admin dashboard. Requires ROLE_ADMIN token in TokenStore.
     */
    public Map<String, Object> getAdminDashboard() {
        log.info("=== Fetching admin dashboard...");
        Map<String, Object> dashboard = userClient.getAdminDashboard(tokenStore.getBearerToken());
        log.info("=== Dashboard: {}", dashboard);
        return dashboard;
    }

    /**
     * Get full user list (admin only).
     */
    public Map<String, Object> getAdminUsers() {
        log.info("=== Fetching admin user list...");
        Map<String, Object> result = userClient.getAdminUsers(tokenStore.getBearerToken());
        log.info("=== Admin users result: {}", result);
        return result;
    }

    /**
     * Delete a user (admin only).
     */
    public Map<String, Object> deleteUser(Long id) {
        log.info("=== Deleting user with id: {}", id);
        Map<String, Object> result = userClient.deleteUser(tokenStore.getBearerToken(), id);
        log.info("=== Delete result: {}", result);
        return result;
    }

    /**
     * Attempt an admin endpoint using a NON-admin token.
     * Demonstrates that the gateway correctly returns 403.
     */
    public String attemptAdminWithUserToken(String userToken) {
        log.info("=== Attempting admin endpoint with ROLE_USER token (expect 403)...");
        try {
            userClient.getAdminDashboard("Bearer " + userToken);
            return "ERROR: Should have been rejected!";
        } catch (RuntimeException e) {
            log.warn("=== Correctly rejected: {}", e.getMessage());
            return "Correctly rejected: " + e.getMessage();
        }
    }
}
