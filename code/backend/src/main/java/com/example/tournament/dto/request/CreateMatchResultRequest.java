package com.example.tournament.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateMatchResultRequest(
        @NotNull @Min(0) Integer teamAScore,
        @NotNull @Min(0) Integer teamBScore,
        @NotNull Long winnerTeamId) {
}