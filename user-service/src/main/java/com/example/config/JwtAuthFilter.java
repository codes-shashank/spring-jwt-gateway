package com.example.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.List;

/**
 * JWT filter for User Service.
 *
 * This service sits behind the API Gateway. Normally the gateway validates
 * the JWT and forwards X-Auth-Username / X-Auth-Role headers. However, this
 * filter provides a defense-in-depth second layer of JWT validation, and also
 * sets the Spring Security context so @PreAuthorize annotations work.
 *
 * Strategy: trust the gateway headers (fast path) OR fall back to validating
 * the raw JWT from the Authorization header if present.
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Fast path: trust gateway-forwarded headers
        String username = request.getHeader("X-Auth-Username");
        String role = request.getHeader("X-Auth-Role");

        if (username != null && !username.isBlank() && role != null && !role.isBlank()) {
            log.debug("Trusting gateway headers: user={}, role={}", username, role);
            setSecurityContext(username, role);
            filterChain.doFilter(request, response);
            return;
        }

        // Fallback: validate raw JWT from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                username = claims.getSubject();
                role = claims.get("role", String.class);

                if (username != null) {
                    setSecurityContext(username, role);
                }
            } catch (Exception e) {
                log.warn("JWT validation failed in user-service filter: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setSecurityContext(String username, String role) {
        List<SimpleGrantedAuthority> authorities = role != null
                ? List.of(new SimpleGrantedAuthority(role))
                : List.of();

        var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
