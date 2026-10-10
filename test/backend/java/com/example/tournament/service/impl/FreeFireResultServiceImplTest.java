
package com.example.tournament.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.tournament.domain.entity.FreeFireGame;
import com.example.tournament.domain.entity.FreeFireGameResult;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentPlacementPoint;
import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.dto.request.FreeFireTeamResultRequest;
import com.example.tournament.dto.request.RecordFreeFireResultsRequest;
import com.example.tournament.dto.response.FreeFireGameResultsResponse;
import com.example.tournament.dto.response.FreeFireGameSummaryResponse;
import com.example.tournament.dto.response.FreeFireStandingsResponse;
import com.example.tournament.event.FreeFireGameRecordedEvent;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.FreeFireGameRepository;
import com.example.tournament.repository.FreeFireGameResultRepository;
import com.example.tournament.repository.TournamentPlacementPointRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.freefire.PointsCalculator;

@ExtendWith(MockitoExtension.class)
class FreeFireResultServiceImplTest {

    @Mock
    FreeFireGameRepository games;

    @Mock
    FreeFireGameResultRepository results;

    @Mock
    TournamentRepository tournaments;

    @Mock
    TournamentTeamRepository tournamentTeams;

    @Mock
    TournamentPlacementPointRepository placementPoints;

    @Mock
    ApplicationEventPublisher events;

    FreeFireResultServiceImpl service;
    Tournament tournament;
    FreeFireGame game;

    @BeforeEach
    void setUp() {
        service = new FreeFireResultServiceImpl(
                games,
                results,
                tournaments,
                tournamentTeams,
                placementPoints,
                new PointsCalculator(),
                events);

        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setFormat(TournamentFormat.POINTS);
        tournament.setTotalGames((short) 10);
        tournament.setPointsPerKill((short) 1);

        game = new FreeFireGame();
        game.setId(5L);
        game.setTournament(tournament);
        game.setGameNumber((short) 7);
        game.setStatus("SCHEDULED");
    }

    // ---------- record: กฎการกรอกผล ----------

    @Test
    void rejectsWhenGameNotFound() {
        when(games.findById(5L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.record(5L, validRequest()));
    }

    @Test
    void rejectsWhenTournamentIsNotPointsFormat() {
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        givenGameWithThreeTeams();

        assertThrows(
                BusinessException.class,
                () -> service.record(5L, validRequest()));
    }

    @Test
    void rejectsWhenGameAlreadyCompleted() {
        game.setStatus("COMPLETED");
        givenGameWithThreeTeams();

        assertThrows(
                BusinessException.class,
                () -> service.record(5L, validRequest()));
    }

    @Test
    void rejectsWhenResultsAlreadyExist() {
        givenGameWithThreeTeams();
        when(results.existsByGameId(5L)).thenReturn(true);

        assertThrows(
                BusinessException.class,
                () -> service.record(5L, validRequest()));
    }

    @Test
    void rejectsDuplicateTeam() {
        givenGameWithThreeTeams();

        assertThrows(
                ValidationException.class,
                () -> service.record(
                        5L,
                        request(
                                row(10L, 1, 5),
                                row(10L, 2, 3),
                                row(30L, 3, 0))));
    }

    @Test
    void rejectsTeamNotInTournament() {
        givenGameWithThreeTeams();

        assertThrows(
                ValidationException.class,
                () -> service.record(
                        5L,
                        request(
                                row(10L, 1, 5),
                                row(20L, 2, 3),
                                row(99L, 3, 0))));
    }

    @Test
    void rejectsMissingTeam() {
        givenGameWithThreeTeams();

        assertThrows(
                ValidationException.class,
                () -> service.record(
                        5L,
                        request(
                                row(10L, 1, 5),
                                row(20L, 2, 3))));
    }

    @Test
    void rejectsDuplicatePlacement() {
        givenGameWithThreeTeams();

        assertThrows(
                ValidationException.class,
                () -> service.record(
                        5L,
                        request(
                                row(10L, 1, 5),
                                row(20L, 1, 3),
                                row(30L, 3, 0))));
    }

    @Test
    void rejectsPlacementOutOfRange() {
        givenGameWithThreeTeams();

        assertThrows(
                ValidationException.class,
                () -> service.record(
                        5L,
                        request(
                                row(10L, 1, 5),
                                row(20L, 2, 3),
                                row(30L, 4, 0))));
    }

    @Test
    void doesNotSaveOrPublishWhenRejected() {
        givenGameWithThreeTeams();

        assertThrows(
                ValidationException.class,
                () -> service.record(
                        5L,
                        request(
                                row(10L, 1, 5),
                                row(20L, 2, 3))));

        verify(results, never()).saveAll(anyList());
        verify(events, never()).publishEvent(any());
    }

    // ---------- record: กรณีสำเร็จ ----------

    @Test
    void savesResultsComputesPointsAndPublishesEvent() {
        givenGameWithThreeTeams();
        when(results.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FreeFireGameResultsResponse response =
                service.record(5L, validRequest());

        assertEquals(3, response.results().size());
        assertEquals(10L, response.results().get(0).teamId());
        assertEquals(12 + 5, response.results().get(0).totalPoints());
        assertEquals(9 + 3, response.results().get(1).totalPoints());
        assertEquals("COMPLETED", game.getStatus());

        verify(events).publishEvent(
                new FreeFireGameRecordedEvent(5L, 1L));
    }

    // ---------- standings ----------

    @Test
    void standingsRejectsSingleEliminationTournament() {
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        when(tournaments.findById(1L))
                .thenReturn(Optional.of(tournament));

        assertThrows(
                BusinessException.class,
                () -> service.standings(1L));
    }

    @Test
    void standingsListsAllTeamsWithTotalGames() {
        when(tournaments.findById(1L))
                .thenReturn(Optional.of(tournament));

        givenThreeTeams();

        when(results.findByTournamentId(1L)).thenReturn(List.of());
        when(games.countByTournamentIdAndStatus(1L, "COMPLETED"))
                .thenReturn(0L);

        FreeFireStandingsResponse response = service.standings(1L);

        assertEquals(10, response.totalGames());
        assertEquals(3, response.standings().size());
        assertEquals(
                10,
                response.standings().get(0).pointsPerGame().size());
    }

    // ---------- helpers ----------

    private void givenGameWithThreeTeams() {
        when(games.findById(5L)).thenReturn(Optional.of(game));
        lenient().when(results.existsByGameId(5L)).thenReturn(false);

        givenThreeTeams();
    }

    private void givenThreeTeams() {
        lenient().when(tournamentTeams.findByTournamentId(1L))
                .thenReturn(List.of(
                        joined(10L, "Alpha"),
                        joined(20L, "Bravo"),
                        joined(30L, "Charlie")));

        lenient().when(placementPoints.findByTournamentId(1L))
                .thenReturn(List.of(
                        points(1, 12),
                        points(2, 9),
                        points(3, 8)));
    }

    private static RecordFreeFireResultsRequest validRequest() {
        return request(
                row(10L, 1, 5),
                row(20L, 2, 3),
                row(30L, 3, 0));
    }

    private static RecordFreeFireResultsRequest request(
            FreeFireTeamResultRequest... rows) {
        return new RecordFreeFireResultsRequest(List.of(rows));
    }

    private static FreeFireTeamResultRequest row(
            Long teamId, int placement, int kills) {
        return new FreeFireTeamResultRequest(teamId, placement, kills);
    }

    private static TournamentTeam joined(Long teamId, String name) {
        Team team = new Team();
        team.setId(teamId);
        team.setName(name);

        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setTeam(team);

        // กำหนดชื่อทีมที่บันทึกไว้ตอนสมัครเข้าร่วมการแข่งขัน
        tournamentTeam.setTeamName(name);

        return tournamentTeam;
    }

    private static TournamentPlacementPoint points(
            int placement, int value) {
        TournamentPlacementPoint point = new TournamentPlacementPoint();
        point.setPlacement((short) placement);
        point.setPoints((short) value);
        return point;
    }

    // ---------- listGames: ตารางเกม ----------

    @Test
    void listGamesRejectsUnknownTournament() {
        when(tournaments.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.listGames(1L));
    }

    @Test
    void listGamesRejectsNonPointsTournament() {
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        when(tournaments.findById(1L))
                .thenReturn(Optional.of(tournament));

        assertThrows(
                BusinessException.class,
                () -> service.listGames(1L));
    }

    @Test
    void listGamesReturnsGamesInOrderWithBooyahTeam() {
        FreeFireGame g1 = gameOf(10L, 1, "COMPLETED");
        FreeFireGame g2 = gameOf(11L, 2, "SCHEDULED");

        Team winner = teamOf(21L, "Blue Wave");
        Team second = teamOf(22L, "Fire Ants");

        when(tournaments.findById(1L))
                .thenReturn(Optional.of(tournament));

        when(games.findByTournamentIdOrderByGameNumberAsc(1L))
                .thenReturn(List.of(g1, g2));

        when(results.findByTournamentId(1L))
                .thenReturn(List.of(
                        resultOf(g1, second, 2),
                        resultOf(g1, winner, 1)));

        List<FreeFireGameSummaryResponse> list =
                service.listGames(1L);

        assertEquals(2, list.size());
        assertEquals(1, list.get(0).gameNumber());
        assertEquals("COMPLETED", list.get(0).status());
        assertEquals(21L, list.get(0).booyahTeamId());
        assertEquals("Blue Wave", list.get(0).booyahTeamName());
    }

    @Test
    void listGamesLeavesBooyahEmptyForUnplayedGame() {
        FreeFireGame g2 = gameOf(11L, 2, "SCHEDULED");

        when(tournaments.findById(1L))
                .thenReturn(Optional.of(tournament));

        when(games.findByTournamentIdOrderByGameNumberAsc(1L))
                .thenReturn(List.of(g2));

        when(results.findByTournamentId(1L)).thenReturn(List.of());

        FreeFireGameSummaryResponse only =
                service.listGames(1L).get(0);

        assertEquals("SCHEDULED", only.status());
        assertNull(only.booyahTeamId());
        assertNull(only.booyahTeamName());
    }

    private FreeFireGame gameOf(
            Long id, int number, String status) {
        FreeFireGame g = new FreeFireGame();
        g.setId(id);
        g.setTournament(tournament);
        g.setGameNumber((short) number);
        g.setStatus(status);
        return g;
    }

    private static Team teamOf(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private FreeFireGameResult resultOf(
            FreeFireGame game, Team team, int placement) {
        FreeFireGameResult result = new FreeFireGameResult();
        result.setGame(game);
        result.setTournament(tournament);
        result.setTeam(team);
        result.setPlacement((short) placement);
        result.setKills((short) 0);
        return result;
    }
}
