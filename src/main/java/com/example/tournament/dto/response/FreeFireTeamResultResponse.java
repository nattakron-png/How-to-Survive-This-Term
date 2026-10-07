package com.example.tournament.dto.response;

public record FreeFireTeamResultResponse(
        Long teamId,
        String teamName,
        Integer placement,
        Integer kills,
        Integer placementPoints,
        Integer killPoints,
        Integer totalPoints) {
}