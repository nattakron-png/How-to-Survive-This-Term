package com.example.tournament.mapper;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Player;
import com.example.tournament.dto.response.TeamPlayerResponse;

@Component
public class TeamMembershipMapper {

    public TeamPlayerResponse toResponse(Player player) {
        return new TeamPlayerResponse(player.getId(), player.getName(), player.getRole(),
                player.getDescription(), player.getTeam() == null ? null : player.getTeam().getId());
    }
}
