package com.example.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.TournamentPlacementPoint;
import com.example.tournament.domain.entity.TournamentPlacementPointId;

public interface TournamentPlacementPointRepository
        extends JpaRepository<TournamentPlacementPoint, TournamentPlacementPointId> {

    // ใช้ใน FreeFireResultServiceImpl (คำนวณคะแนน)
    List<TournamentPlacementPoint> findByTournamentId(Long tournamentId);

    // ใช้ในงาน Tournament ของ Naruset
    List<TournamentPlacementPoint> findByTournamentIdOrderByPlacementAsc(Long tournamentId);
}