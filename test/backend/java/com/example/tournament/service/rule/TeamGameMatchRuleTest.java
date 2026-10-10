package com.example.tournament.service.rule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.tournament.domain.entity.Game;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.domain.enums.TournamentFormat;

class TeamGameMatchRuleTest {

    private TeamGameMatchRule rule;
    private Team team;
    private Tournament tournament;
    private Game teamGame;
    private Game tournamentGame;

    @BeforeEach
    void setUp() {
        rule = new TeamGameMatchRule();

        teamGame = new Game();
        teamGame.setId(1L);

        tournamentGame = new Game();
        tournamentGame.setId(1L);

        team = new Team();
        team.setId(10L);
        team.setGame(teamGame);

        tournament = new Tournament();
        tournament.setId(20L);
        tournament.setGame(tournamentGame);
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
    }

    @Test
    void acceptsWhenTeamGameMatchesTournamentGame() {
        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void rejectsWhenTeamGameDoesNotMatchTournamentGame() {
        tournamentGame.setId(2L);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }

    @Test
    void rejectsWhenTeamHasNoGame() {
        team.setGame(null);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }

    @Test
    void rejectsWhenTournamentHasNoGame() {
        tournament.setGame(null);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }
}