package com.example.tournament.controller.api;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.tournament.dto.request.TeamRequest;
import com.example.tournament.dto.response.PageResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.service.TeamService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teams;

    public TeamController(TeamService teams) {
        this.teams = teams;
    }

    @GetMapping
    public PageResponse<TeamResponse> list(@RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(teams.list(name, pageable));
    }

    @GetMapping("/{id}")
    public TeamResponse get(@PathVariable Long id) {
        return teams.get(id);
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        TeamResponse team = teams.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(team.id()).toUri();
        return ResponseEntity.created(location).body(team);
    }

    @PutMapping("/{id}")
    public TeamResponse update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return teams.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teams.delete(id);
        return ResponseEntity.noContent().build();
    }

}
