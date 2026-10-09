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
import com.example.tournament.dto.request.TeamRequest;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository teams;

    @Mock
    private TeamMapper mapper;

    @InjectMocks
    private TeamServiceImpl service;

    @Test
    void createRejectsDuplicateNameBeforeSaving() {
        when(teams.existsByNameIgnoreCase("Phoenix")).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(new TeamRequest("  Phoenix  ", "Description")));

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
                () -> service.update(1L, new TeamRequest(" Phoenix ", "Changed")));

        assertEquals("Team name already exists", error.getMessage());
        assertEquals("Original", team.getName());
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
