package com.example.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.repository.FreeFireGameRepository;
import com.example.tournament.repository.GameRepository;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.service.TournamentTeamService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleResultFlowTests {

    @Autowired private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();
    @Autowired private GameRepository games;
    @Autowired private MatchRepository matches;
    @Autowired private FreeFireGameRepository freeFireGames;
    @Autowired private TournamentTeamService registrations;

    @Test
    void eliminationFlowSchedulesAdvancesWinnersAndCompletesTournament() throws Exception {
        long gameId = gameId("ROV");
        List<Long> teamIds = new ArrayList<>();
        for (int team = 0; team < 4; team++) {
            long teamId = idFrom(postJson("/api/v1/teams",
                    "{\"name\":\"" + unique("rov-team") + "\",\"gameId\":" + gameId + "}"));
            teamIds.add(teamId);
            for (int player = 0; player < 5; player++) {
                postJson("/api/v1/players", "{\"name\":\"" + unique("player")
                        + "\",\"role\":\"Player\",\"teamId\":" + teamId + "}");
            }
        }
        long tournamentId = createTournament(gameId, "SINGLE_ELIMINATION", null);
        teamIds.forEach(teamId -> registrations.addTeam(tournamentId, teamId));

        mvc.perform(post("/api/v1/tournaments/{id}/schedule", tournamentId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(3));
        mvc.perform(get("/api/v1/tournaments/{id}/matches", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(post("/api/v1/tournaments/{id}/schedule", tournamentId))
                .andExpect(status().isConflict());

        List<Match> bracket = matches.findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(tournamentId);
        assertEquals(3, bracket.size());
        Match first = bracket.get(0);
        Match second = bracket.get(1);
        Match finalMatch = bracket.get(2);
        mvc.perform(get("/api/v1/matches/{id}", finalMatch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        recordWin(first, first.getTeamA().getId());
        mvc.perform(get("/api/v1/matches/{id}", finalMatch.getId()))
                .andExpect(jsonPath("$.teamAId").value(first.getTeamA().getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
        recordWin(second, second.getTeamA().getId());
        mvc.perform(get("/api/v1/matches/{id}", finalMatch.getId()))
                .andExpect(jsonPath("$.teamAId").value(first.getTeamA().getId()))
                .andExpect(jsonPath("$.teamBId").value(second.getTeamA().getId()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        recordWin(finalMatch.getId(), first.getTeamA().getId());
        mvc.perform(get("/api/v1/matches/{id}/result", finalMatch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerTeamId").value(first.getTeamA().getId()));
        mvc.perform(get("/api/v1/tournaments/{id}", tournamentId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/v1/matches/{id}/result", finalMatch.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(resultJson(first.getTeamA().getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void pointsFlowRecordsEveryTeamAndShowsFinalStandings() throws Exception {
        long gameId = gameId("FREE_FIRE");
        List<Long> teamIds = new ArrayList<>();
        for (int team = 0; team < 2; team++) {
            long teamId = idFrom(postJson("/api/v1/teams",
                    "{\"name\":\"" + unique("ff-team") + "\",\"gameId\":" + gameId + "}"));
            teamIds.add(teamId);
            for (int player = 0; player < 4; player++) {
                postJson("/api/v1/players", "{\"name\":\"" + unique("player")
                        + "\",\"role\":\"Player\",\"teamId\":" + teamId + "}");
            }
        }
        long tournamentId = createTournament(gameId, "POINTS", 1);
        teamIds.forEach(teamId -> registrations.addTeam(tournamentId, teamId));
        mvc.perform(post("/api/v1/tournaments/{id}/schedule", tournamentId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1));
        assertTrue(matches.findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(tournamentId).isEmpty());
        long roundId = freeFireGames.findByTournamentIdOrderByGameNumberAsc(tournamentId).getFirst().getId();
        String results = "{\"results\":[{\"teamId\":" + teamIds.get(0)
                + ",\"placement\":1,\"kills\":3},{\"teamId\":" + teamIds.get(1)
                + ",\"placement\":2,\"kills\":1}]}";

        mvc.perform(post("/api/v1/free-fire-games/{id}/results", roundId)
                .contentType(MediaType.APPLICATION_JSON).content(results))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.results[0].teamId").value(teamIds.get(0)));
        mvc.perform(get("/api/v1/free-fire-games/{id}/results", roundId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].kills").value(3));
        mvc.perform(get("/api/v1/tournaments/{id}/standings", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesCompleted").value(1))
                .andExpect(jsonPath("$.standings[0].teamId").value(teamIds.get(0)))
                .andExpect(jsonPath("$.standings[0].totalPoints").value(15));
        mvc.perform(get("/api/v1/tournaments/{id}", tournamentId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(post("/api/v1/free-fire-games/{id}/results", roundId)
                .contentType(MediaType.APPLICATION_JSON).content(results))
                .andExpect(status().isConflict());
    }

    private void recordWin(Match match, long winnerId) throws Exception {
        recordWin(match.getId(), winnerId);
    }

    private void recordWin(long matchId, long winnerId) throws Exception {
        mvc.perform(post("/api/v1/matches/{id}/result", matchId)
                .contentType(MediaType.APPLICATION_JSON).content(resultJson(winnerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.winnerTeamId").value(winnerId));
    }

    private String resultJson(long winnerId) {
        return "{\"teamAScore\":1,\"teamBScore\":0,\"winnerTeamId\":" + winnerId + "}";
    }

    private long createTournament(long gameId, String format, Integer totalGames) throws Exception {
        String body = "{\"name\":\"" + unique("tournament") + "\",\"gameId\":" + gameId
                + ",\"format\":\"" + format + "\",\"pointsPerKill\":1,\"startDate\":\"2026-11-01\""
                + ",\"endDate\":\"2026-11-02\"" + (totalGames == null ? "" : ",\"totalGames\":" + totalGames) + "}";
        return idFrom(postJson("/api/v1/tournaments", body));
    }

    private JsonNode postJson(String path, String body) throws Exception {
        return json.readTree(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long idFrom(JsonNode response) {
        return response.path("id").asLong();
    }

    private long gameId(String code) {
        return games.findAll().stream().filter(game -> code.equals(game.getCode()))
                .findFirst().orElseThrow().getId();
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
