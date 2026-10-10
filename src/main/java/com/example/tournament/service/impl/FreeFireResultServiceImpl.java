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

    public FreeFireResultServiceImpl(FreeFireGameRepository games, FreeFireGameResultRepository results,
            TournamentRepository tournaments, TournamentTeamRepository tournamentTeams,
            TournamentPlacementPointRepository placementPoints, PointsCalculator calculator,
            ApplicationEventPublisher events) {
        this.games = games;
        this.results = results;
        this.tournaments = tournaments;
        this.tournamentTeams = tournamentTeams;
        this.placementPoints = placementPoints;
        this.calculator = calculator;
        this.events = events;
    }

    @Override
    public FreeFireGameResultsResponse record(Long gameId, RecordFreeFireResultsRequest request) {
        // 1. เกมต้องมีอยู่จริง → 404
        FreeFireGame game = findGame(gameId);
        Tournament tournament = game.getTournament();

        // 2. ต้องเป็นรายการแบบเก็บคะแนน → 409
        requirePointsFormat(tournament);

        // 3. เกมนี้ต้องยังไม่มีผล → 409
        if (COMPLETED.equals(game.getStatus()) || results.existsByGameId(gameId)) {
            throw new BusinessException("Results for this game have already been recorded");
        }

        // 4. ต้องกรอกครบทุกทีมในรายการ ไม่ซ้ำ และไม่มีทีมนอกรายการ → 400
        Map<Long, Team> participants = participantsOf(tournament.getId());
        List<FreeFireTeamResultRequest> rows = request.results();
        Set<Long> submittedTeams = new HashSet<>();
        for (FreeFireTeamResultRequest row : rows) {
            if (!submittedTeams.add(row.teamId())) {
                throw new ValidationException("Team " + row.teamId() + " is submitted more than once");
            }
            if (!participants.containsKey(row.teamId())) {
                throw new ValidationException("Team " + row.teamId() + " is not in this tournament");
            }
        }
        if (submittedTeams.size() != participants.size()) {
            throw new ValidationException("Results must include all " + participants.size() + " teams");
        }

        // 5. อันดับต้องไม่ซ้ำ และอยู่ระหว่าง 1 ถึงจำนวนทีม → 400
        Set<Integer> placements = new HashSet<>();
        for (FreeFireTeamResultRequest row : rows) {
            if (row.placement() > participants.size()) {
                throw new ValidationException("Placement must be between 1 and " + participants.size());
            }
            if (!placements.add(row.placement())) {
                throw new ValidationException("Placement " + row.placement() + " is used more than once");
            }
        }

        // 6. kill ไม่ติดลบ ตรวจแล้วใน DTO ด้วย @Min(0) → 400

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

        // Observer: ประกาศว่าเกมนี้มีผลแล้ว ให้ Listener เช็กว่าครบทุกเกมหรือยัง
        events.publishEvent(new FreeFireGameRecordedEvent(game.getId(), tournament.getId()));

        return toGameResponse(game, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FreeFireGameResultsResponse getResults(Long gameId) {
        FreeFireGame game = findGame(gameId);
        return toGameResponse(game, results.findByGameIdOrderByPlacementAsc(gameId));
    }

    @Override
    @Transactional(readOnly = true)
    public FreeFireStandingsResponse standings(Long tournamentId) {
        Tournament tournament = tournaments.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + tournamentId));
        requirePointsFormat(tournament);

        int totalGames = tournament.getTotalGames() == null ? 0 : tournament.getTotalGames();
        List<Team> participants = new ArrayList<>(participantsOf(tournamentId).values());
        Map<Integer, Integer> table = PointsCalculator.toPlacementTable(
                placementPoints.findByTournamentId(tournamentId));

        return new FreeFireStandingsResponse(
                tournamentId,
                totalGames,
                (int) games.countByTournamentIdAndStatus(tournamentId, COMPLETED),
                calculator.standings(participants, results.findByTournamentId(tournamentId),
                        table, tournament.getPointsPerKill(), totalGames));
    }

        @Override
    @Transactional(readOnly = true)
    public List<FreeFireGameSummaryResponse> listGames(Long tournamentId) {
        // 1. รายการต้องมีอยู่จริง → 404
        Tournament tournament = tournaments.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + tournamentId));

        // 2. ต้องเป็นแบบเก็บคะแนน → 409
        requirePointsFormat(tournament);

        // 3. หาทีมอันดับ 1 (Booyah) ของแต่ละเกม จากผลทั้งรายการในคำสั่งเดียว
        Map<Long, Team> booyahByGame = results.findByTournamentId(tournamentId).stream()
                .filter(r -> r.getPlacement() == 1)
                .collect(Collectors.toMap(r -> r.getGame().getId(), FreeFireGameResult::getTeam, (a, b) -> a));

        // 4. เรียงตามเลขเกม เกมที่ยังไม่แข่งจะไม่มี Booyah (null)
        return games.findByTournamentIdOrderByGameNumberAsc(tournamentId).stream()
                .map(g -> {
                    Team booyah = booyahByGame.get(g.getId());
                    return new FreeFireGameSummaryResponse(
                            g.getId(),
                            (int) g.getGameNumber(),
                            g.getScheduledAt(),
                            g.getStatus(),
                            booyah == null ? null : booyah.getId(),
                            booyah == null ? null : booyah.getName());
                })
                .toList();
    }

    private FreeFireGame findGame(Long gameId) {
        return games.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Free Fire game not found: " + gameId));
    }

    private static void requirePointsFormat(Tournament tournament) {
        if (tournament.getFormat() != TournamentFormat.POINTS) {
            throw new BusinessException("This tournament does not use the points format");
        }
    }

    private Map<Long, Team> participantsOf(Long tournamentId) {
        return tournamentTeams.findByTournamentId(tournamentId).stream()
                .map(TournamentTeam::getTeam)
                .collect(Collectors.toMap(Team::getId, Function.identity()));
    }

    private FreeFireGameResultsResponse toGameResponse(FreeFireGame game, List<FreeFireGameResult> rows) {
        Tournament tournament = game.getTournament();
        Map<Integer, Integer> table = PointsCalculator.toPlacementTable(
                placementPoints.findByTournamentId(tournament.getId()));
        int pointsPerKill = tournament.getPointsPerKill();

        List<FreeFireTeamResultResponse> teamRows = rows.stream()
                .sorted((a, b) -> Short.compare(a.getPlacement(), b.getPlacement()))
                .map(r -> {
                    int placementScore = calculator.placementPoints(r.getPlacement(), table);
                    int killScore = calculator.killPoints(r.getKills(), pointsPerKill);
                    return new FreeFireTeamResultResponse(r.getTeam().getId(), r.getTeam().getName(),
                            (int) r.getPlacement(), (int) r.getKills(),
                            placementScore, killScore, placementScore + killScore);
                })
                .toList();

        return new FreeFireGameResultsResponse(game.getId(), tournament.getId(),
                (int) game.getGameNumber(), game.getStatus(), teamRows);
    }
}