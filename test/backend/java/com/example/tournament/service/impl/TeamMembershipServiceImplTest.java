package com.example.tournament.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import com.example.tournament.domain.entity.Player;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TeamMembershipMapper;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class TeamMembershipServiceImplTest {

    @Mock
    private TeamRepository teams;

    @Mock
    private PlayerRepository players;

    @Mock
    private TeamMembershipMapper mapper;

    @InjectMocks
    private TeamMembershipServiceImpl service;

    @Test
    void addPlayerMovesExistingPlayerToRequestedTeam() {
        Team oldTeam = team(1L);
        Team newTeam = team(2L);
        Player player = new Player();
        player.setId(10L);
        player.setTeam(oldTeam);
        when(teams.findById(2L)).thenReturn(Optional.of(newTeam));
        when(players.findById(10L)).thenReturn(Optional.of(player));
        when(players.save(player)).thenAnswer(invocation -> invocation.getArgument(0));

        service.addPlayer(2L, 10L);

        assertSame(newTeam, player.getTeam());
        verify(players).save(player);
        verify(mapper).toResponse(player);
    }

    @Test
    void removePlayerClearsTeamWhenPlayerBelongsToIt() {
        Team team = team(2L);
        Player player = new Player();
        player.setId(10L);
        player.setTeam(team);
        when(teams.findById(2L)).thenReturn(Optional.of(team));
        when(players.findById(10L)).thenReturn(Optional.of(player));

        service.removePlayer(2L, 10L);

        assertNull(player.getTeam());
        verify(players).save(player);
        verifyNoInteractions(mapper);
    }

    @Test
    void removePlayerRejectsDifferentTeamWithoutSaving() {
        Team requestedTeam = team(2L);
        Team actualTeam = team(3L);
        Player player = new Player();
        player.setId(10L);
        player.setTeam(actualTeam);
        when(teams.findById(2L)).thenReturn(Optional.of(requestedTeam));
        when(players.findById(10L)).thenReturn(Optional.of(player));

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> service.removePlayer(2L, 10L));

        assertEquals("Player is not in this team: 10", error.getMessage());
        assertSame(actualTeam, player.getTeam());
        verify(players, never()).save(any(Player.class));
        verifyNoInteractions(mapper);
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }
}
