package com.support.agent.common.api;

import java.time.Instant;
import org.slf4j.MDC;

/**
 * Uniform response envelope returned by every REST endpoint.
 */
public record ApiResponse<T>(int code, String message, T data, String traceId, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, currentTraceId(), Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(0, "created", data, currentTraceId(), Instant.now());
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, currentTraceId(), Instant.now());
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
