package com.example.tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}