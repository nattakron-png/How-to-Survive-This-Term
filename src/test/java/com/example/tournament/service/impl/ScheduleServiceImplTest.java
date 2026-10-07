package com.example.tournament.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.dto.response.ScheduleResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.format.FormatStrategy;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock TournamentRepository tournaments;
    @Mock TournamentTeamRepository tournamentTeams;
    @Mock MatchRepository matches;
    @Mock FormatStrategy singleElimination;

    ScheduleServiceImpl service;
    Tournament tournament;

    @BeforeEach
    void setUp() {
        when(singleElimination.format()).thenReturn(TournamentFormat.SINGLE_ELIMINATION);
        service = new ScheduleServiceImpl(tournaments, tournamentTeams, matches, new MatchMapper(),
                List.of(singleElimination));
        tournament = new Tournament();
        tournament.setStatus("UPCOMING");
    }

    @Test
    void rejectsWhenTournamentNotFound() {
        when(tournaments.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createSchedule(1L));
    }

    @Test
    void rejectsWhenTournamentAlreadyStarted() {
        tournament.setStatus("ONGOING");
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));

        assertThrows(BusinessException.class, () -> service.createSchedule(1L));
    }

    @Test
    void rejectsWhenScheduleAlreadyExists() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(matches.existsByTournamentId(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.createSchedule(1L));
    }

    @Test
    void rejectsWhenFewerThanTwoTeams() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentTeams.findByTournamentIdOrderByJoinedAtAsc(1L)).thenReturn(List.of(joined(10L)));

        assertThrows(BusinessException.class, () -> service.createSchedule(1L));
    }

    @Test
    void rejectsWhenNoStrategySupportsTheFormat() {
        tournament.setFormat(TournamentFormat.POINTS);
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentTeams.findByTournamentIdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(joined(10L), joined(20L)));

        assertThrows(BusinessException.class, () -> service.createSchedule(1L));
    }

    @Test
    void usesStrategyAndStartsTournament() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentTeams.findByTournamentIdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(joined(10L), joined(20L)));
        when(singleElimination.createSchedule(eq(tournament), anyList())).thenReturn(1);

        ScheduleResponse response = service.createSchedule(1L);

        assertEquals(1, response.created());
        assertEquals("SINGLE_ELIMINATION", response.format());
        assertEquals("ONGOING", tournament.getStatus());
        verify(singleElimination).createSchedule(eq(tournament), anyList());
    }

    private static TournamentTeam joined(Long teamId) {
        Team team = new Team();
        team.setId(teamId);
        TournamentTeam tt = new TournamentTeam();
        tt.setTeam(team);
        return tt;
    }
}
