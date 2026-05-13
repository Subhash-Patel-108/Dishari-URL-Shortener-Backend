package com.dishari.in.aspect;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;


public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        Object error
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(false, message, null, Instant.now(), error);
    }
}