package com.example.tournament.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.tournament.dto.response.TeamPlayerResponse;

public interface TeamMembershipService {

    Page<TeamPlayerResponse> listPlayers(Long teamId, Pageable pageable);

    TeamPlayerResponse addPlayer(Long teamId, Long playerId);

    void removePlayer(Long teamId, Long playerId);
}
