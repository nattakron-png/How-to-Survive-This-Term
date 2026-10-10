
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.TournamentTeamRepository;

@Component
public class TeamPointsLimitRule implements TeamJoinRule {

    private final TournamentTeamRepository tournamentTeamRepository;
    private TeamJoinRule nextRule;

    public TeamPointsLimitRule(
            TournamentTeamRepository tournamentTeamRepository) {
        this.tournamentTeamRepository = tournamentTeamRepository;
    }

    @Override
    public void validate(Team team, Tournament tournament) {
        if (tournament.getFormat() == TournamentFormat.POINTS) {
            long currentTeams =
                    tournamentTeamRepository.countByTournamentId(
                            tournament.getId());

            if (currentTeams >= 12) {
                throw new BusinessException(
                        "POINTS tournaments cannot have more than 12 teams");
            }
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