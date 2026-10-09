package com.example.tournament.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    // Search tournaments by partial name, ignoring letter case.
    List<Tournament> findByNameContainingIgnoreCase(String name);

    Optional<Tournament> findByName(String name);

    boolean existsByName(String name);
}