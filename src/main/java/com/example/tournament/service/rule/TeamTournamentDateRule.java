
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.TournamentTeamRepository;

@Component
public class TeamTournamentDateRule implements TeamJoinRule {

    private final TournamentTeamRepository tournamentTeamRepository;
    private TeamJoinRule nextRule;

    public TeamTournamentDateRule(
            TournamentTeamRepository tournamentTeamRepository) {
        this.tournamentTeamRepository = tournamentTeamRepository;
    }

    @Override
    public void validate(Team team, Tournament tournament) {
        long overlappingTournaments =
                tournamentTeamRepository.countOverlappingTournaments(
                        team.getId(),
                        tournament.getId(),
                        tournament.getStartDate(),
                        tournament.getEndDate());

        if (overlappingTournaments > 0) {
            throw new BusinessException(
                    "Team already joined another tournament with overlapping dates");
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