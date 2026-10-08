package com.example.tournament;

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
                mvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON)
                                .content(teamJson(name, "First description")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value(name))
                                .andExpect(jsonPath("$.description").value("First description"))
                                .andExpect(jsonPath("$.createdAt").exists());

                Long id = teams.findByNameContainingIgnoreCase(name, org.springframework.data.domain.Pageable.unpaged())
                                .getContent().getFirst().getId();

                mvc.perform(get("/api/v1/teams").param("name", name).param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.content[0].id").value(id));

                mvc.perform(put("/api/v1/teams/{id}", id).contentType(MediaType.APPLICATION_JSON)
                                .content(teamJson(name + " Updated", "Updated description")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(name + " Updated"));

                mvc.perform(get("/api/v1/teams/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.description").value("Updated description"));

                mvc.perform(delete("/api/v1/teams/{id}", id)).andExpect(status().isNoContent());
                mvc.perform(get("/api/v1/teams/{id}", id)).andExpect(status().isNotFound());
        }

        @Test
        void rejectsBlankOrDuplicateName() throws Exception {
                String name = uniqueName();
                mvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON)
                                .content(teamJson("  ", "")))
                                .andExpect(status().isBadRequest());

                mvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON)
                                .content(teamJson(name, "")))
                                .andExpect(status().isCreated());

                mvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON)
                                .content(teamJson("  " + name.toUpperCase() + "  ", "")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Team name already exists"));
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

                mvc.perform(put("/api/v1/teams/{id}/players/{playerId}", firstTeamId, playerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.teamId").value(firstTeamId));

                mvc.perform(get("/api/v1/teams/{id}/players", firstTeamId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(1));

                mvc.perform(put("/api/v1/teams/{id}/players/{playerId}", secondTeamId, playerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.teamId").value(secondTeamId));

                mvc.perform(get("/api/v1/teams/{id}/players", firstTeamId))
                                .andExpect(jsonPath("$.totalElements").value(0));
                mvc.perform(delete("/api/v1/teams/{id}/players/{playerId}", firstTeamId, playerId))
                                .andExpect(status().isNotFound());
                mvc.perform(delete("/api/v1/teams/{id}/players/{playerId}", secondTeamId, playerId))
                                .andExpect(status().isNoContent());
                org.junit.jupiter.api.Assertions.assertNull(players.findById(playerId).orElseThrow().getTeam());
        }

        @Test
        void searchesPlayersByNameWithinTeam() throws Exception {

                // สร้าง Team สำหรับทดสอบ
                Long teamId = createTeam(uniqueName());

                // สร้าง Player คนที่ 1
                // ชื่อมีคำว่า "Alpha"
                Player firstPlayer = new Player();
                firstPlayer.setName("Alpha Player");
                firstPlayer.setRole("Player");
                firstPlayer.setCreatedAt(LocalDateTime.now());
                firstPlayer.setTeam(teams.findById(teamId).orElseThrow());
                players.saveAndFlush(firstPlayer);

                // สร้าง Player คนที่ 2
                // ชื่อไม่มีคำว่า "Alpha"
                Player secondPlayer = new Player();
                secondPlayer.setName("Beta Player");
                secondPlayer.setRole("Player");
                secondPlayer.setCreatedAt(LocalDateTime.now());
                secondPlayer.setTeam(teams.findById(teamId).orElseThrow());
                players.saveAndFlush(secondPlayer);

                // เรียก API โดยค้นหาคำว่า "alpha"
                // IgnoreCase ทำให้ "alpha" ค้นหา "Alpha" ได้
                mvc.perform(
                                get("/api/v1/teams/{id}/players", teamId)
                                                .param("name", "alpha")
                                                .param("size", "20"))
                                .andExpect(status().isOk())

                                // ต้องเจอเพียง 1 คน
                                .andExpect(jsonPath("$.totalElements").value(1))

                                // Player ที่เจอต้องเป็น Alpha Player
                                .andExpect(jsonPath("$.content[0].name")
                                                .value("Alpha Player"));
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

                mvc.perform(delete("/api/v1/teams/{id}", teamId)).andExpect(status().isNoContent());
                org.junit.jupiter.api.Assertions.assertNull(players.findById(playerId).orElseThrow().getTeam());
        }

        private Long createTeam(String name) throws Exception {
                mvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON)
                                .content(teamJson(name, "")))
                                .andExpect(status().isCreated());
                return teams.findByNameContainingIgnoreCase(name, org.springframework.data.domain.Pageable.unpaged())
                                .getContent().getFirst().getId();
        }

        private String uniqueName() {
                return "Team-" + UUID.randomUUID();
        }

        private String teamJson(String name, String description) {
                return "{\"name\":\"" + name + "\",\"description\":\"" + description + "\"}";
        }
}
