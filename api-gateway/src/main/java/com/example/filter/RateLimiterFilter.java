package com.example.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global rate-limiting filter.
 *
 * Uses an in-memory token-bucket algorithm to limit each client IP
 * to a maximum of {@value #MAX_REQUESTS_PER_SECOND} requests per second.
 * Returns HTTP 429 (Too Many Requests) when the limit is exceeded.
 */
@Slf4j
@Component
public class RateLimiterFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_SECOND = 2;

    private final ConcurrentHashMap<String, ClientBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange);
        ClientBucket bucket = buckets.computeIfAbsent(clientIp, k -> new ClientBucket());

        if (bucket.tryConsume()) {
            return chain.filter(exchange);
        }

        log.warn("[RATE-LIMIT] 429 Too Many Requests for client IP: {}", clientIp);
        return onError(exchange);
    }

    @Override
    public int getOrder() {
        // Run right after RequestLoggingFilter (HIGHEST_PRECEDENCE)
        // but before any authentication filters
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For first for proxied requests
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            InetAddress address = remoteAddress.getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
        }
        return "unknown";
    }

    private Mono<Void> onError(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        var body = response.bufferFactory().wrap(
                "{\"error\": \"Rate limit exceeded. Maximum 2 requests per second allowed.\"}".getBytes()
        );
        return response.writeWith(Mono.just(body));
    }

    /**
     * Simple token-bucket that refills to {@link #MAX_REQUESTS_PER_SECOND}
     * tokens every second.
     */
    private static class ClientBucket {
        private final AtomicLong lastRefillTimestamp = new AtomicLong(System.nanoTime());
        private final AtomicInteger tokens = new AtomicInteger(MAX_REQUESTS_PER_SECOND);

        boolean tryConsume() {
            refill();
            while (true) {
                int current = tokens.get();
                if (current <= 0) {
                    return false;
                }
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
            }
        }

        private void refill() {
            long now = System.nanoTime();
            long last = lastRefillTimestamp.get();
            long elapsedNanos = now - last;

            if (elapsedNanos >= 1_000_000_000L) {
                // At least one second has elapsed; refill bucket
                if (lastRefillTimestamp.compareAndSet(last, now)) {
                    tokens.set(MAX_REQUESTS_PER_SECOND);
                }
            }
        }
    }
}
