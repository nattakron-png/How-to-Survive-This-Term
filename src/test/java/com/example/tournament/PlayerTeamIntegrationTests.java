package com.example.tournament;

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
class PlayerTeamIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private TeamRepository teams;

    @Test
    void teamApiChangesAreVisibleThroughPlayerApi() throws Exception {
        Long playerId = createPlayer();
        Long firstTeamId = createTeam();
        Long secondTeamId = createTeam();

        mvc.perform(put("/api/v1/teams/{id}/players/{playerId}", firstTeamId, playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(firstTeamId));
        mvc.perform(get("/api/v1/players/{id}", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(firstTeamId));

        mvc.perform(put("/api/v1/teams/{id}/players/{playerId}", secondTeamId, playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(secondTeamId));
        mvc.perform(get("/api/v1/teams/{id}/players", firstTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/teams/{id}/players", secondTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(playerId));
        mvc.perform(get("/api/v1/players/{id}", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(secondTeamId));

        mvc.perform(delete("/api/v1/teams/{id}/players/{playerId}", secondTeamId, playerId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/players/{id}", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").doesNotExist());
        mvc.perform(get("/api/v1/teams/{id}/players", secondTeamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private Long createPlayer() throws Exception {
        String name = uniqueName();
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"role\":\"Captain\"}"))
                .andExpect(status().isCreated());
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
        return "Shared-" + UUID.randomUUID();
    }
}
