package com.example.tournament.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FreeFireTeamResultRequest(
        @NotNull Long teamId,
        @NotNull @Min(1) Integer placement,
        @NotNull @Min(0) Integer kills) {
}