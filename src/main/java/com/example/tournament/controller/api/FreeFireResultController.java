package com.example.tournament.controller.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.tournament.dto.request.RecordFreeFireResultsRequest;
import com.example.tournament.dto.response.FreeFireGameResultsResponse;
import com.example.tournament.dto.response.FreeFireStandingsResponse;
import com.example.tournament.service.FreeFireResultService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class FreeFireResultController {

    private final FreeFireResultService service;

    public FreeFireResultController(FreeFireResultService service) {
        this.service = service;
    }

    @PostMapping("/free-fire-games/{gameId}/results")
    public ResponseEntity<FreeFireGameResultsResponse> record(@PathVariable Long gameId,
            @Valid @RequestBody RecordFreeFireResultsRequest request) {
        FreeFireGameResultsResponse body = service.record(gameId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/free-fire-games/{gameId}/results")
    public FreeFireGameResultsResponse results(@PathVariable Long gameId) {
        return service.getResults(gameId);
    }

    @GetMapping("/tournaments/{tournamentId}/standings")
    public FreeFireStandingsResponse standings(@PathVariable Long tournamentId) {
        return service.standings(tournamentId);
    }
}