package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Game;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamMinimumPlayersRuleTest {

    @Test
    void shouldPassWhenTeamHasEnoughPlayers() {
        PlayerRepository repo = mock(PlayerRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);
        Game game = mock(Game.class);

        when(team.getId()).thenReturn(10L);
        when(tournament.getGame()).thenReturn(game);
        when(game.getMinPlayers()).thenReturn(5);
        when(repo.countByTeamId(10L)).thenReturn(5L);

        TeamMinimumPlayersRule rule = new TeamMinimumPlayersRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void shouldThrowWhenTeamHasTooFewPlayers() {
        PlayerRepository repo = mock(PlayerRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);
        Game game = mock(Game.class);

        when(team.getId()).thenReturn(10L);
        when(tournament.getGame()).thenReturn(game);
        when(game.getMinPlayers()).thenReturn(5);
        when(repo.countByTeamId(10L)).thenReturn(4L);

        TeamMinimumPlayersRule rule = new TeamMinimumPlayersRule(repo);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }

    @Test
    void shouldPassWhenMinimumPlayersIsNotConfigured() {
        PlayerRepository repo = mock(PlayerRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);
        Game game = mock(Game.class);

        when(tournament.getGame()).thenReturn(game);
        when(game.getMinPlayers()).thenReturn(null);

        TeamMinimumPlayersRule rule = new TeamMinimumPlayersRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
        verifyNoInteractions(repo);
    }
}
