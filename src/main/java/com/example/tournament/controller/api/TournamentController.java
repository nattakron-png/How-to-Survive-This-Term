package com.example.tournament.controller.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.tournament.dto.request.TournamentRequest;
import com.example.tournament.dto.response.PlacementPointResponse;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.service.TournamentService;
import com.example.tournament.dto.response.PlacementPointResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournaments;

    public TournamentController(TournamentService tournaments) {
        this.tournaments = tournaments;
    }

    @GetMapping
    public List<TournamentResponse> list(
            @RequestParam(required = false) String name) {

        // Return all tournaments when no search keyword is provided.
        if (name == null || name.isBlank()) {
            return tournaments.getAll();
        }

        // Otherwise, return tournaments matching the keyword.
        return tournaments.searchByName(name);
    }

    @GetMapping("/{id}")
    public TournamentResponse get(@PathVariable Long id) {
        return tournaments.getById(id);
    }

    @GetMapping("/{id}/placement-points")
    public List<PlacementPointResponse> getPlacementPoints(
            @PathVariable Long id) {

        return tournaments.getPlacementPoints(id);
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> create(
            @Valid @RequestBody TournamentRequest request) {

        TournamentResponse tournament = tournaments.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(tournament.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(tournament);
    }

    @PutMapping("/{id}")
    public TournamentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TournamentRequest request) {

        return tournaments.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tournaments.delete(id);
        return ResponseEntity.noContent().build();
    }
}