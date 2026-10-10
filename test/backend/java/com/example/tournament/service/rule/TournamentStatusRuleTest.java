package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.MatchRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TournamentStatusRuleTest {

    @Test
    void shouldPassWhenUpcomingAndNoMatchesExist() {
        MatchRepository repo = mock(MatchRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getId()).thenReturn(20L);
        when(tournament.getStatus()).thenReturn(TournamentStatus.UPCOMING);
        when(repo.existsByTournamentId(20L)).thenReturn(false);

        TournamentStatusRule rule = new TournamentStatusRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void shouldThrowWhenTournamentIsNotUpcoming() {
        MatchRepository repo = mock(MatchRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getStatus()).thenReturn(null);

        TournamentStatusRule rule = new TournamentStatusRule(repo);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );

        verifyNoInteractions(repo);
    }

    @Test
    void shouldThrowWhenMatchesAlreadyExist() {
        MatchRepository repo = mock(MatchRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getId()).thenReturn(20L);
        when(tournament.getStatus()).thenReturn(TournamentStatus.UPCOMING);
        when(repo.existsByTournamentId(20L)).thenReturn(true);

        TournamentStatusRule rule = new TournamentStatusRule(repo);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }
}
