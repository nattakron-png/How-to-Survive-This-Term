package com.example.tournament.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.request.TeamRequest;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.service.TeamService;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teams;
    private final TeamMapper mapper;

    public TeamServiceImpl(TeamRepository teams, TeamMapper mapper) {
        this.teams = teams;
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

    private Team findTeam(Long id) {
        return teams.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }
}
