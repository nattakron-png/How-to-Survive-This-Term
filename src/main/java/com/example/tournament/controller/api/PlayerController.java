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

import com.example.tournament.dto.request.PlayerRequest;
import com.example.tournament.dto.response.PageResponse;
import com.example.tournament.dto.response.PlayerResponse;
import com.example.tournament.service.PlayerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService players;

    public PlayerController(PlayerService players) {
        this.players = players;
    }

    @GetMapping
    public PageResponse<PlayerResponse> list(@RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(players.list(name, pageable));
    }

    @GetMapping("/{id}")
    public PlayerResponse get(@PathVariable Long id) {
        return players.get(id);
    }

    @PostMapping
    public ResponseEntity<PlayerResponse> create(@Valid @RequestBody PlayerRequest request) {
        PlayerResponse player = players.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(player.id()).toUri();
        return ResponseEntity.created(location).body(player);
    }

    @PutMapping("/{id}")
    public PlayerResponse update(@PathVariable Long id, @Valid @RequestBody PlayerRequest request) {
        return players.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        players.delete(id);
        return ResponseEntity.noContent().build();
    }
}
