package com.example.tournament.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddTournamentTeamRequest(@NotNull Long teamId) {
}
