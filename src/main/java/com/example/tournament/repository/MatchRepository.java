package com.example.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.tournament.domain.entity.Match;


public interface MatchRepository extends JpaRepository<Match, Long> {

    boolean existsByTournamentId(Long tournamentId);

    List<Match> findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(Long tournamentId);
}
