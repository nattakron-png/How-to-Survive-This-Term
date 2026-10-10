package com.example.tournament.dto.response;

import java.time.LocalDateTime;

public record FreeFireGameSummaryResponse(
        Long id,
        int gameNumber,
        LocalDateTime scheduledAt,
        String status,
        Long booyahTeamId,
        String booyahTeamName) {
}