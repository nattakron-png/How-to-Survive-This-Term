package com.example.tournament.dto.response;

public record TeamPlayerResponse(
        Long id,
        String name,
        String role,
        String description,
        Long teamId) {
}
