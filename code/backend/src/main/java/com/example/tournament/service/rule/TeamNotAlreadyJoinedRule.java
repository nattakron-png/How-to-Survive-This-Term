
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.TournamentTeamRepository;

@Component
public class TeamNotAlreadyJoinedRule implements TeamJoinRule {

    private final TournamentTeamRepository tournamentTeamRepository;
    private TeamJoinRule nextRule;

    public TeamNotAlreadyJoinedRule(
            TournamentTeamRepository tournamentTeamRepository) {
        this.tournamentTeamRepository = tournamentTeamRepository;
    }

    @Override
    public void validate(Team team, Tournament tournament) {
        boolean alreadyJoined =
                tournamentTeamRepository.existsById(
                        new com.example.tournament.domain.entity.TournamentTeamId(
                                tournament.getId(), team.getId()));

        if (alreadyJoined) {
            throw new BusinessException(
                    "Team has already joined this tournament");
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