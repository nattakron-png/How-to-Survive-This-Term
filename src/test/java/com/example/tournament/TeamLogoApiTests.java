package com.example.tournament;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.repository.TeamRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.storage.logo-dir=target/test-logos")
class TeamLogoApiTests {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l7sAAAAASUVORK5CYII=");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TeamRepository teams;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void uploadsLogoAndReturnsSavedUrlAndImage() throws Exception {
        Long teamId = createTeam();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG);

        byte[] response = mvc.perform(multipart("/api/v1/teams/{id}/logo", teamId)
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teamId))
                .andExpect(jsonPath("$.logoUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsByteArray();

        JsonNode body = json.readTree(response);
        String logoUrl = body.get("logoUrl").asText();
        assertTrue(logoUrl.startsWith("/api/v1/logos/"));
        assertEquals(logoUrl, teams.findById(teamId).orElseThrow().getLogoUrl());

        mvc.perform(get("/api/v1/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(logoUrl));

        String updatedName = "Updated-" + UUID.randomUUID();
        mvc.perform(put("/api/v1/teams/{id}", teamId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + updatedName + "\",\"description\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(logoUrl));

        byte[] image = mvc.perform(get(logoUrl))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("image/png", result.getResponse().getContentType()))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(PNG, image);
    }

    @Test
    void rejectsInvalidImageWithoutChangingLogo() throws Exception {
        Long teamId = createTeam();
        MockMultipartFile fake = new MockMultipartFile("file", "fake.png", "image/png",
                "not an image".getBytes());

        mvc.perform(multipart("/api/v1/teams/{id}/logo", teamId)
                .file(fake)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isBadRequest());

        assertEquals(null, teams.findById(teamId).orElseThrow().getLogoUrl());
    }

    @Test
    void missingTeamDoesNotAcceptLogo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG);

        mvc.perform(multipart("/api/v1/teams/{id}/logo", Long.MAX_VALUE)
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsImageWithTooManyPixelsBeforeSavingUrl() throws Exception {
        Long teamId = createTeam();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        MockMultipartFile file = new MockMultipartFile("file", "wide.png", "image/png", output.toByteArray());

        mvc.perform(multipart("/api/v1/teams/{id}/logo", teamId)
                .file(file)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isBadRequest());

        assertEquals(null, teams.findById(teamId).orElseThrow().getLogoUrl());
    }

    private Long createTeam() {
        Team team = new Team();
        team.setName("Logo-" + UUID.randomUUID());
        team.setCreatedAt(LocalDateTime.now());
        return teams.saveAndFlush(team).getId();
    }
}
