
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
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.dto.request.SeedAssignmentRequest;
import com.example.tournament.dto.response.TournamentTeamResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.TournamentRosterSnapshotService;
import com.example.tournament.service.TournamentTeamService;
import com.example.tournament.service.rule.TeamJoinRuleChain;

@Service
@Transactional
public class TournamentTeamServiceImpl implements TournamentTeamService {

    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamJoinRuleChain teamJoinRuleChain;
    private final TournamentRosterSnapshotService rosters;

    public TournamentTeamServiceImpl(
            TeamRepository teamRepository,
            TournamentRepository tournamentRepository,
            TournamentTeamRepository tournamentTeamRepository,
            TeamJoinRuleChain teamJoinRuleChain,
            TournamentRosterSnapshotService rosters) {
        this.teamRepository = teamRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.teamJoinRuleChain = teamJoinRuleChain;
        this.rosters = rosters;
    }

    @Override
    public void addTeam(Long tournamentId, Long teamId) {
        // ตรวจสอบทีมก่อน เพื่อรักษาลำดับการตรวจสอบของกฎเดิม
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Team not found"));

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Tournament not found"));

        // ตรวจสอบเงื่อนไขการสมัครทีมทั้งหมด
        teamJoinRuleChain.validate(team, tournament);

        // บันทึกข้อมูลทีม ณ วันที่สมัคร และสร้าง snapshot ของผู้เล่น
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(new TournamentTeamId(tournamentId, teamId));
        tournamentTeam.setTournament(tournament);
        tournamentTeam.setTeam(team);
        tournamentTeam.setJoinedAt(LocalDateTime.now());
        tournamentTeam.setTeamName(team.getName());
        tournamentTeam.setTeamDescription(team.getDescription());
        tournamentTeam.setTeamLogoUrl(team.getLogoUrl());

        tournamentTeamRepository.saveAndFlush(tournamentTeam);
        rosters.capturePlayers(tournamentId, teamId);
    }

    @Override
    public void removeTeam(Long tournamentId, Long teamId) {
        TournamentTeamId id = new TournamentTeamId(tournamentId, teamId);

        // ค้นหารายการสมัครทีมก่อน แล้วตรวจสอบสถานะจากรายการนั้น
        TournamentTeam tournamentTeam = tournamentTeamRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team is not registered in this tournament"));

        if (tournamentTeam.getTournament().getStatus()
                != TournamentStatus.UPCOMING) {
            throw new BusinessException(
                    "Cannot remove a team after the tournament has started");
        }

        tournamentTeamRepository.delete(tournamentTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TournamentTeamResponse> listTeams(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new ResourceNotFoundException(
                    "Tournament not found: " + tournamentId);
        }

        return tournamentTeamRepository
                .findByTournamentIdOrderByJoinedAtAsc(tournamentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TournamentTeamResponse> setSeeds(
            Long tournamentId,
            SeedAssignmentRequest request) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tournament not found: " + tournamentId));

        if (tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new BusinessException(
                    "Seeds can only be changed for upcoming tournaments");
        }

        if (request == null || request.assignments() == null
                || request.assignments().isEmpty()) {
            throw new ValidationException(
                    "Seed assignments must not be empty");
        }

        List<TournamentTeam> entries =
                tournamentTeamRepository.findByTournamentId(tournamentId);

        Map<Long, TournamentTeam> entriesByTeamId = entries.stream()
                .collect(Collectors.toMap(
                        entry -> entry.getTeam().getId(),
                        Function.identity()));

        Set<Long> submittedTeamIds = new HashSet<>();
        Set<Integer> submittedSeeds = new HashSet<>();

        // ตรวจสอบข้อมูล Seed ให้ครบก่อนแก้ไขข้อมูลในฐานข้อมูล
        for (SeedAssignmentRequest.SeedItem item : request.assignments()) {
            if (item == null || item.teamId() == null || item.seed() == null
                    || item.teamId() <= 0 || item.seed() <= 0) {
                throw new ValidationException(
                        "Team ID and seed must be positive");
            }

            if (!submittedTeamIds.add(item.teamId())) {
                throw new ValidationException(
                        "Duplicate team ID: " + item.teamId());
            }

            if (!submittedSeeds.add(item.seed())) {
                throw new ValidationException(
                        "Duplicate seed: " + item.seed());
            }

            if (!entriesByTeamId.containsKey(item.teamId())) {
                throw new ValidationException(
                        "Team " + item.teamId()
                                + " is not registered in this tournament");
            }
        }

        // บันทึก Seed หลังจากข้อมูลทั้งหมดผ่านการตรวจสอบแล้ว
        for (SeedAssignmentRequest.SeedItem item : request.assignments()) {
            entriesByTeamId.get(item.teamId()).setSeed(item.seed());
        }

        tournamentTeamRepository.saveAll(entries);

        return entries.stream()
                .map(this::toResponse)
                .toList();
    }

    private TournamentTeamResponse toResponse(TournamentTeam entry) {
        return new TournamentTeamResponse(
                entry.getTournament().getId(),
                entry.getTeam().getId(),
                entry.getTeamName(),
                entry.getJoinedAt(),
                entry.getSeed());
    }
}
