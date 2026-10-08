package com.example.tournament.mapper;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Player;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.response.TeamPlayerResponse;
import com.example.tournament.dto.response.TeamResponse;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getDescription(), team.getCreatedAt());
    }

    public TeamPlayerResponse toPlayerResponse(Player player) {
        return new TeamPlayerResponse(player.getId(), player.getName(), player.getRole(),
                player.getDescription(), player.getTeam() == null ? null : player.getTeam().getId());
    }
}
