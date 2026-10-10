package com.example.tournament.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.MatchResult;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    boolean existsByMatchId(Long matchId);
    Optional<MatchResult> findByMatchId(Long matchId);
}
