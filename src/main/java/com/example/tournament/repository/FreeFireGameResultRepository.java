package com.example.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.FreeFireGameResult;

public interface FreeFireGameResultRepository extends JpaRepository<FreeFireGameResult, Long> {

    boolean existsByGameId(Long gameId);

    List<FreeFireGameResult> findByGameIdOrderByPlacementAsc(Long gameId);

    List<FreeFireGameResult> findByTournamentId(Long tournamentId);
}