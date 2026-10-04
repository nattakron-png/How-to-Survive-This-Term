package com.example.tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.tournament.domain.entity.Match;


public interface MatchRepository extends JpaRepository<Match, Long> {
}
