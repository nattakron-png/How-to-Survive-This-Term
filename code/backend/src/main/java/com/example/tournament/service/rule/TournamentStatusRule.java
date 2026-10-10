
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.MatchRepository;

@Component
public class TournamentStatusRule implements TeamJoinRule {

    private final MatchRepository matchRepository;
    private TeamJoinRule nextRule;

    public TournamentStatusRule(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Override
    public void validate(Team team, Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new BusinessException(
                "Teams can only join an upcoming tournament"
            );
        }

        if (matchRepository.existsByTournamentId(tournament.getId())) {
            throw new BusinessException(
                "Cannot join a tournament after its schedule or bracket has been created"
            );
        }

        if (nextRule != null) {
            nextRule.validate(team, tournament);
        }
    }

    @Override
    public void setNext(TeamJoinRule nextRule) {
        this.nextRule = nextRule;
    }
}