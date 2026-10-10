
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.BusinessException;

@Component
public class TeamGameMatchRule implements TeamJoinRule {

    private TeamJoinRule nextRule;

    @Override
    public void validate(Team team, Tournament tournament) {
        if (team.getGame() == null || tournament.getGame() == null
                || !team.getGame().getId().equals(tournament.getGame().getId())) {
            throw new BusinessException(
                    "Team's game must match the tournament's game");
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