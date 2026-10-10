package com.example.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.TournamentTeamId;
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.repository.GameRepository;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.TournamentTeamService;

@SpringBootTest
@AutoConfigureMockMvc
class TeamTournamentHistoryBehaviorTests {

    @Autowired private MockMvc mvc;
    @Autowired private GameRepository games;
    @Autowired private TeamRepository teams;
    @Autowired private PlayerRepository players;
    @Autowired private TournamentRepository tournaments;
    @Autowired private TournamentTeamRepository registrations;
    @Autowired private MatchRepository matches;
    @Autowired private TournamentTeamService tournamentTeams;

    @Test
    void upcomingTournamentAllowsRenamingTeamAndAddingEditingDeletingPlayers() throws Exception {
        assertTeamAndPlayerEditsAllowedAfterRegistration(false);
    }

    @Test
    void completedTournamentStillAllowsRenamingTeamAndAddingEditingDeletingPlayers() throws Exception {
        assertTeamAndPlayerEditsAllowedAfterRegistration(true);
    }

    @Test
    void sameTeamInTwoCompletedTournamentsStillUsesCurrentNameAndRoster() throws Exception {
        Long gameId = rovGameId();
        TeamFixture team = createPreparedTeam(gameId);
        Long firstTournamentId = createTournament(gameId);
        Long secondTournamentId = createTournament(gameId, 60);
        tournamentTeams.addTeam(firstTournamentId, team.id());
        tournamentTeams.addTeam(secondTournamentId, team.id());
        completeTournament(firstTournamentId);
        completeTournament(secondTournamentId);

        String changedName = uniqueName("renamed-ROV-team");
        mvc.perform(put("/api/v1/teams/{id}", team.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + changedName + "\",\"gameId\":" + gameId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(changedName));
        mvc.perform(delete("/api/v1/teams/{teamId}/players/{playerId}", team.id(), team.firstPlayerId()))
                .andExpect(status().isNoContent());

        for (Long tournamentId : new Long[] { firstTournamentId, secondTournamentId }) {
            assertTrue(registrations.existsById(new TournamentTeamId(tournamentId, team.id())));
            assertEquals(team.id(), registrations.findById(new TournamentTeamId(tournamentId, team.id()))
                    .orElseThrow().getTeam().getId());
            assertEquals(changedName, teams.findById(team.id()).orElseThrow().getName());
            assertEquals(4, players.countByTeamId(team.id()));
        }
        mvc.perform(get("/api/v1/players/{id}", team.firstPlayerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").isEmpty());
    }

    @Test
    void deletingRegisteredTeamBeforeTournamentStartsIsRejected() throws Exception {
        Long gameId = rovGameId();
        TeamFixture team = createPreparedTeam(gameId);
        Long tournamentId = createTournament(gameId);
        tournamentTeams.addTeam(tournamentId, team.id());

        mvc.perform(delete("/api/v1/teams/{id}", team.id()))
                .andExpect(status().isConflict());

        assertTrue(teams.existsById(team.id()));
        assertTrue(registrations.existsById(new TournamentTeamId(tournamentId, team.id())));
        assertTrue(tournaments.existsById(tournamentId));
    }

    @Test
    void deletingTeamWithTwoCompletedRegistrationsIsRejected() throws Exception {
        Long gameId = rovGameId();
        TeamFixture team = createPreparedTeam(gameId);
        Long firstTournamentId = createTournament(gameId);
        Long secondTournamentId = createTournament(gameId, 60);
        tournamentTeams.addTeam(firstTournamentId, team.id());
        tournamentTeams.addTeam(secondTournamentId, team.id());
        completeTournament(firstTournamentId);
        completeTournament(secondTournamentId);

        mvc.perform(delete("/api/v1/teams/{id}", team.id()))
                .andExpect(status().isConflict());

        assertTrue(teams.existsById(team.id()));
        assertTrue(registrations.existsById(new TournamentTeamId(firstTournamentId, team.id())));
        assertTrue(registrations.existsById(new TournamentTeamId(secondTournamentId, team.id())));
        assertTrue(tournaments.existsById(firstTournamentId));
        assertTrue(tournaments.existsById(secondTournamentId));
        assertEquals(5, players.countByTeamId(team.id()));
        assertTrue(players.existsById(team.firstPlayerId()));
    }

    @Test
    void deletingTeamReferencedByMatchIsRejectedAndRegistrationRemains() throws Exception {
        Long gameId = rovGameId();
        TeamFixture team = createPreparedTeam(gameId);
        Long tournamentId = createTournament(gameId);
        tournamentTeams.addTeam(tournamentId, team.id());

        Match match = new Match();
        match.setTournament(tournaments.findById(tournamentId).orElseThrow());
        match.setTeamA(teams.findById(team.id()).orElseThrow());
        match.setRoundNumber(1);
        match.setMatchNumber(1);
        match.setStatus("PENDING");
        match.setCreatedAt(LocalDateTime.now());
        matches.saveAndFlush(match);
        completeTournament(tournamentId);

        mvc.perform(delete("/api/v1/teams/{id}", team.id()))
                .andExpect(status().isConflict());

        assertTrue(teams.existsById(team.id()));
        assertTrue(registrations.existsById(new TournamentTeamId(tournamentId, team.id())));
        assertTrue(matches.existsById(match.getId()));
    }

    private void assertTeamAndPlayerEditsAllowedAfterRegistration(boolean completed) throws Exception {
        Long gameId = rovGameId();
        TeamFixture team = createPreparedTeam(gameId);
        Long tournamentId = createTournament(gameId);
        tournamentTeams.addTeam(tournamentId, team.id());
        if (completed) {
            completeTournament(tournamentId);
        }

        String changedTeamName = uniqueName("renamed-team");
        mvc.perform(put("/api/v1/teams/{id}", team.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + changedTeamName + "\",\"gameId\":" + gameId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(changedTeamName));

        String changedPlayerName = uniqueName("renamed-player");
        mvc.perform(put("/api/v1/players/{id}", team.firstPlayerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + changedPlayerName
                        + "\",\"role\":\"Player\",\"teamId\":" + team.id() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(changedPlayerName));

        String newPlayerName = uniqueName("new-player");
        mvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + newPlayerName
                        + "\",\"role\":\"Player\",\"teamId\":" + team.id() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teamId").value(team.id()));
        Long newPlayerId = players.findByNameContainingIgnoreCase(newPlayerName, Pageable.unpaged())
                .getContent().getFirst().getId();
        assertEquals(6, players.countByTeamId(team.id()));

        mvc.perform(delete("/api/v1/players/{id}", newPlayerId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/players/{id}", newPlayerId))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/players/{id}", team.firstPlayerId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(changedPlayerName));

        assertEquals(5, players.countByTeamId(team.id()));
        assertEquals(changedTeamName, teams.findById(team.id()).orElseThrow().getName());
        assertTrue(registrations.existsById(new TournamentTeamId(tournamentId, team.id())));
    }

    private TeamFixture createPreparedTeam(Long gameId) throws Exception {
        String teamName = uniqueName("ROV-team");
        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + teamName + "\",\"gameId\":" + gameId + "}"))
                .andExpect(status().isCreated());
        Long teamId = teams.findByNameContainingIgnoreCase(teamName, Pageable.unpaged())
                .getContent().getFirst().getId();

        Long firstPlayerId = null;
        for (int index = 0; index < 5; index++) {
            String playerName = uniqueName("ROV-player-" + index);
            mvc.perform(post("/api/v1/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + playerName
                            + "\",\"role\":\"Player\",\"teamId\":" + teamId + "}"))
                    .andExpect(status().isCreated());
            if (firstPlayerId == null) {
                firstPlayerId = players.findByNameContainingIgnoreCase(playerName, Pageable.unpaged())
                        .getContent().getFirst().getId();
            }
        }
        assertEquals(5, players.countByTeamId(teamId));
        return new TeamFixture(teamId, firstPlayerId);
    }

    private Long createTournament(Long gameId) throws Exception {
        return createTournament(gameId, 30);
    }

    private Long createTournament(Long gameId, int daysFromNow) throws Exception {
        String tournamentName = uniqueName("ROV-tournament");
        LocalDate start = LocalDate.now().plusDays(daysFromNow);
        mvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + tournamentName + "\",\"gameId\":" + gameId
                        + ",\"format\":\"SINGLE_ELIMINATION\",\"pointsPerKill\":1,\"startDate\":\""
                        + start + "\",\"endDate\":\"" + start.plusDays(1) + "\"}"))
                .andExpect(status().isCreated());
        return tournaments.findByName(tournamentName).orElseThrow().getId();
    }

    private void completeTournament(Long tournamentId) {
        var tournament = tournaments.findById(tournamentId).orElseThrow();
        tournament.setStatus(TournamentStatus.COMPLETED);
        tournaments.saveAndFlush(tournament);
    }

    private Long rovGameId() {
        return games.findAll().stream()
                .filter(game -> "ROV".equals(game.getCode()))
                .findFirst().orElseThrow().getId();
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private record TeamFixture(Long id, Long firstPlayerId) {
    }
}
