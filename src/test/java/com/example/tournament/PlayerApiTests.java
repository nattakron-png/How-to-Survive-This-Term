package com.example.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.example.tournament.repository.PlayerRepository;
import com.example.tournament.repository.TeamRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PlayerApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private TeamRepository teams;

    @Test
    void getPlayerReturnsSavedValuesAndSearchFindsNameIgnoringCase() throws Exception {
        String name = uniqueName();
        Long id = createPlayer(name, null);

        mvc.perform(get("/api/v1/players/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.role").value("Captain"))
                .andExpect(jsonPath("$.teamId").doesNotExist());

        mvc.perform(get("/api/v1/players").param("name", name.toUpperCase()).param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test
    void postPlayerPersistsSentValuesWithoutTeam() throws Exception {
        String name = uniqueName();
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                .content(playerJson("  " + name + "  ", "  Captain  ", "First description", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.role").value("Captain"))
                .andExpect(jsonPath("$.description").value("First description"))
                .andExpect(jsonPath("$.teamId").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists());

        Long id = findPlayerId(name);
        mvc.perform(get("/api/v1/players/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.role").value("Captain"))
                .andExpect(jsonPath("$.description").value("First description"));
        assertNull(players.findById(id).orElseThrow().getTeam());
    }

    @Test
    void putPlayerPersistsUpdatedValuesAndChangesTeam() throws Exception {
        Long id = createPlayer(uniqueName(), null);
        Long teamId = createTeam();
        String updatedName = uniqueName();

        mvc.perform(put("/api/v1/players/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(updatedName, "Support", "Updated description", teamId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.role").value("Support"))
                .andExpect(jsonPath("$.teamId").value(teamId));

        mvc.perform(get("/api/v1/players/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedName))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.teamId").value(teamId));
        assertEquals(teamId, players.findById(id).orElseThrow().getTeam().getId());

        mvc.perform(put("/api/v1/players/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(updatedName, "Support", "Updated description", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").doesNotExist());
        assertNull(players.findById(id).orElseThrow().getTeam());
    }

    @Test
    void deletePlayerMakesItUnavailableAndRemovesItFromTeam() throws Exception {
        Long teamId = createTeam();
        Long id = createPlayer(uniqueName(), teamId);

        mvc.perform(delete("/api/v1/players/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/players/{id}", id)).andExpect(status().isNotFound());
        assertFalse(players.existsById(id));
    }

    @Test
    void invalidFieldsAndUnknownTeamAreRejectedWithoutSaving() throws Exception {
        String name = uniqueName();
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                .content(playerJson("  ", "Captain", "", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(name, "  ", "", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(name, "Captain", "", Long.MAX_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Team not found: " + Long.MAX_VALUE));
        mvc.perform(get("/api/v1/players").param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void unknownTeamOnUpdateLeavesExistingPlayerUnchanged() throws Exception {
        String name = uniqueName();
        Long id = createPlayer(name, null);

        mvc.perform(put("/api/v1/players/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(uniqueName(), "Support", "Changed", Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/players/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.role").value("Captain"));
    }

    @Test
    void sameNameCanBelongToTwoDifferentPlayers() throws Exception {
        String name = uniqueName();
        Long firstId = createPlayer(name, null);
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(name, "Support", "", null)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/players").param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        assertEquals("Captain", players.findById(firstId).orElseThrow().getRole());
    }

    private Long createPlayer(String name, Long teamId) throws Exception {
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(name, "Captain", "", teamId)))
                .andExpect(status().isCreated());
        return findPlayerId(name);
    }

    private Long findPlayerId(String name) {
        return players.findByNameContainingIgnoreCase(name, Pageable.unpaged())
                .getContent().getFirst().getId();
    }

    private Long createTeam() throws Exception {
        String name = uniqueName();
        mvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"description\":\"\"}"))
                .andExpect(status().isCreated());
        return teams.findByNameContainingIgnoreCase(name, Pageable.unpaged())
                .getContent().getFirst().getId();
    }

    private String uniqueName() {
        return "Player-" + UUID.randomUUID();
    }

    private String playerJson(String name, String role, String description, Long teamId) {
        return "{\"name\":\"" + name + "\",\"role\":\"" + role
                + "\",\"description\":\"" + description + "\",\"teamId\":" + teamId + "}";
    }
}
