package com.example.tournament.mapper;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.response.MatchResponse;

@Component
public class MatchMapper {

    public MatchResponse toResponse(Match match) {
        Team a = match.getTeamA();
        Team b = match.getTeamB();
        Match next = match.getNextMatch();
        return new MatchResponse(
                match.getId(),
                match.getTournament().getId(),
                match.getRoundNumber(),
                match.getMatchNumber(),
                a == null ? null : a.getId(),
                a == null ? null : a.getName(),
                b == null ? null : b.getId(),
                b == null ? null : b.getName(),
                next == null ? null : next.getId(),
                match.getScheduledAt(),
                match.getStatus());
    }
}
