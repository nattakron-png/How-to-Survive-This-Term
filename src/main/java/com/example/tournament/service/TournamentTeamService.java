package com.example.tournament.service;

public interface TournamentTeamService {


void addTeam(Long tournamentId, Long teamId);

void removeTeam(Long tournamentId, Long teamId);


}
