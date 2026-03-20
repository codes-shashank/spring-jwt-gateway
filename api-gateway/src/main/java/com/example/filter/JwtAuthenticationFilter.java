package com.example.filter;

import com.example.config.JwtValidator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT Authentication Gateway Filter.
 *
 * Applied to protected routes. Validates the Bearer token in the
 * Authorization header, then forwards claims (username, role) as
 * downstream request headers so microservices can trust them without
 * re-validating the JWT themselves.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        super(Config.class);
        this.jwtValidator = jwtValidator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header for: {}", request.getPath());
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization format. Expected: Bearer <token>", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = jwtValidator.validateAndExtractClaims(token);

                if (jwtValidator.isTokenExpired(claims)) {
                    return onError(exchange, "Token has expired", HttpStatus.UNAUTHORIZED);
                }

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                log.debug("Authenticated request: user={}, role={}, path={}", username, role, request.getPath());

                // Forward user info as headers to downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-Auth-Username", username)
                        .header("X-Auth-Role", role != null ? role : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        var body = response.bufferFactory().wrap(
                ("{\"error\": \"" + message + "\"}").getBytes()
        );
        return response.writeWith(Mono.just(body));
    }

    public static class Config {
        // Place configuration properties here if needed
    }
}
