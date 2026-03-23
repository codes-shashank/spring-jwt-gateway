package com.example.controller;

import com.example.model.AuthModels.*;
import com.example.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login and token validation")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/register
     * Register a new user account.
     *
     * Body: { "username": "alice", "password": "secret", "role": "ROLE_USER" }
     */
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Username already taken")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /auth/login
     * Authenticate and receive a JWT token.
     *
     * Body: { "username": "alice", "password": "secret" }
     */
    @Operation(summary = "Login and receive JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /auth/validate?token=...
     * Used internally by the API Gateway to validate tokens.
     */
    @Operation(summary = "Validate an existing JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam("token") String token) {
        boolean valid = authService.validateToken(token);
        if (valid) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
    }
}
