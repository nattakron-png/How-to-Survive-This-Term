package com.example.tournament.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.rule.TeamJoinRuleChain;
import com.example.tournament.domain.entity.TournamentTeamId;

@ExtendWith(MockitoExtension.class)
class TournamentTeamServiceImplTest {

        @Mock
        private TeamRepository teamRepository;

        @Mock
        private TournamentRepository tournamentRepository;

        @Mock
        private TournamentTeamRepository tournamentTeamRepository;

        @Mock
        private TeamJoinRuleChain teamJoinRuleChain;

        private TournamentTeamServiceImpl service;
        private Team team;
        private Tournament tournament;

        @BeforeEach
        void setUp() {
                service = new TournamentTeamServiceImpl(
                                teamRepository,
                                tournamentRepository,
                                tournamentTeamRepository,
                                teamJoinRuleChain);

                team = new Team();
                team.setId(10L);

                tournament = new Tournament();
                tournament.setId(20L);
                tournament.setStartDate(LocalDate.of(2026, 11, 1));
                tournament.setEndDate(LocalDate.of(2026, 11, 10));
                tournament.setStatus(TournamentStatus.UPCOMING);
        }

        @Test
        void addsTeamWhenAllRulesPass() {
                when(teamRepository.findById(10L))
                                .thenReturn(Optional.of(team));
                when(tournamentRepository.findById(20L))
                                .thenReturn(Optional.of(tournament));

                service.addTeam(20L, 10L);

                verify(teamJoinRuleChain).validate(team, tournament);
                verify(tournamentTeamRepository).save(any(TournamentTeam.class));
        }

        @Test
        void rejectsWhenTeamDoesNotExist() {
                when(teamRepository.findById(10L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> service.addTeam(20L, 10L));

                verify(tournamentRepository, never()).findById(20L);
                verify(teamJoinRuleChain, never()).validate(any(), any());
                verify(tournamentTeamRepository, never()).save(any());
        }

        @Test
        void rejectsWhenTournamentDoesNotExist() {
                when(teamRepository.findById(10L))
                                .thenReturn(Optional.of(team));
                when(tournamentRepository.findById(20L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> service.addTeam(20L, 10L));

                verify(teamJoinRuleChain, never()).validate(any(), any());
                verify(tournamentTeamRepository, never()).save(any());
        }

        @Test
        void removesExistingTeamFromTournament() {
                when(tournamentRepository.findById(20L))
                                .thenReturn(Optional.of(tournament));

                when(tournamentTeamRepository.findById(any(TournamentTeamId.class)))
                                .thenAnswer(invocation -> {
                                        TournamentTeamId id = invocation.getArgument(0);
                                        TournamentTeam relation = new TournamentTeam();
                                        relation.setId(id);
                                        return Optional.of(relation);
                                });

                service.removeTeam(20L, 10L);

                verify(tournamentRepository).findById(20L);
                verify(tournamentTeamRepository).delete(any(TournamentTeam.class));
        }

        @Test
        void rejectsRemovingTeamThatIsNotRegistered() {
                when(tournamentRepository.findById(20L))
                                .thenReturn(Optional.of(tournament));

                when(tournamentTeamRepository.findById(any(TournamentTeamId.class)))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> service.removeTeam(20L, 10L));

                verify(tournamentRepository).findById(20L);
                verify(tournamentTeamRepository, never())
                                .delete(any(TournamentTeam.class));
        }

}