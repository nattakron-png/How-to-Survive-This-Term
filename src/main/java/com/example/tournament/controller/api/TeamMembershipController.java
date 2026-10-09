package com.example.tournament.controller.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournament.dto.response.PageResponse;
import com.example.tournament.dto.response.TeamPlayerResponse;
import com.example.tournament.service.TeamMembershipService;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/players")
public class TeamMembershipController {

    private final TeamMembershipService memberships;

    public TeamMembershipController(TeamMembershipService memberships) {
        this.memberships = memberships;
    }

    @GetMapping
    public PageResponse<TeamPlayerResponse> list(@PathVariable Long teamId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(memberships.listPlayers(teamId, pageable));
    }

    @PutMapping("/{playerId}")
    public TeamPlayerResponse add(@PathVariable Long teamId, @PathVariable Long playerId) {
        return memberships.addPlayer(teamId, playerId);
    }

    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> remove(@PathVariable Long teamId, @PathVariable Long playerId) {
        memberships.removePlayer(teamId, playerId);
        return ResponseEntity.noContent().build();
    }
}
