package com.example.tournament.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.entity.TournamentTeamId;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.TournamentTeamService;
import com.example.tournament.service.rule.TeamJoinRuleChain;

@Service
public class TournamentTeamServiceImpl implements TournamentTeamService {

    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamJoinRuleChain teamJoinRuleChain;

    public TournamentTeamServiceImpl(
            TeamRepository teamRepository,
            TournamentRepository tournamentRepository,
            TournamentTeamRepository tournamentTeamRepository,
            TeamJoinRuleChain teamJoinRuleChain) {
        this.teamRepository = teamRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.teamJoinRuleChain = teamJoinRuleChain;
    }

    @Override
    @Transactional
    public void addTeam(Long tournamentId, Long teamId) {
        // Check team first to preserve the required validation order.
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        // Check tournament second.
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));

        // Run the remaining business rules in the required order.
        teamJoinRuleChain.validate(team, tournament);

        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(new TournamentTeamId(tournamentId, teamId));
        tournamentTeam.setTournament(tournament);
        tournamentTeam.setTeam(team);
        tournamentTeam.setJoinedAt(LocalDateTime.now());

        tournamentTeamRepository.save(tournamentTeam);
    }

    @Override
    @Transactional
    public void removeTeam(Long tournamentId, Long teamId) {
        TournamentTeamId id = new TournamentTeamId(tournamentId, teamId);

        TournamentTeam tournamentTeam = tournamentTeamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Team is not registered in this tournament"));

        tournamentTeamRepository.delete(tournamentTeam);
    }

}
