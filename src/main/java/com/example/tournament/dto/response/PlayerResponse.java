package com.example.tournament.dto.response;

import java.time.LocalDateTime;

public record PlayerResponse(
        Long id,
        String name,
        String role,
        String description,
        Long teamId,
        LocalDateTime createdAt) {
}
