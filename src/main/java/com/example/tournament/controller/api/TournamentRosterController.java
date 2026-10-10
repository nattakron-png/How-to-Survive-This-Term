package com.example.tournament.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournament.dto.response.TournamentRosterResponse;
import com.example.tournament.service.TournamentRosterSnapshotService;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/teams")
public class TournamentRosterController {

    private final TournamentRosterSnapshotService rosters;

    public TournamentRosterController(TournamentRosterSnapshotService rosters) {
        this.rosters = rosters;
    }

    @GetMapping
    public List<TournamentRosterResponse> list(@PathVariable Long tournamentId) {
        return rosters.list(tournamentId);
    }

    @GetMapping("/{teamId}/roster")
    public TournamentRosterResponse get(@PathVariable Long tournamentId, @PathVariable Long teamId) {
        return rosters.get(tournamentId, teamId);
    }
}
