package com.example.tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.Game;

public interface GameRepository extends JpaRepository<Game, Long> {
}