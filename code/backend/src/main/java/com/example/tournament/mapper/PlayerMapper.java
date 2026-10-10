package com.example.tournament.mapper;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Player;
import com.example.tournament.dto.response.PlayerResponse;

@Component
public class PlayerMapper {

    public PlayerResponse toResponse(Player player) {
        return new PlayerResponse(player.getId(), player.getName(), player.getRole(),
                player.getDescription(), player.getTeam() == null ? null : player.getTeam().getId(),
                player.getCreatedAt());
    }
}
