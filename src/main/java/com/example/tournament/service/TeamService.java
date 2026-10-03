package com.example.tournament.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.tournament.dto.request.TeamRequest;
import com.example.tournament.dto.response.TeamPlayerResponse;
import com.example.tournament.dto.response.TeamResponse;

public interface TeamService {

    Page<TeamResponse> list(String name, Pageable pageable);

    TeamResponse get(Long id);

    TeamResponse create(TeamRequest request);

    TeamResponse update(Long id, TeamRequest request);

    void delete(Long id);

    Page<TeamPlayerResponse> listPlayers(Long teamId, Pageable pageable);

    TeamPlayerResponse addPlayer(Long teamId, Long playerId);

    void removePlayer(Long teamId, Long playerId);
}
