package com.example.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.TournamentPlacementPoint;
import com.example.tournament.domain.entity.TournamentPlacementPointId;

public interface TournamentPlacementPointRepository
        extends JpaRepository<TournamentPlacementPoint, TournamentPlacementPointId> {

    List<TournamentPlacementPoint> findByTournamentId(Long tournamentId);
}