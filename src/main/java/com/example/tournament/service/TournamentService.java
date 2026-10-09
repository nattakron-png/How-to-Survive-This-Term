package com.example.tournament.service;

import java.util.List;

import com.example.tournament.dto.request.TournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.dto.response.PlacementPointResponse;
import com.example.tournament.domain.enums.TournamentStatus;

public interface TournamentService {

    TournamentResponse create(TournamentRequest request);

    List<TournamentResponse> getAll();
    List<TournamentResponse> searchByName(String name);

    TournamentResponse getById(Long id);

    TournamentResponse update(Long id, TournamentRequest request);

    void delete(Long id);

    List<PlacementPointResponse> getPlacementPoints(Long tournamentId);

    List<TournamentResponse> getByStatus(TournamentStatus status);

    List<TournamentResponse> searchByNameAndStatus(
            String name,
            TournamentStatus status);
}
