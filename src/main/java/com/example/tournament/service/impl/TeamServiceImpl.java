package com.example.tournament.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Player;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.request.TeamRequest;
import com.example.tournament.dto.response.TeamPlayerResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.service.TeamService;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teams;
    private final PlayerRepository players;
    private final TeamMapper mapper;

    public TeamServiceImpl(TeamRepository teams, PlayerRepository players, TeamMapper mapper) {
        this.teams = teams;
        this.players = players;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamResponse> list(String name, Pageable pageable) {
        Page<Team> result = name == null || name.isBlank()
                ? teams.findAll(pageable)
                : teams.findByNameContainingIgnoreCase(name.trim(), pageable);
        return result.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse get(Long id) {
        return mapper.toResponse(findTeam(id));
    }

    @Override
    public TeamResponse create(TeamRequest request) {
        String name = request.name().trim();
        if (teams.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Team name already exists");
        }
        Team team = new Team();
        team.setName(name);
        team.setDescription(request.description());
        team.setCreatedAt(LocalDateTime.now());
        return mapper.toResponse(teams.saveAndFlush(team));
    }

    @Override
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = findTeam(id);
        String name = request.name().trim();
        if (teams.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Team name already exists");
        }
        team.setName(name);
        team.setDescription(request.description());
        return mapper.toResponse(teams.saveAndFlush(team));
    }

    @Override
    public void delete(Long id) {
        Team team = findTeam(id);
        teams.delete(team);
        teams.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeamPlayerResponse> listPlayers(
            Long teamId,
            String name,
            String role,
            Pageable pageable) {

        // 1. ตรวจสอบก่อนว่า Team ที่ต้องการค้นหามีอยู่จริง
        // ถ้าไม่มี → ResourceNotFoundException
        findTeam(teamId);

        // 2. ตรวจสอบว่า name และ role ถูกส่งมาหรือไม่
        boolean hasName = name != null && !name.isBlank();
        boolean hasRole = role != null && !role.isBlank();

        Page<Player> result;

        // 3. ถ้ามีทั้ง name และ role
        // ค้นหา Player ที่ตรงทั้งชื่อและ Role
        if (hasName && hasRole) {

            result = players.findByTeamIdAndNameContainingIgnoreCaseAndRoleIgnoreCase(
                    teamId,
                    name.trim(),
                    role.trim(),
                    pageable);

            // 4. ถ้ามีเฉพาะ name
        } else if (hasName) {

            result = players.findByTeamIdAndNameContainingIgnoreCase(
                    teamId,
                    name.trim(),
                    pageable);

            // 5. ถ้ามีเฉพาะ role
        } else if (hasRole) {

            result = players.findByTeamIdAndRoleIgnoreCase(
                    teamId,
                    role.trim(),
                    pageable);

            // 6. ถ้าไม่ได้ส่งทั้ง name และ role
            // แสดง Player ทุกคนใน Team
        } else {

            result = players.findByTeamId(
                    teamId,
                    pageable);
        }

        // 7. แปลง Player Entity เป็น TeamPlayerResponse
        // เพื่อไม่ส่ง Entity ออกไปตรง ๆ จาก API
        return result.map(mapper::toPlayerResponse);
    }

    @Override
    public TeamPlayerResponse addPlayer(Long teamId, Long playerId) {
        Team team = findTeam(teamId);
        Player player = findPlayer(playerId);
        player.setTeam(team);
        return mapper.toPlayerResponse(players.save(player));
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
