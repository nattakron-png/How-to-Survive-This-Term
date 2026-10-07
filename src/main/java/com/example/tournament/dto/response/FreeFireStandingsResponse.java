package com.example.tournament.dto.response;

import java.util.List;

public record FreeFireStandingsResponse(
        Long tournamentId,
        Integer totalGames,
        int gamesCompleted,
        List<StandingRowResponse> standings) {
}