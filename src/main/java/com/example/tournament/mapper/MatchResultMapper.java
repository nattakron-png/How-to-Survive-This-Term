package com.example.tournament.mapper;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.MatchResult;
import com.example.tournament.dto.response.MatchResultResponse;

@Component
public class MatchResultMapper {

    public MatchResultResponse toResponse(MatchResult result) {
        Match match = result.getMatch();
        return new MatchResultResponse(
                result.getId(),
                match.getId(),
                match.getTeamA().getId(),
                result.getTeamAScore(),
                match.getTeamB().getId(),
                result.getTeamBScore(),
                result.getWinnerTeam().getId(),
                result.getCreatedAt());
    }
}