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

import com.example.tournament.dto.request.CreateMatchResultRequest;
import com.example.tournament.dto.response.MatchResultResponse;
import com.example.tournament.service.MatchResultService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/result")
public class MatchResultController {

    private final MatchResultService service;

    public MatchResultController(MatchResultService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MatchResultResponse> record(@PathVariable Long matchId,
            @Valid @RequestBody CreateMatchResultRequest request) {
        MatchResultResponse body = service.record(matchId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    public MatchResultResponse get(@PathVariable Long matchId) {
        return service.get(matchId);
    }
}
