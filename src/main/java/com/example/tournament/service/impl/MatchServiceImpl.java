package com.example.tournament.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.service.MatchService;

@Service
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matches;
    private final MatchMapper mapper;

    public MatchServiceImpl(MatchRepository matches, MatchMapper mapper) {
        this.matches = matches;
        this.mapper = mapper;
    }

    @Override
    public Page<MatchResponse> list(Pageable pageable) {
        return matches.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    public MatchResponse get(Long id) {
        return matches.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }
}
