package com.example.tournament.exception;

import java.time.OffsetDateTime;

public record ApiError(String message, OffsetDateTime timestamp) {

    public static ApiError of(String message) {
        return new ApiError(message, OffsetDateTime.now());
    }
}
