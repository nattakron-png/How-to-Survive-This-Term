package com.example.tournament.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.service.TeamLogoService;
import com.example.tournament.service.storage.FileStorageService;

@Service
public class TeamLogoServiceImpl implements TeamLogoService {

    private final TeamRepository teams;
    private final TeamMapper mapper;
    private final FileStorageService files;

    public TeamLogoServiceImpl(TeamRepository teams, TeamMapper mapper, FileStorageService files) {
        this.teams = teams;
        this.mapper = mapper;
        this.files = files;
    }

    @Override
    @Transactional
    public TeamResponse upload(Long teamId, MultipartFile file) {
        Team team = teams.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + teamId));
        String filename = files.storeLogo(file);
        team.setLogoUrl("/api/v1/logos/" + filename);
        try {
            return mapper.toResponse(teams.saveAndFlush(team));
        } catch (RuntimeException exception) {
            try {
                files.deleteLogo(filename);
            } catch (RuntimeException cleanupError) {
                exception.addSuppressed(cleanupError);
            }
            throw exception;
        }
    }
}
