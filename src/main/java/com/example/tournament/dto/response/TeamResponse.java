package com.example.tournament.dto.response;

import java.time.LocalDateTime;

public record TeamResponse(
        Long id,
        String name,
        String description,
        String logoUrl,
        LocalDateTime createdAt) {
}
