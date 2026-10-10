package com.example.tournament.service;

import java.util.List;

import com.example.tournament.dto.request.SeedAssignmentRequest;
import com.example.tournament.dto.response.TournamentTeamResponse;

public interface TournamentTeamService {

    // เพิ่มทีมเข้าร่วมการแข่งขัน
    void addTeam(Long tournamentId, Long teamId);

    // นำทีมออกจากการแข่งขัน
    void removeTeam(Long tournamentId, Long teamId);

    // แสดงรายชื่อทีมในการแข่งขัน
    List<TournamentTeamResponse> listTeams(Long tournamentId);

    // กำหนด Seed ให้ทีม
    List<TournamentTeamResponse> setSeeds(
            Long tournamentId,
            SeedAssignmentRequest request);
}
