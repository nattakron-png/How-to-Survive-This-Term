package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.TournamentTeamRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeamTournamentDateRuleTest {

    @Test
    void shouldPassWhenThereAreNoOverlappingTournaments() {
        TournamentTeamRepository repo = mock(TournamentTeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 10);

        when(team.getId()).thenReturn(10L);
        when(tournament.getId()).thenReturn(20L);
        when(tournament.getStartDate()).thenReturn(start);
        when(tournament.getEndDate()).thenReturn(end);

        when(repo.countOverlappingTournaments(
            10L, 20L, start, end
        )).thenReturn(0L);

        TeamTournamentDateRule rule = new TeamTournamentDateRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void shouldThrowWhenTournamentDatesOverlap() {
        TournamentTeamRepository repo = mock(TournamentTeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 10);

        when(team.getId()).thenReturn(10L);
        when(tournament.getId()).thenReturn(20L);
        when(tournament.getStartDate()).thenReturn(start);
        when(tournament.getEndDate()).thenReturn(end);

        when(repo.countOverlappingTournaments(
            10L, 20L, start, end
        )).thenReturn(1L);

        TeamTournamentDateRule rule = new TeamTournamentDateRule(repo);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }
}
