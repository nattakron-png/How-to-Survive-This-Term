package com.example.tournament.service.rule;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;

@Component
public class TeamJoinRuleChain {

    private final TeamExistsRule teamExistsRule;

    public TeamJoinRuleChain(
            TeamExistsRule teamExistsRule,
            TournamentExistsRule tournamentExistsRule,
            TournamentStatusRule tournamentStatusRule,
            TeamNotAlreadyJoinedRule teamNotAlreadyJoinedRule,
            TeamGameMatchRule teamGameMatchRule,
            TeamMinimumPlayersRule teamMinimumPlayersRule,
            TeamPointsLimitRule teamPointsLimitRule,
            TeamTournamentDateRule teamTournamentDateRule) {

        this.teamExistsRule = teamExistsRule;

        teamExistsRule.setNext(tournamentExistsRule);
        tournamentExistsRule.setNext(tournamentStatusRule);
        tournamentStatusRule.setNext(teamNotAlreadyJoinedRule);
        teamNotAlreadyJoinedRule.setNext(teamGameMatchRule);
        teamGameMatchRule.setNext(teamMinimumPlayersRule);
        teamMinimumPlayersRule.setNext(teamPointsLimitRule);
        teamPointsLimitRule.setNext(teamTournamentDateRule);
    }

    public void validate(Team team, Tournament tournament) {
        teamExistsRule.validate(team, tournament);
    }
}