package com.example.client;

import com.example.model.AuthResponse;
import com.example.model.LoginRequest;
import com.example.model.RegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign client for the Auth Service.
 *
 * All requests go through the API Gateway (port 8080).
 * url is injected from application.yml → feign.gateway-url
 *
 * Endpoints covered:
 *   POST /auth/register  → register a new user
 *   POST /auth/login     → authenticate and receive JWT
 *   GET  /auth/validate  → validate an existing token
 */
@FeignClient(name = "api-gateway", contextId = "authClient")
public interface AuthClient {

    /**
     * Register a new user account.
     *
     * @param request username, password, role
     * @return AuthResponse containing the JWT token
     */
    @PostMapping("/auth/register")
    AuthResponse register(@RequestBody RegisterRequest request);

    /**
     * Login with existing credentials.
     *
     * @param request username and password
     * @return AuthResponse containing the JWT token
     */
    @PostMapping("/auth/login")
    AuthResponse login(@RequestBody LoginRequest request);

    /**
     * Validate an existing JWT token.
     *
     * @param token the raw JWT string
     * @return Map with { "valid": true/false }
     */
    @GetMapping("/auth/validate")
    Map<String, Object> validateToken(@RequestParam("token") String token);
}
