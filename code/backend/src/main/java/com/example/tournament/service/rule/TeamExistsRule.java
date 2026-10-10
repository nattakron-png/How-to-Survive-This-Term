
package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.TeamRepository;

@Component
public class TeamExistsRule implements TeamJoinRule {

    private final TeamRepository teamRepository;
    private TeamJoinRule nextRule;

    public TeamExistsRule(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Override
    public void validate(Team team, Tournament tournament) {
        if (!teamRepository.existsById(team.getId())) {
            throw new ResourceNotFoundException(
                "Team not found: " + team.getId()
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