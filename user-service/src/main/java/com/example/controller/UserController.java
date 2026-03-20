package com.example.controller;

import com.example.model.UserProfile;
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
