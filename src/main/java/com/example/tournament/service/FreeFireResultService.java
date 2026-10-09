package com.example.tournament.service;

import com.example.tournament.dto.request.RecordFreeFireResultsRequest;
import com.example.tournament.dto.response.FreeFireGameResultsResponse;
import com.example.tournament.dto.response.FreeFireStandingsResponse;

public interface FreeFireResultService {

    FreeFireGameResultsResponse record(Long gameId, RecordFreeFireResultsRequest request);

    FreeFireGameResultsResponse getResults(Long gameId);

    FreeFireStandingsResponse standings(Long tournamentId);
}