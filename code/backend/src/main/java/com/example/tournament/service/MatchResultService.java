package com.example.tournament.service;

import com.example.tournament.dto.request.CreateMatchResultRequest;
import com.example.tournament.dto.response.MatchResultResponse;

public interface MatchResultService {

    MatchResultResponse record(Long matchId, CreateMatchResultRequest request);

    MatchResultResponse get(Long matchId);
}
