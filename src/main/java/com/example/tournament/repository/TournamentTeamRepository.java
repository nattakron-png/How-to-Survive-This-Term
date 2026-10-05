package com.example.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.entity.TournamentTeamId;

public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, TournamentTeamId> {

    List<TournamentTeam> findByTournamentId(Long tournamentId);

    List<TournamentTeam> findByTournamentIdOrderByJoinedAtAsc(Long tournamentId);
}