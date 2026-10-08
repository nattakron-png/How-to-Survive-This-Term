package com.example.tournament.service.freefire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.tournament.domain.entity.FreeFireGame;
import com.example.tournament.domain.entity.FreeFireGameResult;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.dto.response.StandingRowResponse;

class PointsCalculatorTest {

    PointsCalculator calculator = new PointsCalculator();

    // ตารางคะแนนอันดับมาตรฐาน FFWS
    Map<Integer, Integer> ffws = Map.ofEntries(
            Map.entry(1, 12), Map.entry(2, 9), Map.entry(3, 8), Map.entry(4, 7),
            Map.entry(5, 6), Map.entry(6, 5), Map.entry(7, 4), Map.entry(8, 3),
            Map.entry(9, 2), Map.entry(10, 1), Map.entry(11, 0), Map.entry(12, 0));

    Team teamA = team(1L, "Alpha");
    Team teamB = team(2L, "Bravo");

    @Test
    void booyahWithNineKillsScoresTwentyOne() {
        assertEquals(21, calculator.gamePoints(1, 9, ffws, 1));
    }

    @Test
    void placementOutsideTableScoresOnlyKills() {
        assertEquals(3, calculator.gamePoints(13, 3, ffws, 1));
    }

    @Test
    void pointsPerKillMultipliesKills() {
        assertEquals(8 + 6, calculator.gamePoints(3, 3, ffws, 2));
    }

    @Test
    void higherTotalRanksFirst() {
        List<FreeFireGameResult> results = List.of(
                result(teamA, 1, 2, 0),   // 9
                result(teamB, 1, 1, 0));  // 12

        List<StandingRowResponse> rows = calculator.standings(List.of(teamA, teamB), results, ffws, 1, 10);

        assertEquals(2L, rows.get(0).teamId());
        assertEquals(12, rows.get(0).totalPoints());
    }

    @Test
    void tieOnPointsIsBrokenByBooyahs() {
        List<FreeFireGameResult> results = List.of(
                result(teamA, 1, 1, 0), result(teamA, 2, 5, 0),   // 12 + 6 = 18, Booyah 1
                result(teamB, 1, 2, 0), result(teamB, 2, 2, 0));  // 9 + 9 = 18, Booyah 0

        List<StandingRowResponse> rows = calculator.standings(List.of(teamB, teamA), results, ffws, 1, 10);

        assertEquals(18, rows.get(0).totalPoints());
        assertEquals(18, rows.get(1).totalPoints());
        assertEquals(1L, rows.get(0).teamId());
    }

    @Test
    void tieOnPointsAndBooyahsIsBrokenByKills() {
        List<FreeFireGameResult> results = List.of(
                result(teamA, 1, 3, 4),   // 8 + 4 = 12
                result(teamB, 1, 2, 3));  // 9 + 3 = 12

        List<StandingRowResponse> rows = calculator.standings(List.of(teamB, teamA), results, ffws, 1, 10);

        assertEquals(1L, rows.get(0).teamId());
    }

    @Test
    void remainingTieIsBrokenByLatestGamePlacement() {
        List<FreeFireGameResult> results = List.of(
                result(teamA, 1, 4, 2), result(teamA, 2, 3, 1),   // 9 + 9 = 18, kill 3, เกมล่าสุดอันดับ 3
                result(teamB, 1, 3, 1), result(teamB, 2, 4, 2));  // 9 + 9 = 18, kill 3, เกมล่าสุดอันดับ 4

        List<StandingRowResponse> rows = calculator.standings(List.of(teamB, teamA), results, ffws, 1, 10);

        assertEquals(1L, rows.get(0).teamId());
        assertEquals(1, rows.get(0).rank());
        assertEquals(2, rows.get(1).rank());
    }

    @Test
    void teamWithoutResultsStillAppearsWithEmptyGames() {
        List<StandingRowResponse> rows = calculator.standings(
                List.of(teamA, teamB), List.of(result(teamA, 1, 1, 5)), ffws, 1, 10);

        StandingRowResponse bravo = rows.get(1);
        assertEquals(2L, bravo.teamId());
        assertEquals(0, bravo.totalPoints());
        assertEquals(10, bravo.pointsPerGame().size());
        assertNull(bravo.pointsPerGame().get(0));
    }

    @Test
    void pointsPerGameIsPlacedAtTheGameNumber() {
        List<StandingRowResponse> rows = calculator.standings(
                List.of(teamA), List.of(result(teamA, 3, 1, 2)), ffws, 1, 10);

        List<Integer> games = new ArrayList<>(rows.get(0).pointsPerGame());
        assertNull(games.get(0));
        assertEquals(14, games.get(2));
    }

    private static Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private static FreeFireGameResult result(Team team, int gameNumber, int placement, int kills) {
        FreeFireGame game = new FreeFireGame();
        game.setGameNumber((short) gameNumber);
        FreeFireGameResult result = new FreeFireGameResult();
        result.setGame(game);
        result.setTeam(team);
        result.setPlacement((short) placement);
        result.setKills((short) kills);
        return result;
    }
}