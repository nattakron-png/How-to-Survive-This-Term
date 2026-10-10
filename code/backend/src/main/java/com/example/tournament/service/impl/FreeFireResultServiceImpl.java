
package com.example.tournament.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.FreeFireGame;
import com.example.tournament.domain.entity.FreeFireGameResult;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.dto.request.FreeFireTeamResultRequest;
import com.example.tournament.dto.request.RecordFreeFireResultsRequest;
import com.example.tournament.dto.response.FreeFireGameResultsResponse;
import com.example.tournament.dto.response.FreeFireStandingsResponse;
import com.example.tournament.dto.response.FreeFireTeamResultResponse;
import com.example.tournament.dto.response.FreeFireGameSummaryResponse;
import com.example.tournament.event.FreeFireGameRecordedEvent;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.FreeFireGameRepository;
import com.example.tournament.repository.FreeFireGameResultRepository;
import com.example.tournament.repository.TournamentPlacementPointRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.FreeFireResultService;
import com.example.tournament.service.freefire.PointsCalculator;

@Service
@Transactional
public class FreeFireResultServiceImpl implements FreeFireResultService {

    private static final String COMPLETED = "COMPLETED";

    private final FreeFireGameRepository games;
    private final FreeFireGameResultRepository results;
    private final TournamentRepository tournaments;
    private final TournamentTeamRepository tournamentTeams;
    private final TournamentPlacementPointRepository placementPoints;
    private final PointsCalculator calculator;
    private final ApplicationEventPublisher events;

    public FreeFireResultServiceImpl(
            FreeFireGameRepository games,
            FreeFireGameResultRepository results,
            TournamentRepository tournaments,
            TournamentTeamRepository tournamentTeams,
            TournamentPlacementPointRepository placementPoints,
            PointsCalculator calculator,
            ApplicationEventPublisher events) {

        this.games = games;
        this.results = results;
        this.tournaments = tournaments;
        this.tournamentTeams = tournamentTeams;
        this.placementPoints = placementPoints;
        this.calculator = calculator;
        this.events = events;
    }

    // บันทึกผลการแข่งขัน Free Fire
    @Override
    public FreeFireGameResultsResponse record(
            Long gameId,
            RecordFreeFireResultsRequest request) {

        FreeFireGame game = findGame(gameId);
        Tournament tournament = game.getTournament();

        // ต้องเป็นทัวร์นาเมนต์รูปแบบเก็บคะแนน
        requirePointsFormat(tournament);

        // ห้ามบันทึกผลเกมเดิมซ้ำ
        if (COMPLETED.equals(game.getStatus())
                || results.existsByGameId(gameId)) {
            throw new BusinessException(
                    "Results for this game have already been recorded");
        }

        // ดึงทีมที่สมัครเข้าร่วมทัวร์นาเมนต์
        Map<Long, Team> participants = participantsOf(tournament.getId());

        if (request == null || request.results() == null) {
            throw new ValidationException("Results must not be empty");
        }

        List<FreeFireTeamResultRequest> rows = request.results();
        Set<Long> submittedTeams = new HashSet<>();

        // ตรวจว่าทีมไม่ซ้ำ และทุกทีมอยู่ในทัวร์นาเมนต์นี้
        for (FreeFireTeamResultRequest row : rows) {
            if (row == null || row.teamId() == null
                    || row.placement() == null || row.kills() == null) {
                throw new ValidationException(
                        "Team ID, placement and kills are required");
            }

            if (!submittedTeams.add(row.teamId())) {
                throw new ValidationException(
                        "Team " + row.teamId()
                                + " is submitted more than once");
            }

            if (!participants.containsKey(row.teamId())) {
                throw new ValidationException(
                        "Team " + row.teamId()
                                + " is not in this tournament");
            }

            if (row.placement() < 1
                    || row.placement() > participants.size()) {
                throw new ValidationException(
                        "Placement must be between 1 and "
                                + participants.size());
            }

            if (row.kills() < 0) {
                throw new ValidationException(
                        "Kills must not be negative");
            }
        }

        // ต้องส่งผลให้ครบทุกทีม
        if (submittedTeams.size() != participants.size()) {
            throw new ValidationException(
                    "Results must include all "
                            + participants.size() + " teams");
        }

        // ตรวจว่าอันดับไม่ซ้ำ
        Set<Integer> placements = new HashSet<>();

        for (FreeFireTeamResultRequest row : rows) {
            if (!placements.add(row.placement())) {
                throw new ValidationException(
                        "Placement " + row.placement()
                                + " is used more than once");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<FreeFireGameResult> toSave = new ArrayList<>();

        for (FreeFireTeamResultRequest row : rows) {
            FreeFireGameResult result = new FreeFireGameResult();

            result.setGame(game);
            result.setTournament(tournament);
            result.setTeam(participants.get(row.teamId()));
            result.setPlacement(row.placement().shortValue());
            result.setKills(row.kills().shortValue());
            result.setCreatedAt(now);

            toSave.add(result);
        }

        List<FreeFireGameResult> saved = results.saveAll(toSave);

        game.setStatus(COMPLETED);

        // แจ้งระบบให้ตรวจว่าเกมทั้งหมดแข่งขันครบหรือยัง
        events.publishEvent(
                new FreeFireGameRecordedEvent(
                        game.getId(), tournament.getId()));

        return toGameResponse(game, saved);
    }

    // ดูผลการแข่งขันของเกม
    @Override
    @Transactional(readOnly = true)
    public FreeFireGameResultsResponse getResults(Long gameId) {
        FreeFireGame game = findGame(gameId);

        return toGameResponse(
                game,
                results.findByGameIdOrderByPlacementAsc(gameId));
    }

    // ดูตารางคะแนนรวมของทัวร์นาเมนต์
    @Override
    @Transactional(readOnly = true)
    public FreeFireStandingsResponse standings(Long tournamentId) {
        Tournament tournament = tournaments.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tournament not found: " + tournamentId));

        requirePointsFormat(tournament);

        int totalGames = tournament.getTotalGames() == null
                ? 0
                : tournament.getTotalGames();

        List<Team> participants =
                new ArrayList<>(participantsOf(tournamentId).values());

        Map<Integer, Integer> table =
                PointsCalculator.toPlacementTable(
                        placementPoints.findByTournamentId(tournamentId));

        return new FreeFireStandingsResponse(
                tournamentId,
                totalGames,
                (int) games.countByTournamentIdAndStatus(
                        tournamentId, COMPLETED),
                calculator.standings(
                        participants,
                        results.findByTournamentId(tournamentId),
                        table,
                        tournament.getPointsPerKill(),
                        totalGames));
    }

    // แสดงรายการเกม Free Fire ของทัวร์นาเมนต์
    @Override
    @Transactional(readOnly = true)
    public List<FreeFireGameSummaryResponse> listGames(Long tournamentId) {
        // ตรวจสอบว่าทัวร์นาเมนต์มีอยู่จริง
        Tournament tournament = tournaments.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tournament not found: " + tournamentId));

        // ต้องเป็นรูปแบบเก็บคะแนน
        requirePointsFormat(tournament);

        // ดึงทีมอันดับ 1 ของแต่ละเกม
        Map<Long, Team> booyahByGame = results.findByTournamentId(tournamentId)
                .stream()
                .filter(result -> result.getPlacement() == 1)
                .collect(Collectors.toMap(
                        result -> result.getGame().getId(),
                        FreeFireGameResult::getTeam,
                        (first, second) -> first));

        // เรียงเกมตามหมายเลข และแสดง null หากเกมยังไม่มีผู้ชนะ
        return games.findByTournamentIdOrderByGameNumberAsc(tournamentId)
                .stream()
                .map(game -> {
                    Team booyah = booyahByGame.get(game.getId());

                    return new FreeFireGameSummaryResponse(
                            game.getId(),
                            (int) game.getGameNumber(),
                            game.getScheduledAt(),
                            game.getStatus(),
                            booyah == null ? null : booyah.getId(),
                            booyah == null ? null : booyah.getName());
                })
                .toList();
    }

    // ค้นหาเกม ถ้าไม่พบให้ตอบ 404
    private FreeFireGame findGame(Long gameId) {
        return games.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Free Fire game not found: " + gameId));
    }

    // ตรวจรูปแบบการแข่งขัน
    private static void requirePointsFormat(Tournament tournament) {
        if (tournament.getFormat() != TournamentFormat.POINTS) {
            throw new BusinessException(
                    "This tournament does not use the points format");
        }
    }

    // ดึงทีมที่เข้าร่วมทัวร์นาเมนต์
    private Map<Long, Team> participantsOf(Long tournamentId) {
        return tournamentTeams
                .findByTournamentId(tournamentId)
                .stream()
                .map(TournamentTeam::getTeam)
                .collect(Collectors.toMap(
                        Team::getId,
                        Function.identity()));
    }

    // สร้างผลการแข่งขันโดยใช้ชื่อทีมที่บันทึกไว้ตอนสมัคร
    private FreeFireGameResultsResponse toGameResponse(
            FreeFireGame game,
            List<FreeFireGameResult> rows) {

        Tournament tournament = game.getTournament();

        Map<Integer, Integer> table =
                PointsCalculator.toPlacementTable(
                        placementPoints.findByTournamentId(
                                tournament.getId()));

        int pointsPerKill = tournament.getPointsPerKill();

        // ใช้ชื่อทีมที่บันทึกไว้ใน tournament_teams
        Map<Long, String> teamNames = tournamentTeams
                .findByTournamentId(tournament.getId())
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getTeam().getId(),
                        TournamentTeam::getTeamName));

        List<FreeFireTeamResultResponse> teamRows = rows.stream()
                .sorted((first, second) -> Short.compare(
                        first.getPlacement(), second.getPlacement()))
                .map(result -> {
                    int placementScore = calculator.placementPoints(
                            result.getPlacement(), table);

                    int killScore = calculator.killPoints(
                            result.getKills(), pointsPerKill);

                    String teamName = teamNames.getOrDefault(
                            result.getTeam().getId(),
                            result.getTeam().getName());

                    return new FreeFireTeamResultResponse(
                            result.getTeam().getId(),
                            teamName,
                            (int) result.getPlacement(),
                            (int) result.getKills(),
                            placementScore,
                            killScore,
                            placementScore + killScore);
                })
                .toList();

        return new FreeFireGameResultsResponse(
                game.getId(),
                tournament.getId(),
                (int) game.getGameNumber(),
                game.getStatus(),
                teamRows);
    }
}
