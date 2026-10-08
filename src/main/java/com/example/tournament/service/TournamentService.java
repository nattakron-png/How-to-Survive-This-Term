package com.example.tournament.service;

import java.util.List;

import com.example.tournament.dto.request.TournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.dto.response.PlacementPointResponse;

public interface TournamentService {

    TournamentResponse create(TournamentRequest request);

    List<TournamentResponse> getAll();

    TournamentResponse getById(Long id);

    TournamentResponse update(Long id, TournamentRequest request);

    void delete(Long id);

    List<PlacementPointResponse> getPlacementPoints(Long tournamentId);
}
