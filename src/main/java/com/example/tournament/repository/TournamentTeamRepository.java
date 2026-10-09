package com.example.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.entity.TournamentTeamId;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, TournamentTeamId> {

    List<TournamentTeam> findByTournamentId(Long tournamentId);

    List<TournamentTeam> findByTournamentIdOrderByJoinedAtAsc(Long tournamentId);

    long countByTournamentId(Long tournamentId);

    @Query("""
            SELECT COUNT(tt)
            FROM TournamentTeam tt
            WHERE tt.team.id = :teamId
              AND tt.tournament.id <> :tournamentId
              AND tt.tournament.startDate <= :endDate
              AND tt.tournament.endDate >= :startDate
            """)
    long countOverlappingTournaments(
            @Param("teamId") Long teamId,
            @Param("tournamentId") Long tournamentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}