package com.example.tournament.dto.response;

import java.util.List;

public record FreeFireGameResultsResponse(
        Long gameId,
        Long tournamentId,
        Integer gameNumber,
        String status,
        List<FreeFireTeamResultResponse> results) {
}