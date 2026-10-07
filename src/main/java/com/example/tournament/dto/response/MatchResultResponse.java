package com.example.tournament.dto.response;

import java.time.LocalDateTime;

public record MatchResultResponse(
        Long id,
        Long matchId,
        Long teamAId,
        Integer teamAScore,
        Long teamBId,
        Integer teamBScore,
        Long winnerTeamId,
        LocalDateTime createdAt) {
}