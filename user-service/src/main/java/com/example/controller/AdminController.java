package com.example.controller;

import com.example.model.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin", description = "Admin only operations — requires ROLE_ADMIN")
public class AdminController {

    /**
     * GET /api/admin/dashboard
     * Admin dashboard summary (ROLE_ADMIN only).
     */
    @Operation(summary = "Admin dashboard summary",
            security = @SecurityRequirement(name = "Bearer Token"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard data"),
            @ApiResponse(responseCode = "403", description = "Access denied — ROLE_ADMIN required")
    })
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
    @Operation(summary = "Get all users with full details",
            security = @SecurityRequirement(name = "Bearer Token"))
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
    @Operation(summary = "Delete a user by ID",
            security = @SecurityRequirement(name = "Bearer Token"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied — ROLE_ADMIN required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
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
