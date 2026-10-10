package com.example.tournament.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.entity.TournamentTeamId;
import com.example.tournament.dto.response.TournamentRosterResponse;
import com.example.tournament.dto.response.TournamentRosterResponse.PlayerAtRegistration;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.TournamentTeamRepository;

@Service
public class TournamentRosterSnapshotService {

    private final JdbcTemplate jdbc;
    private final TournamentTeamRepository registrations;

    public TournamentRosterSnapshotService(JdbcTemplate jdbc, TournamentTeamRepository registrations) {
        this.jdbc = jdbc;
        this.registrations = registrations;
    }

    // Called in the same transaction immediately after a new registration is flushed.
    public void capturePlayers(Long tournamentId, Long teamId) {
        jdbc.update("""
                INSERT INTO tournament_team_rosters
                    (tournament_id, team_id, player_id, player_name, player_role)
                SELECT ?, ?, id, name, role
                FROM players
                WHERE team_id = ?
                """, tournamentId, teamId, teamId);
    }

    @Transactional(readOnly = true)
    public List<TournamentRosterResponse> list(Long tournamentId) {
        return registrations.findByTournamentIdOrderByJoinedAtAsc(tournamentId).stream()
                .map(registration -> get(tournamentId, registration.getTeam().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentRosterResponse get(Long tournamentId, Long teamId) {
        TournamentTeam registration = registrations.findById(new TournamentTeamId(tournamentId, teamId))
                .orElseThrow(() -> new ResourceNotFoundException("Team is not registered in this tournament"));
        List<PlayerAtRegistration> players = jdbc.query("""
                SELECT player_id, player_name, player_role
                FROM tournament_team_rosters
                WHERE tournament_id = ? AND team_id = ?
                ORDER BY player_id
                """, (rs, rowNum) -> new PlayerAtRegistration(
                rs.getLong("player_id"), rs.getString("player_name"), rs.getString("player_role")),
                tournamentId, teamId);
        return new TournamentRosterResponse(tournamentId, teamId, registration.getTeamName(),
                registration.getTeamDescription(), registration.getTeamLogoUrl(), players);
    }
}
