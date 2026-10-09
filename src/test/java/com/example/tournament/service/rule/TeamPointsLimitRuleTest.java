package com.example.tournament.service.rule;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.TournamentTeamRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeamPointsLimitRuleTest {

    @Test
    void shouldPassWhenPointsTournamentHasFewerThanTwelveTeams() {
        TournamentTeamRepository repo = mock(TournamentTeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getId()).thenReturn(20L);
        when(tournament.getFormat()).thenReturn(TournamentFormat.POINTS);
        when(repo.countByTournamentId(20L)).thenReturn(11L);

        TeamPointsLimitRule rule = new TeamPointsLimitRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
    }

    @Test
    void shouldThrowWhenPointsTournamentAlreadyHasTwelveTeams() {
        TournamentTeamRepository repo = mock(TournamentTeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getId()).thenReturn(20L);
        when(tournament.getFormat()).thenReturn(TournamentFormat.POINTS);
        when(repo.countByTournamentId(20L)).thenReturn(12L);

        TeamPointsLimitRule rule = new TeamPointsLimitRule(repo);

        assertThrows(
            BusinessException.class,
            () -> rule.validate(team, tournament)
        );
    }

    @Test
    void shouldSkipTeamLimitForNonPointsTournament() {
        TournamentTeamRepository repo = mock(TournamentTeamRepository.class);
        Team team = mock(Team.class);
        Tournament tournament = mock(Tournament.class);

        when(tournament.getFormat()).thenReturn(
            TournamentFormat.SINGLE_ELIMINATION
        );

        TeamPointsLimitRule rule = new TeamPointsLimitRule(repo);

        assertDoesNotThrow(() -> rule.validate(team, tournament));
        verifyNoInteractions(repo);
    }
}
