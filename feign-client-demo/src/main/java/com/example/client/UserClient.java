package com.example.client;

import com.example.model.UserProfile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

/**
 * Feign client for the User Service.
 *
 * All requests go through the API Gateway (port 8080).
 * The gateway validates the JWT and routes to user-service (port 8082).
 *
 * IMPORTANT: Every method requires an Authorization header:
 *   "Bearer eyJhbGciOiJIUzI1NiJ9..."
 *
 * Endpoints covered:
 *   GET    /api/users/profile    → current user's profile (any role)
 *   GET    /api/users            → all users              (any role)
 *   GET    /api/users/{id}       → user by id             (any role)
 *   GET    /api/admin/dashboard  → admin dashboard        (ROLE_ADMIN)
 *   GET    /api/admin/users      → all users with details (ROLE_ADMIN)
 *   DELETE /api/admin/users/{id} → delete a user          (ROLE_ADMIN)
 */
@FeignClient(name = "api-gateway", contextId = "userClient")
public interface UserClient {

    /**
     * Get the profile of the currently authenticated user.
     * The gateway reads the JWT and forwards X-Auth-Username/Role headers.
     *
     * @param bearerToken "Bearer eyJ..."
     */
    @GetMapping("/api/users/profile")
    Map<String, Object> getMyProfile(@RequestHeader("Authorization") String bearerToken);

    /**
     * Get all user profiles.
     *
     * @param bearerToken "Bearer eyJ..."
     */
    @GetMapping("/api/users")
    List<UserProfile> getAllUsers(@RequestHeader("Authorization") String bearerToken);

    /**
     * Get a specific user by ID.
     *
     * @param bearerToken "Bearer eyJ..."
     * @param id          user ID
     */
    @GetMapping("/api/users/{id}")
    UserProfile getUserById(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable("id") Long id);

    // ── Admin endpoints (ROLE_ADMIN only) ────────────────────────────────────

    /**
     * Get admin dashboard summary.
     * Returns 403 if the token does not have ROLE_ADMIN.
     *
     * @param bearerToken "Bearer eyJ..."
     */
    @GetMapping("/api/admin/dashboard")
    Map<String, Object> getAdminDashboard(@RequestHeader("Authorization") String bearerToken);

    /**
     * Get full user list with sensitive details (admin only).
     *
     * @param bearerToken "Bearer eyJ..."
     */
    @GetMapping("/api/admin/users")
    Map<String, Object> getAdminUsers(@RequestHeader("Authorization") String bearerToken);

    /**
     * Delete a user by ID (admin only).
     *
     * @param bearerToken "Bearer eyJ..."
     * @param id          user ID to delete
     */
    @DeleteMapping("/api/admin/users/{id}")
    Map<String, Object> deleteUser(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable("id") Long id);
}
