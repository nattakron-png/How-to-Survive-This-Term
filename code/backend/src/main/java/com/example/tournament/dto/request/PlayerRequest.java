package com.example.tournament.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String role,
        String description,
        Long teamId) {
}
