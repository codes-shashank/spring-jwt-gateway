package com.example.controller;

import com.example.model.AuthResponse;
import com.example.model.UserProfile;
import com.example.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DemoController exposes simple endpoints so you can trigger
 * every Feign client call from Talend / Postman / browser.
 *
 * All endpoints on this service run on port 8083.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Auth endpoints (no token needed)                               │
 * │  POST /demo/register         → register a new user              │
 * │  POST /demo/login            → login, stores token internally   │
 * │  GET  /demo/validate         → validate stored token            │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  User endpoints (token auto-attached by FeignAuthInterceptor)   │
 * │  GET  /demo/profile          → current user's profile           │
 * │  GET  /demo/users            → all users                        │
 * │  GET  /demo/users/{id}       → user by id                       │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Admin endpoints (must be logged in as ROLE_ADMIN)              │
 * │  GET  /demo/admin/dashboard  → admin dashboard                  │
 * │  GET  /demo/admin/users      → admin user list                  │
 * │  DELETE /demo/admin/users/{id} → delete user                    │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  Full scenario (runs everything in sequence)                    │
 * │  GET  /demo/run-all          → runs the complete demo           │
 * └─────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    // ── Auth ──────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "role", defaultValue = "ROLE_USER") String role) {
        return ResponseEntity.ok(demoService.register(username, password, role));
    }

    /**
     * Login and store the token internally.
     * After calling this, all subsequent /demo/** calls use this token automatically.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        return ResponseEntity.ok(demoService.login(username, password));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        return ResponseEntity.ok(demoService.validateCurrentToken());
    }

    // ── User ──────────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile() {
        return ResponseEntity.ok(demoService.getMyProfile());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserProfile>> users() {
        return ResponseEntity.ok(demoService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfile> userById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(demoService.getUserById(id));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(demoService.getAdminDashboard());
    }

    @GetMapping("/admin/users")
    public ResponseEntity<Map<String, Object>> adminUsers() {
        return ResponseEntity.ok(demoService.getAdminUsers());
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable("id") Long id) {
        return ResponseEntity.ok(demoService.deleteUser(id));
    }

    // ── Full Scenario ─────────────────────────────────────────────────────────

    /**
     * GET /demo/run-all
     *
     * Runs the complete demo scenario in sequence and returns a summary:
     *   1. Login as admin
     *   2. Get profile
     *   3. Get all users
     *   4. Get user by id
     *   5. Admin dashboard
     *   6. Admin user list
     *   7. Login as regular user
     *   8. Try admin endpoint with ROLE_USER (expect rejection)
     */
    @GetMapping("/run-all")
    public ResponseEntity<Map<String, Object>> runAll() {
        Map<String, Object> results = new LinkedHashMap<>();

        // Step 1 — Login as admin
        AuthResponse adminLogin = demoService.login("admin", "admin123");
        results.put("1_admin_login", Map.of(
                "username", adminLogin.getUsername(),
                "role", adminLogin.getRole(),
                "token_preview", adminLogin.getToken().substring(0, 20) + "..."
        ));

        // Step 2 — Get profile
        results.put("2_my_profile", demoService.getMyProfile());

        // Step 3 — Get all users
        List<UserProfile> users = demoService.getAllUsers();
        results.put("3_all_users_count", users.size());

        // Step 4 — Get user by ID
        results.put("4_user_by_id_1", demoService.getUserById(1L));

        // Step 5 — Admin dashboard
        results.put("5_admin_dashboard", demoService.getAdminDashboard());

        // Step 6 — Admin user list
        results.put("6_admin_users", demoService.getAdminUsers());

        // Step 7 — Login as regular user
        AuthResponse userLogin = demoService.login("user", "user123");
        results.put("7_user_login", Map.of(
                "username", userLogin.getUsername(),
                "role", userLogin.getRole()
        ));

        // Step 8 — Attempt admin endpoint with ROLE_USER token (expect 403)
        results.put("8_admin_with_user_token",
                demoService.attemptAdminWithUserToken(userLogin.getToken()));

        return ResponseEntity.ok(results);
    }
}
