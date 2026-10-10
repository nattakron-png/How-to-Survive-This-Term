package com.example.tournament.service;

import org.springframework.web.multipart.MultipartFile;

import com.example.tournament.dto.response.TeamResponse;

public interface TeamLogoService {

    TeamResponse upload(Long teamId, MultipartFile file);
}
