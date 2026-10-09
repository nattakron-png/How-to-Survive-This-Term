
package com.example.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.tournament.domain.entity.Player;
import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TeamApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private PlayerRepository players;

    @Test
    void createsListsUpdatesAndDeletesTeam() throws Exception {
        String name = uniqueName();

        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson(name, "First description")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value("First description"))
                .andExpect(jsonPath("$.createdAt").exists());

        Long id = teams.findByNameContainingIgnoreCase(
                name, org.springframework.data.domain.Pageable.unpaged())
                .getContent().getFirst().getId();

        mvc.perform(get("/api/v1/teams")
                .param("name", name)
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));

        mvc.perform(put("/api/v1/teams/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson(name + " Updated", "Updated description")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + " Updated"));

        mvc.perform(get("/api/v1/teams/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));

        mvc.perform(delete("/api/v1/teams/{id}", id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/teams/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTeamReturnsSavedValues() throws Exception {
        String name = uniqueName();
        Long id = createTeam(name);

        mvc.perform(get("/api/v1/teams/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value(""));

        mvc.perform(get("/api/v1/teams")
                .param("name", name)
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].name").value(name));
    }

    @Test
    void postTeamPersistsSentValues() throws Exception {
        String name = uniqueName();

        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson(name, "First description")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value("First description"))
                .andExpect(jsonPath("$.createdAt").exists());

        Long id = teams.findByNameContainingIgnoreCase(
                name, org.springframework.data.domain.Pageable.unpaged())
                .getContent().getFirst().getId();

        mvc.perform(get("/api/v1/teams/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value("First description"));
    }

    @Test
    void putTeamPersistsUpdatedValues() throws Exception {
        Long id = createTeam(uniqueName());
        String updatedName = uniqueName();

        mvc.perform(put("/api/v1/teams/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson(updatedName, "Updated description")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.description").value("Updated description"));

        mvc.perform(get("/api/v1/teams/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void deleteTeamMakesItUnavailable() throws Exception {
        Long id = createTeam(uniqueName());

        mvc.perform(delete("/api/v1/teams/{id}", id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/teams/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankOrDuplicateName() throws Exception {
        String name = uniqueName();

        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson("  ", "")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson(name, "")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson("  " + name.toUpperCase() + "  ", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Team name already exists"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson("  ", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicateNameWhenCreatingTeam() throws Exception {
        String name = uniqueName();
        Long originalId = createTeam(name);

        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson("  " + name.toUpperCase() + "  ", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Team name already exists"));

        mvc.perform(get("/api/v1/teams")
                .param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(originalId))
                .andExpect(jsonPath("$.content[0].name").value(name));
    }

    @Test
    void rejectsDuplicateNameWhenUpdatingTeam() throws Exception {
        String firstName = uniqueName();
        Long firstId = createTeam(firstName);

        String secondName = uniqueName();
        Long secondId = createTeam(secondName);

        mvc.perform(put("/api/v1/teams/{id}", secondId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson("  " + firstName.toUpperCase() + "  ", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Team name already exists"));

        mvc.perform(get("/api/v1/teams/{id}", firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(firstName));

        mvc.perform(get("/api/v1/teams/{id}", secondId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(secondName));
    }

    @Test
    void movesAndRemovesPlayerFromTeam() throws Exception {
        Long firstTeamId = createTeam(uniqueName());
        Long secondTeamId = createTeam(uniqueName());

        Player player = new Player();
        player.setName(uniqueName());
        player.setRole("Captain");
        player.setCreatedAt(LocalDateTime.now());

        Long playerId = players.saveAndFlush(player).getId();

        mvc.perform(put("/api/v1/teams/{id}/players/{playerId}",
                firstTeamId, playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(firstTeamId));

        mvc.perform(get("/api/v1/teams/{id}/players", firstTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(playerId))
                .andExpect(jsonPath("$.content[0].teamId").value(firstTeamId));

        mvc.perform(put("/api/v1/teams/{id}/players/{playerId}",
                secondTeamId, playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(secondTeamId));

        mvc.perform(get("/api/v1/teams/{id}/players", firstTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/v1/teams/{id}/players", secondTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(playerId))
                .andExpect(jsonPath("$.content[0].teamId").value(secondTeamId));

        assertEquals(secondTeamId,
                players.findById(playerId).orElseThrow().getTeam().getId());

        mvc.perform(delete("/api/v1/teams/{id}/players/{playerId}",
                firstTeamId, playerId))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/teams/{id}/players/{playerId}",
                secondTeamId, playerId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/teams/{id}/players", secondTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        assertNull(players.findById(playerId).orElseThrow().getTeam());
    }

    @Test
    void searchesPlayersByRoleWithinTeam() throws Exception {
        Long teamId = createTeam(uniqueName());

        Player captain = new Player();
        captain.setName("Alpha Player");
        captain.setRole("Captain");
        captain.setCreatedAt(LocalDateTime.now());
        captain.setTeam(teams.findById(teamId).orElseThrow());
        players.saveAndFlush(captain);

        Player normalPlayer = new Player();
        normalPlayer.setName("Beta Player");
        normalPlayer.setRole("Player");
        normalPlayer.setCreatedAt(LocalDateTime.now());
        normalPlayer.setTeam(teams.findById(teamId).orElseThrow());
        players.saveAndFlush(normalPlayer);

        mvc.perform(get("/api/v1/teams/{id}/players", teamId)
                .param("role", "captain")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alpha Player"))
                .andExpect(jsonPath("$.content[0].role").value("Captain"));
    }

    @Test
    void searchesPlayersByNameAndRoleWithinTeam() throws Exception {
        Long teamId = createTeam(uniqueName());

        Player matchingPlayer = new Player();
        matchingPlayer.setName("Alpha Captain");
        matchingPlayer.setRole("Captain");
        matchingPlayer.setCreatedAt(LocalDateTime.now());
        matchingPlayer.setTeam(teams.findById(teamId).orElseThrow());
        players.saveAndFlush(matchingPlayer);

        Player wrongRole = new Player();
        wrongRole.setName("Alpha Player");
        wrongRole.setRole("Player");
        wrongRole.setCreatedAt(LocalDateTime.now());
        wrongRole.setTeam(teams.findById(teamId).orElseThrow());
        players.saveAndFlush(wrongRole);

        Player wrongName = new Player();
        wrongName.setName("Beta Captain");
        wrongName.setRole("Captain");
        wrongName.setCreatedAt(LocalDateTime.now());
        wrongName.setTeam(teams.findById(teamId).orElseThrow());
        players.saveAndFlush(wrongName);

        mvc.perform(get("/api/v1/teams/{id}/players", teamId)
                .param("name", "alpha")
                .param("role", "captain")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alpha Captain"))
                .andExpect(jsonPath("$.content[0].role").value("Captain"));
    }

    @Test
    void deletingTeamKeepsPlayerWithoutTeam() throws Exception {
        Long teamId = createTeam(uniqueName());

        Player player = new Player();
        player.setName(uniqueName());
        player.setRole("Player");
        player.setCreatedAt(LocalDateTime.now());
        player.setTeam(teams.findById(teamId).orElseThrow());

        Long playerId = players.saveAndFlush(player).getId();

        mvc.perform(delete("/api/v1/teams/{id}", teamId))
                .andExpect(status().isNoContent());

        assertNull(players.findById(playerId).orElseThrow().getTeam());
    }

    private Long createTeam(String name) throws Exception {
        mvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(teamJson(name, "")))
                .andExpect(status().isCreated());

        return teams.findByNameContainingIgnoreCase(
                name, org.springframework.data.domain.Pageable.unpaged())
                .getContent().getFirst().getId();
    }

    private String uniqueName() {
        return "Team-" + UUID.randomUUID();
    }

    private String teamJson(String name, String description) {
        return "{\"name\":\"" + name + "\",\"description\":\"" + description + "\"}";
    }
}