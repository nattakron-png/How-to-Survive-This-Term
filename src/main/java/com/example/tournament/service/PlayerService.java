package com.example.tournament.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.tournament.dto.request.PlayerRequest;
import com.example.tournament.dto.response.PlayerResponse;

public interface PlayerService {

    Page<PlayerResponse> list(String name, Pageable pageable);

    PlayerResponse get(Long id);

    PlayerResponse create(PlayerRequest request);

    PlayerResponse update(Long id, PlayerRequest request);

    void delete(Long id);
}
