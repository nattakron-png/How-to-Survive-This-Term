package com.example.tournament.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Player;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.response.TeamPlayerResponse;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TeamMembershipMapper;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.service.TeamMembershipService;

@Service
@Transactional
public class TeamMembershipServiceImpl implements TeamMembershipService {

    private final TeamRepository teams;
    private final PlayerRepository players;
    private final TeamMembershipMapper mapper;

    public TeamMembershipServiceImpl(TeamRepository teams, PlayerRepository players,
            TeamMembershipMapper mapper) {
        this.teams = teams;
        this.players = players;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamPlayerResponse> listPlayers(Long teamId, Pageable pageable) {
        findTeam(teamId);
        return players.findByTeamId(teamId, pageable).map(mapper::toResponse);
    }

    @Override
    public TeamPlayerResponse addPlayer(Long teamId, Long playerId) {
        Team team = findTeam(teamId);
        Player player = findPlayer(playerId);
        player.setTeam(team);
        return mapper.toResponse(players.save(player));
    }

    @Override
    public void removePlayer(Long teamId, Long playerId) {
        findTeam(teamId);
        Player player = findPlayer(playerId);
        if (player.getTeam() == null || !teamId.equals(player.getTeam().getId())) {
            throw new ResourceNotFoundException("Player is not in this team: " + playerId);
        }
        player.setTeam(null);
        players.save(player);
    }

    private Team findTeam(Long id) {
        return teams.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private Player findPlayer(Long id) {
        return players.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }
}
