package com.example.tournament.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.entity.TournamentTeamId;
import com.example.tournament.dto.request.SeedAssignmentRequest;
import com.example.tournament.dto.response.TournamentTeamResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.TournamentTeamService;

@Service
@Transactional
public class TournamentTeamServiceImpl implements TournamentTeamService {

    private final TournamentRepository tournaments;
    private final TeamRepository teams;
    private final TournamentTeamRepository tournamentTeams;

    public TournamentTeamServiceImpl(
            TournamentRepository tournaments,
            TeamRepository teams,
            TournamentTeamRepository tournamentTeams) {
        this.tournaments = tournaments;
        this.teams = teams;
        this.tournamentTeams = tournamentTeams;
    }

    // เพิ่มทีมเข้าร่วมการแข่งขัน
    @Override
    public void addTeam(Long tournamentId, Long teamId) {
        Tournament tournament = findTournament(tournamentId);

        Team team = teams.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Team not found: " + teamId));

        // อนุญาตให้จัดการทีมได้เฉพาะทัวร์นาเมนต์ที่ยังไม่เริ่ม
        requireUpcoming(tournament);

        // ตรวจสอบว่าเกมของทีมตรงกับเกมของทัวร์นาเมนต์
        if (team.getGame() == null
                || tournament.getGame() == null
                || !team.getGame().getId().equals(tournament.getGame().getId())) {
            throw new ValidationException(
                    "Team game must match tournament game");
        }

        TournamentTeamId id = new TournamentTeamId(tournamentId, teamId);

        // ป้องกันการเพิ่มทีมเดิมซ้ำ
        if (tournamentTeams.existsById(id)) {
            throw new BusinessException(
                    "Team already joined this tournament");
        }

        TournamentTeam entry = new TournamentTeam();
        entry.setId(id);
        entry.setTournament(tournament);
        entry.setTeam(team);
        entry.setJoinedAt(LocalDateTime.now());

        // เก็บชื่อทีม ณ เวลาที่สมัครไว้เป็นข้อมูลสำรอง
        entry.setTeamNameSnapshot(team.getName());

        tournamentTeams.save(entry);
    }

    // นำทีมออกจากการแข่งขัน
    @Override
    public void removeTeam(Long tournamentId, Long teamId) {
        Tournament tournament = findTournament(tournamentId);

        requireUpcoming(tournament);

        TournamentTeamId id = new TournamentTeamId(tournamentId, teamId);

        if (!tournamentTeams.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Team is not registered in this tournament");
        }

        tournamentTeams.deleteById(id);
    }

    // แสดงรายชื่อทีมที่สมัคร โดยเรียงตามเวลาที่สมัคร
    @Override
    @Transactional(readOnly = true)
    public List<TournamentTeamResponse> listTeams(Long tournamentId) {
        if (!tournaments.existsById(tournamentId)) {
            throw new ResourceNotFoundException(
                    "Tournament not found: " + tournamentId);
        }

        return tournamentTeams
                .findByTournamentIdOrderByJoinedAtAsc(tournamentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // กำหนด Seed ให้ทีมที่สมัครเข้าร่วมแล้ว
    @Override
    public List<TournamentTeamResponse> setSeeds(
            Long tournamentId,
            SeedAssignmentRequest request) {

        Tournament tournament = findTournament(tournamentId);
        requireUpcoming(tournament);

        if (request == null
                || request.assignments() == null
                || request.assignments().isEmpty()) {
            throw new ValidationException(
                    "Seed assignments must not be empty");
        }

        List<TournamentTeam> entries =
                tournamentTeams.findByTournamentId(tournamentId);

        // สร้างแผนที่สำหรับค้นหาทีมจาก teamId
        Map<Long, TournamentTeam> entriesByTeamId = entries.stream()
                .collect(Collectors.toMap(
                        entry -> entry.getTeam().getId(),
                        Function.identity()));

        Set<Long> submittedTeamIds = new HashSet<>();
        Set<Integer> submittedSeeds = new HashSet<>();

        // ตรวจสอบข้อมูล Seed ก่อนบันทึก
        for (SeedAssignmentRequest.SeedItem item
                : request.assignments()) {

            if (item == null
                    || item.teamId() == null
                    || item.seed() == null
                    || item.teamId() <= 0
                    || item.seed() <= 0) {
                throw new ValidationException(
                        "Team ID and seed must be positive");
            }

            // ทีมหนึ่งทีมต้องไม่ถูกส่งมาซ้ำ
            if (!submittedTeamIds.add(item.teamId())) {
                throw new ValidationException(
                        "Duplicate team ID: " + item.teamId());
            }

            // Seed แต่ละหมายเลขต้องไม่ซ้ำกัน
            if (!submittedSeeds.add(item.seed())) {
                throw new ValidationException(
                        "Duplicate seed: " + item.seed());
            }

            // ต้องเป็นทีมที่สมัครทัวร์นาเมนต์นี้แล้ว
            if (!entriesByTeamId.containsKey(item.teamId())) {
                throw new ValidationException(
                        "Team " + item.teamId()
                                + " is not registered in this tournament");
            }
        }

        // บันทึก Seed หลังจากตรวจสอบข้อมูลทั้งหมดผ่านแล้ว
        for (SeedAssignmentRequest.SeedItem item
                : request.assignments()) {
            entriesByTeamId.get(item.teamId()).setSeed(item.seed());
        }

        tournamentTeams.saveAll(entries);

        return entries.stream()
                .map(this::toResponse)
                .toList();
    }

    // ค้นหาทัวร์นาเมนต์ หากไม่พบให้แจ้งข้อผิดพลาด
    private Tournament findTournament(Long tournamentId) {
        return tournaments.findById(tournamentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tournament not found: " + tournamentId));
    }

    // ตรวจสอบว่าทัวร์นาเมนต์ยังอยู่ในสถานะ UPCOMING
    private void requireUpcoming(Tournament tournament) {
        if (!"UPCOMING".equalsIgnoreCase(tournament.getStatus())) {
            throw new BusinessException(
                    "This operation is only allowed for upcoming tournaments");
        }
    }

    // แปลงข้อมูล Entity เป็น Response สำหรับส่งกลับ API
    private TournamentTeamResponse toResponse(TournamentTeam entry) {
        return new TournamentTeamResponse(
                entry.getTournament().getId(),
                entry.getTeam().getId(),
                entry.getTeamNameSnapshot(),
                entry.getJoinedAt(),
                entry.getSeed());
    }
}