package com.example.tournament;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.example.tournament.service.TournamentTeamService;

@SpringBootTest
@AutoConfigureMockMvc
class TournamentRosterSnapshotTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private TournamentTeamService registrations;

    @Test
    void editingMovingAndDeletingLivePlayerDoesNotRewriteEitherTournamentRoster() throws Exception {
        String originalTeamName = "history-" + UUID.randomUUID();
        Long teamId = jdbc.queryForObject(
                "INSERT INTO teams (name, game_id) VALUES (?, 4) RETURNING id", Long.class, originalTeamName);
        Long oldPlayerId = jdbc.queryForObject(
                "INSERT INTO players (name, role, team_id) VALUES ('Original', 'Fighter', ?) RETURNING id",
                Long.class, teamId);
        Long firstId = tournament("first-", LocalDate.now().plusDays(30));
        Long secondId = tournament("second-", LocalDate.now().plusDays(60));

        registrations.addTeam(firstId, teamId);
        mvc.perform(get("/api/v1/tournaments/{id}/teams", firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamId").value(teamId))
                .andExpect(jsonPath("$[0].players[0].name").value("Original"));
        mvc.perform(get("/api/v1/tournaments/{id}/teams/{teamId}/roster", firstId, teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName").value(originalTeamName))
                .andExpect(jsonPath("$.players[0].name").value("Original"));

        mvc.perform(put("/api/v1/players/{id}", oldPlayerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\",\"role\":\"Substitute\",\"teamId\":null}"))
                .andExpect(status().isOk());
        Long newPlayerId = jdbc.queryForObject(
                "INSERT INTO players (name, role, team_id) VALUES ('New', 'Fighter', ?) RETURNING id",
                Long.class, teamId);
        registrations.addTeam(secondId, teamId);

        mvc.perform(delete("/api/v1/players/{id}", oldPlayerId))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/v1/teams/{id}", teamId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"changed-" + UUID.randomUUID() + "\",\"gameId\":4}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/tournaments/{id}/teams/{teamId}/roster", firstId, teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName").value(originalTeamName))
                .andExpect(jsonPath("$.players.length()").value(1))
                .andExpect(jsonPath("$.players[0].playerId").value(oldPlayerId))
                .andExpect(jsonPath("$.players[0].name").value("Original"))
                .andExpect(jsonPath("$.players[0].role").value("Fighter"));
        mvc.perform(get("/api/v1/tournaments/{id}/teams/{teamId}/roster", secondId, teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(1))
                .andExpect(jsonPath("$.players[0].playerId").value(newPlayerId))
                .andExpect(jsonPath("$.players[0].name").value("New"));
    }

    private Long tournament(String prefix, LocalDate start) {
        return jdbc.queryForObject("""
                INSERT INTO tournaments (name, start_date, end_date, status, game_id, format)
                VALUES (?, ?, ?, 'UPCOMING', 4, 'SINGLE_ELIMINATION')
                RETURNING id
                """, Long.class, prefix + UUID.randomUUID(), start, start.plusDays(2));
    }
}
