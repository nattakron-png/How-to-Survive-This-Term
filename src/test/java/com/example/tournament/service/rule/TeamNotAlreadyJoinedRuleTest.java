package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentTeamId;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.TournamentTeamRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamNotAlreadyJoinedRuleTest {

    @Test
    void shouldPassWhenTeamHasNotJoined() {
        TournamentTeamRepository repo = mock(TournamentTeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(team.getId()).thenReturn(10L);
        when(tournament.getId()).thenReturn(20L);
        when(repo.existsById(new TournamentTeamId(20L, 10L))).thenReturn(false);

        TeamNotAlreadyJoinedRule rule = new TeamNotAlreadyJoinedRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void shouldThrowWhenTeamAlreadyJoined() {
        TournamentTeamRepository repo = mock(TournamentTeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(team.getId()).thenReturn(10L);
        when(tournament.getId()).thenReturn(20L);
        when(repo.existsById(new TournamentTeamId(20L, 10L))).thenReturn(true);

        TeamNotAlreadyJoinedRule rule = new TeamNotAlreadyJoinedRule(repo);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }
}
