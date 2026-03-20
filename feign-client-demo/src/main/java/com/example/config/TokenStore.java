package com.example.config;

import org.springframework.stereotype.Component;

/**
 * Simple in-memory token holder.
 *
 * After login, the JWT is stored here so the RequestInterceptor
 * and DemoService can access it without passing it around manually.
 *
 * In a real app this would be per-request (ThreadLocal) or
 * stored securely per user session.
 */
@Component
public class TokenStore {

    private String token;

    public void store(String token) {
        this.token = token;
        System.out.println(">>> Token stored: " + token.substring(0, 20) + "...");
    }

    public String getToken() {
        return token;
    }

    public String getBearerToken() {
        return "Bearer " + token;
    }

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }

    public void clear() {
        this.token = null;
    }
}
