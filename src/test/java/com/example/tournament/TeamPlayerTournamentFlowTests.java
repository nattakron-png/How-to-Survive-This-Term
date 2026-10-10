package com.example.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.tournament.domain.entity.TournamentTeamId;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.GameRepository;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.TournamentTeamService;

@SpringBootTest
@AutoConfigureMockMvc
class TeamPlayerTournamentFlowTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GameRepository games;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private TournamentRepository tournaments;

    @Autowired
    private TournamentTeamRepository registrations;

    @Autowired
    private TournamentTeamService tournamentTeams;

    @Test
    void organizerRegistersPreparedTeamButCannotUseWrongGameOrChangeTeamGame() throws Exception {
        Long rovGameId = gameId("ROV");
        Long valorantGameId = gameId("VALORANT");
        String rovTeamName = uniqueName("ROV-team");
        Long rovTeamId = createTeam(rovTeamName, rovGameId);

        for (int index = 0; index < 5; index++) {
            String playerName = uniqueName("ROV-player-" + index);
            mvc.perform(post("/api/v1/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + playerName
                            + "\",\"role\":\"Player\",\"teamId\":" + rovTeamId + "}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.teamId").value(rovTeamId));
        }
        assertEquals(5, players.countByTeamId(rovTeamId));

        String tournamentName = uniqueName("ROV-tournament");
        mvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + tournamentName + "\",\"gameId\":" + rovGameId
                        + ",\"format\":\"SINGLE_ELIMINATION\",\"pointsPerKill\":1,\"startDate\":\"2026-11-01\""
                        + ",\"endDate\":\"2026-11-02\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(rovGameId))
                .andExpect(jsonPath("$.status").value("UPCOMING"));
        Long tournamentId = tournaments.findByName(tournamentName).orElseThrow().getId();

        tournamentTeams.addTeam(tournamentId, rovTeamId);
        assertTrue(registrations.existsById(new TournamentTeamId(tournamentId, rovTeamId)));

        Long valorantTeamId = createTeam(uniqueName("Valorant-team"), valorantGameId);
        BusinessException wrongGame = assertThrows(BusinessException.class,
                () -> tournamentTeams.addTeam(tournamentId, valorantTeamId));
        assertEquals("Team's game must match the tournament's game", wrongGame.getMessage());
        assertFalse(registrations.existsById(new TournamentTeamId(tournamentId, valorantTeamId)));

        mvc.perform(put("/api/v1/teams/{id}", rovTeamId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + rovTeamName + "\",\"gameId\":" + valorantGameId + "}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/teams/{id}", rovTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(rovGameId));
        assertTrue(registrations.existsById(new TournamentTeamId(tournamentId, rovTeamId)));
    }

    private Long createTeam(String name, Long gameId) throws Exception {
        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"gameId\":" + gameId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(gameId));
        return teams.findByNameContainingIgnoreCase(name, Pageable.unpaged())
                .getContent().getFirst().getId();
    }

    private Long gameId(String code) {
        return games.findAll().stream()
                .filter(game -> code.equals(game.getCode()))
                .findFirst().orElseThrow().getId();
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
