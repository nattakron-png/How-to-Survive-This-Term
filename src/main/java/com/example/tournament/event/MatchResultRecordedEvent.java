package com.example.tournament.event;

public record MatchResultRecordedEvent(Long matchId, Long winnerTeamId) {
}