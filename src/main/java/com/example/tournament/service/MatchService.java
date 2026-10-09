package com.example.tournament.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.tournament.dto.response.MatchResponse;

public interface MatchService {

    Page<MatchResponse> list(Pageable pageable);

    MatchResponse get(Long id);
}
