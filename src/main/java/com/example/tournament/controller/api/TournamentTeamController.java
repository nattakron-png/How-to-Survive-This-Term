package com.example.tournament.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tournament.dto.request.AddTournamentTeamRequest;
import com.example.tournament.service.TournamentTeamService;

import jakarta.validation.Valid;

// เพิ่มหรือถอนทีมในรายการ ส่วนการดูทีมในรายการอยู่ที่ TournamentRosterController (path เดียวกัน)
@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/teams")
public class TournamentTeamController {

    private final TournamentTeamService tournamentTeams;

    public TournamentTeamController(TournamentTeamService tournamentTeams) {
        this.tournamentTeams = tournamentTeams;
    }

    // ผ่านกฎ 8 ข้อ (Chain of Responsibility) แล้วบันทึก → 204, ไม่ผ่าน → 400 / 404 / 409
    @PostMapping
    public ResponseEntity<Void> add(@PathVariable Long tournamentId,
            @Valid @RequestBody AddTournamentTeamRequest request) {
        tournamentTeams.addTeam(tournamentId, request.teamId());
        return ResponseEntity.noContent().build();
    }

    // ถอนได้เฉพาะรายการที่ยังไม่เริ่ม (UPCOMING) → 204, ไม่พบ → 404, เริ่มแล้ว → 409
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> remove(@PathVariable Long tournamentId, @PathVariable Long teamId) {
        tournamentTeams.removeTeam(tournamentId, teamId);
        return ResponseEntity.noContent().build();
    }
}
