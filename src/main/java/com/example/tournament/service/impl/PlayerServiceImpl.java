package com.example.tournament.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Player;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.request.PlayerRequest;
import com.example.tournament.dto.response.PlayerResponse;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.PlayerMapper;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.service.PlayerService;

@Service
@Transactional
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository players;
    private final TeamRepository teams;
    private final PlayerMapper mapper;

    public PlayerServiceImpl(PlayerRepository players, TeamRepository teams, PlayerMapper mapper) {
        this.players = players;
        this.teams = teams;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlayerResponse> list(String name, Pageable pageable) {
        Page<Player> result = name == null || name.isBlank()
                ? players.findAll(pageable)
                : players.findByNameContainingIgnoreCase(name.trim(), pageable);
        return result.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse get(Long id) {
        return mapper.toResponse(findPlayer(id));
    }

    @Override
    public PlayerResponse create(PlayerRequest request) {
        Team team = findOptionalTeam(request.teamId());
        Player player = new Player();
        player.setName(request.name().trim());
        player.setRole(request.role().trim());
        player.setDescription(request.description());
        player.setTeam(team);
        player.setCreatedAt(LocalDateTime.now());
        return mapper.toResponse(players.saveAndFlush(player));
    }

    @Override
    public PlayerResponse update(Long id, PlayerRequest request) {
        Player player = findPlayer(id);
        Team team = findOptionalTeam(request.teamId());
        player.setName(request.name().trim());
        player.setRole(request.role().trim());
        player.setDescription(request.description());
        player.setTeam(team);
        return mapper.toResponse(players.saveAndFlush(player));
    }

    @Override
    public void delete(Long id) {
        players.delete(findPlayer(id));
        players.flush();
    }

    private Player findPlayer(Long id) {
        return players.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private Team findOptionalTeam(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return teams.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));
    }
}
