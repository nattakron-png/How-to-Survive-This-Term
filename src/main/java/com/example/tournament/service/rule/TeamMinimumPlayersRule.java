
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.PlayerRepository;

@Component
public class TeamMinimumPlayersRule implements TeamJoinRule {

    private final PlayerRepository playerRepository;
    private TeamJoinRule nextRule;

    public TeamMinimumPlayersRule(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public void validate(Team team, Tournament tournament) {
        Integer minimumPlayers = tournament.getGame().getMinPlayers();

        if (minimumPlayers != null) {
            long actualPlayers = playerRepository.countByTeamId(team.getId());

            if (actualPlayers < minimumPlayers) {
                throw new BusinessException(
                        "Team must have at least " + minimumPlayers
                                + " players to join this tournament");
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