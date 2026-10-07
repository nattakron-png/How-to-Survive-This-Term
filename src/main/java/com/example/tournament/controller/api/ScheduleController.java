package com.example.tournament.controller.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.ScheduleResponse;
import com.example.tournament.service.ScheduleService;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @PostMapping("/schedule")
    public ResponseEntity<ScheduleResponse> create(@PathVariable Long tournamentId) {
        ScheduleResponse body = service.createSchedule(tournamentId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tournaments/{id}/matches").buildAndExpand(tournamentId).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/matches")
    public List<MatchResponse> matches(@PathVariable Long tournamentId) {
        return service.listMatches(tournamentId);
    }
}
