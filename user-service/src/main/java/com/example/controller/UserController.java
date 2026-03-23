package com.example.controller;

import com.example.model.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User profile endpoints.
 *
 * The API Gateway validates the JWT and forwards:
 *   X-Auth-Username  → authenticated user's username
 *   X-Auth-Role      → authenticated user's role
 *
 * This service trusts those headers (assumes gateway is the entry point).
 * For extra safety, this service also has its own JWT filter as a second layer.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile operations")
public class UserController {

    // In-memory sample data
    private static final List<UserProfile> USERS = List.of(
        UserProfile.builder().id(1L).username("alice").email("alice@example.com").role("ROLE_USER").department("Engineering").build(),
        UserProfile.builder().id(2L).username("bob").email("bob@example.com").role("ROLE_USER").department("Marketing").build(),
        UserProfile.builder().id(3L).username("admin").email("admin@example.com").role("ROLE_ADMIN").department("IT").build()
    );

    /**
     * GET /api/users/profile
     * Returns the current authenticated user's profile (from gateway headers).
     */
    @Operation(summary = "Get current user profile",
            security = @SecurityRequirement(name = "Bearer Token"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Token missing or invalid")
    })
    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(
            @RequestHeader(value = "X-Auth-Username", required = false) String username,
            @RequestHeader(value = "X-Auth-Role", required = false) String role) {

        log.info("Profile request from user: {}, role: {}", username, role);

        if (username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No authenticated user context"));
        }

        return ResponseEntity.ok(Map.of(
            "username", username,
            "role", role != null ? role : "unknown",
            "message", "Hello, " + username + "! Your role is: " + role
        ));
    }

    /**
     * GET /api/users
     * Returns all user profiles (any authenticated user).
     */
    @Operation(summary = "Get all users",
            security = @SecurityRequirement(name = "Bearer Token"))
    @ApiResponse(responseCode = "200", description = "List of all users")
    @GetMapping
    public ResponseEntity<List<UserProfile>> getAllUsers(
            @RequestHeader(value = "X-Auth-Username", required = false) String requestingUser) {
        log.info("User list requested by: {}", requestingUser);
        return ResponseEntity.ok(USERS);
    }

    /**
     * GET /api/users/{id}
     * Returns a specific user by ID.
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Auth-Username", required = false) String requestingUser) {

        return USERS.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
