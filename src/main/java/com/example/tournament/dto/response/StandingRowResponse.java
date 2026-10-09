package com.example.tournament.dto.response;

import java.util.List;

public record StandingRowResponse(
        int rank,
        Long teamId,
        String teamName,
        int totalPoints,
        int booyahs,
        int kills,
        List<Integer> pointsPerGame) {
}