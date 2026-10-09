package com.example.tournament.controller.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.service.TeamLogoService;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/logo")
public class TeamLogoController {

    private final TeamLogoService logos;

    public TeamLogoController(TeamLogoService logos) {
        this.logos = logos;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeamResponse upload(@PathVariable Long teamId, @RequestPart("file") MultipartFile file) {
        return logos.upload(teamId, file);
    }
}
