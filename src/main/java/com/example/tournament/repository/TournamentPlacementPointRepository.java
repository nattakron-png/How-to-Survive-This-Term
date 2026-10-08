package com.example.tournament.repository;

import com.example.tournament.domain.entity.TournamentPlacementPoint;
import com.example.tournament.domain.entity.TournamentPlacementPointId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentPlacementPointRepository
        extends JpaRepository<TournamentPlacementPoint, TournamentPlacementPointId> {

    List<TournamentPlacementPoint> findByTournamentIdOrderByPlacementAsc(Long tournamentId);
}