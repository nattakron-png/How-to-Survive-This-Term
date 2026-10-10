package com.example.tournament.dto.response;

import java.time.LocalDateTime;

public record TeamResponse(
        Long id,
        String name,
        String description,
        Long gameId,
        String logoUrl,
        LocalDateTime createdAt) {
}
