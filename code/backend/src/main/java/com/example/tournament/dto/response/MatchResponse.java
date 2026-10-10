package com.example.tournament.dto.response;

import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        Long tournamentId,
        Integer roundNumber,
        Integer matchNumber,
        Long teamAId,
        String teamAName,
        Long teamBId,
        String teamBName,
        Long nextMatchId,
        LocalDateTime scheduledAt,
        String status) {
}
