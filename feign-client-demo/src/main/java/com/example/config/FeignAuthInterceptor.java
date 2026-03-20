package com.example.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Feign Request Interceptor — automatically attaches the JWT token
 * to the Authorization header of every outgoing Feign request.
 *
 * This means you don't need to manually pass the token to every
 * Feign client method call. The interceptor adds it behind the scenes.
 *
 * NOTE: For endpoints that explicitly pass @RequestHeader("Authorization")
 * (like UserClient), the manually passed value takes precedence over
 * what this interceptor sets, so there's no conflict.
 *
 * Flow:
 *   1. DemoService calls authClient.login() → token stored in TokenStore
 *   2. DemoService calls userClient.getAllUsers() (no token param)
 *   3. This interceptor runs → reads token from TokenStore → adds header
 *   4. Feign sends: GET /api/users  Authorization: Bearer eyJ...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignAuthInterceptor implements RequestInterceptor {

    private final TokenStore tokenStore;

    @Override
    public void apply(RequestTemplate template) {
        if (tokenStore.hasToken()) {
            // Only add if not already set by the caller
            if (!template.headers().containsKey("Authorization")) {
                log.debug("FeignAuthInterceptor: attaching token to {}", template.url());
                template.header("Authorization", tokenStore.getBearerToken());
            }
        }
    }
}
