package com.example.controller;

import com.example.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints.
 * These routes are protected by both:
 *  1. JwtAuthenticationFilter (in the gateway)
 *  2. RoleAuthorizationFilter requiring ROLE_ADMIN (in the gateway)
 *
 * The user service itself also enforces security via Spring Security.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * GET /api/admin/dashboard
     * Admin dashboard summary (ROLE_ADMIN only).
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(
            @RequestHeader(value = "X-Auth-Username", required = false) String adminUser) {

        log.info("Admin dashboard accessed by: {}", adminUser);

        return ResponseEntity.ok(Map.of(
            "message", "Welcome to the Admin Dashboard!",
            "accessedBy", adminUser != null ? adminUser : "unknown",
            "totalUsers", 3,
            "totalRoles", List.of("ROLE_USER", "ROLE_ADMIN"),
            "systemStatus", "healthy"
        ));
    }

    /**
     * GET /api/admin/users
     * Full user list with sensitive details (ROLE_ADMIN only).
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsersAdmin(
            @RequestHeader(value = "X-Auth-Username", required = false) String adminUser) {

        log.info("Admin full user list requested by: {}", adminUser);

        List<Map<String, Object>> fullDetails = List.of(
            Map.of("id", 1, "username", "alice", "email", "alice@example.com",
                   "role", "ROLE_USER", "lastLogin", "2025-01-15T10:30:00", "active", true),
            Map.of("id", 2, "username", "bob", "email", "bob@example.com",
                   "role", "ROLE_USER", "lastLogin", "2025-01-14T09:15:00", "active", true),
            Map.of("id", 3, "username", "admin", "email", "admin@example.com",
                   "role", "ROLE_ADMIN", "lastLogin", "2025-01-15T14:00:00", "active", true)
        );

        return ResponseEntity.ok(Map.of(
            "requestedBy", adminUser,
            "users", fullDetails
        ));
    }

    /**
     * DELETE /api/admin/users/{id}
     * Delete a user (ROLE_ADMIN only).
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Username", required = false) String adminUser) {

        log.info("User {} deletion requested by admin: {}", id, adminUser);

        // In a real app, this would delete from the database
        return ResponseEntity.ok(Map.of(
            "message", "User with id=" + id + " has been deleted",
            "deletedBy", adminUser
        ));
    }
}
