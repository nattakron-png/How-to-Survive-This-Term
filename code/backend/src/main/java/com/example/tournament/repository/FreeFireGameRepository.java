package com.example.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.FreeFireGame;

public interface FreeFireGameRepository extends JpaRepository<FreeFireGame, Long> {

    List<FreeFireGame> findByTournamentIdOrderByGameNumberAsc(Long tournamentId);

    long countByTournamentIdAndStatus(Long tournamentId, String status);
}