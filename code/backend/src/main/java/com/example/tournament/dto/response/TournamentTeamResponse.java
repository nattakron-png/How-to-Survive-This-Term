package com.example.tournament.dto.response;

import java.time.LocalDateTime;

// ข้อมูลทีมที่ส่งกลับจาก API
public record TournamentTeamResponse(
        Long tournamentId,
        Long teamId,
        String teamName,
        LocalDateTime joinedAt,
        Integer seed) {
}