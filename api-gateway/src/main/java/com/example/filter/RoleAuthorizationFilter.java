package com.example.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Role Authorization Filter.
 *
 * Applied after JWT authentication. Checks that the X-Auth-Role header
 * (set by JwtAuthenticationFilter) matches the required role for this route.
 *
 * Usage in application.yml:
 *   filters:
 *     - name: RoleAuthorizationFilter
 *       args:
 *         requiredRole: ROLE_ADMIN
 */
@Slf4j
@Component
public class RoleAuthorizationFilter extends AbstractGatewayFilterFactory<RoleAuthorizationFilter.Config> {

    public RoleAuthorizationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String userRole = request.getHeaders().getFirst("X-Auth-Role");

            if (userRole == null || userRole.isBlank()) {
                return onError(exchange, "No role information found", HttpStatus.FORBIDDEN);
            }

            if (!userRole.equals(config.getRequiredRole())) {
                log.warn("Access denied: user has role '{}' but '{}' is required for path {}",
                        userRole, config.getRequiredRole(), request.getPath());
                return onError(exchange, "Access denied. Required role: " + config.getRequiredRole(), HttpStatus.FORBIDDEN);
            }

            log.debug("Role check passed: user role '{}' matches required '{}'", userRole, config.getRequiredRole());
            return chain.filter(exchange);
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
        private String requiredRole;

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }
    }
}
