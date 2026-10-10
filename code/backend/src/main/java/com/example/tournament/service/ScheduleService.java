package com.example.tournament.service;

import java.util.List;

import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.ScheduleResponse;

public interface ScheduleService {

    ScheduleResponse createSchedule(Long tournamentId);

    List<MatchResponse> listMatches(Long tournamentId);
}
