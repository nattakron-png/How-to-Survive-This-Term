package com.example.tournament.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Game;
import com.example.tournament.dto.request.TeamRequest;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.GameRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentTeamRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository teams;

    @Mock
    private GameRepository games;

    @Mock
    private TeamMapper mapper;

    @Mock
    private TournamentTeamRepository registrations;

    @InjectMocks
    private TeamServiceImpl service;

    @Test
    void deleteRejectsTeamWithTournamentHistory() {
        Team team = team(1L);
        when(teams.findById(1L)).thenReturn(Optional.of(team));
        when(registrations.existsByTeam_Id(1L)).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(1L));

        assertEquals("Team has tournament history and cannot be deleted", error.getMessage());
        verify(teams, never()).delete(any(Team.class));
    }

    @Test
    void createRejectsDuplicateNameBeforeSaving() {
        when(teams.existsByNameIgnoreCase("Phoenix")).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(new TeamRequest("  Phoenix  ", "Description", 1L)));

        assertEquals("Team name already exists", error.getMessage());
        verify(teams).existsByNameIgnoreCase("Phoenix");
        verify(teams, never()).saveAndFlush(any(Team.class));
        verifyNoInteractions(mapper);
    }

    @Test
    void updateRejectsNameUsedByAnotherTeamWithoutSaving() {
        Team team = team(1L);
        when(teams.findById(1L)).thenReturn(Optional.of(team));
        when(teams.existsByNameIgnoreCaseAndIdNot("Phoenix", 1L)).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.update(1L, new TeamRequest(" Phoenix ", "Changed", null)));

        assertEquals("Team name already exists", error.getMessage());
        assertEquals("Original", team.getName());
        verify(teams, never()).saveAndFlush(any(Team.class));
        verifyNoInteractions(mapper);
    }

    @Test
    void createRejectsMissingGameBeforeSaving() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.create(new TeamRequest("Phoenix", "Description", null)));

        assertEquals("Game is required for a new team", error.getMessage());
        verifyNoInteractions(teams, games, mapper);
    }

    @Test
    void createRejectsUnknownGameBeforeSaving() {
        when(games.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> service.create(new TeamRequest("Phoenix", "Description", 99L)));

        assertEquals("Game not found: 99", error.getMessage());
        verify(teams, never()).saveAndFlush(any(Team.class));
        verifyNoInteractions(mapper);
    }

    @Test
    void updateRejectsChangingGameBeforeSaving() {
        Team team = team(1L);
        Game originalGame = new Game();
        originalGame.setId(1L);
        team.setGame(originalGame);
        Game otherGame = new Game();
        otherGame.setId(2L);
        when(teams.findById(1L)).thenReturn(Optional.of(team));
        when(games.findById(2L)).thenReturn(Optional.of(otherGame));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.update(1L, new TeamRequest("Changed", "Description", 2L)));

        assertEquals("Team game cannot be changed; create a new team for another game", error.getMessage());
        assertEquals("Original", team.getName());
        assertEquals(1L, team.getGame().getId());
        verify(teams, never()).saveAndFlush(any(Team.class));
        verifyNoInteractions(mapper);
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("Original");
        return team;
    }
}
