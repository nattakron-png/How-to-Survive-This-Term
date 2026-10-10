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
import com.example.tournament.dto.request.PlayerRequest;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.PlayerMapper;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class PlayerServiceImplTest {

    @Mock
    private PlayerRepository players;

    @Mock
    private TeamRepository teams;

    @Mock
    private PlayerMapper mapper;

    @InjectMocks
    private PlayerServiceImpl service;

    @Test
    void createRejectsUnknownTeamWithoutSavingPlayer() {
        when(teams.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> service.create(new PlayerRequest(" Player ", " Carry ", null, 5L)));

        assertEquals("Team not found: 5", error.getMessage());
        verify(players, never()).saveAndFlush(any(Player.class));
        verifyNoInteractions(mapper);
    }

    @Test
    void createWithoutTeamSavesIndependentPlayer() {
        when(players.saveAndFlush(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new PlayerRequest(" Player ", " Carry ", "Reserve", null));

        var saved = org.mockito.ArgumentCaptor.forClass(Player.class);
        verify(players).saveAndFlush(saved.capture());
        assertEquals("Player", saved.getValue().getName());
        assertEquals("Carry", saved.getValue().getRole());
        assertNull(saved.getValue().getTeam());
        verifyNoInteractions(teams);
    }

    @Test
    void updateMovesPlayerToAnotherTeam() {
        Team oldTeam = team(1L);
        Team newTeam = team(2L);
        Player player = player(10L, oldTeam);
        when(players.findById(10L)).thenReturn(Optional.of(player));
        when(teams.findById(2L)).thenReturn(Optional.of(newTeam));
        when(players.saveAndFlush(player)).thenReturn(player);

        service.update(10L, new PlayerRequest(" New Name ", " Support ", null, 2L));

        assertEquals("New Name", player.getName());
        assertEquals("Support", player.getRole());
        assertSame(newTeam, player.getTeam());
        verify(players).saveAndFlush(player);
        verify(mapper).toResponse(player);
    }

    @Test
    void updateRejectsUnknownTeamWithoutChangingExistingPlayer() {
        Team oldTeam = team(1L);
        Player player = player(10L, oldTeam);
        player.setName("Original");
        when(players.findById(10L)).thenReturn(Optional.of(player));
        when(teams.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> service.update(10L, new PlayerRequest("Changed", "Carry", null, 5L)));

        assertEquals("Team not found: 5", error.getMessage());
        assertEquals("Original", player.getName());
        assertSame(oldTeam, player.getTeam());
        verify(players, never()).saveAndFlush(any(Player.class));
        verifyNoInteractions(mapper);
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }

    private Player player(Long id, Team team) {
        Player player = new Player();
        player.setId(id);
        player.setTeam(team);
        return player;
    }
}
