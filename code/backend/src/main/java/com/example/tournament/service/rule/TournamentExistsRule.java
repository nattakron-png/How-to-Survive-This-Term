
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.TournamentRepository;

@Component
public class TournamentExistsRule implements TeamJoinRule {

    private final TournamentRepository tournamentRepository;
    private TeamJoinRule nextRule;

    public TournamentExistsRule(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Override
    public void validate(Team team, Tournament tournament) {
        if (!tournamentRepository.existsById(tournament.getId())) {
            throw new ResourceNotFoundException(
                "Tournament not found: " + tournament.getId()
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