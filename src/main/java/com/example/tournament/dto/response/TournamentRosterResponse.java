package com.example.tournament.dto.response;

import java.util.List;

public record TournamentRosterResponse(
        Long tournamentId,
        Long teamId,
        String teamName,
        String teamDescription,
        String teamLogoUrl,
        List<PlayerAtRegistration> players) {

    public record PlayerAtRegistration(Long playerId, String name, String role) {
    }
}
