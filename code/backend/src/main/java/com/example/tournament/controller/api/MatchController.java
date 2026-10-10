package com.example.tournament.controller.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.PageResponse;
import com.example.tournament.service.MatchService;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matches;

    public MatchController(MatchService matches) {
        this.matches = matches;
    }

    @GetMapping
    public PageResponse<MatchResponse> list(
            @PageableDefault(size = 20, sort = {"roundNumber", "matchNumber"}) Pageable pageable) {
        return PageResponse.from(matches.list(pageable));
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable Long id) {
        return matches.get(id);
    }
}
