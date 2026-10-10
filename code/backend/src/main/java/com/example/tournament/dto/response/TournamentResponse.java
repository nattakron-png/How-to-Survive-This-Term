package com.example.tournament.dto.response;

import java.time.LocalDate;

import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.domain.enums.TournamentStatus;

public record TournamentResponse(
        Long id,
        String name,
        String description,
        Long gameId,
        TournamentFormat format,
        Short totalGames,
        Short pointsPerKill,
        String logoUrl,
        LocalDate startDate,
        LocalDate endDate,
        TournamentStatus status) {
}