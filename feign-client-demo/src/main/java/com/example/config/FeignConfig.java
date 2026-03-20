package com.example.config;

import feign.Logger;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global Feign configuration.
 *
 * Sets logging level to FULL so you can see exactly what
 * request/response goes over the wire — very useful for debugging.
 *
 * Logging levels:
 *   NONE    → no logging (default, use in production)
 *   BASIC   → method, URL, status, time
 *   HEADERS → + request/response headers
 *   FULL    → + request/response body (use in development)
 */
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Custom error decoder — converts HTTP error responses into
     * meaningful Java exceptions instead of generic FeignException.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomFeignErrorDecoder();
    }

    public static class CustomFeignErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            switch (response.status()) {
                case 401:
                    return new RuntimeException(
                        "Unauthorized — token is missing, invalid, or expired. " +
                        "Method: " + methodKey);
                case 403:
                    return new RuntimeException(
                        "Forbidden — you don't have the required role for: " + methodKey);
                case 404:
                    return new RuntimeException(
                        "Not Found — resource does not exist. Method: " + methodKey);
                default:
                    return defaultDecoder.decode(methodKey, response);
            }
        }
    }
}
