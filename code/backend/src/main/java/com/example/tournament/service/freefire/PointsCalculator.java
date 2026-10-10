package com.example.tournament.service.freefire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.FreeFireGameResult;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.TournamentPlacementPoint;
import com.example.tournament.dto.response.StandingRowResponse;

@Component
public class PointsCalculator {

    /** แปลงตารางคะแนนอันดับจากฐานข้อมูลเป็น Map: อันดับ → คะแนน */
    public static Map<Integer, Integer> toPlacementTable(List<TournamentPlacementPoint> rows) {
        Map<Integer, Integer> table = new HashMap<>();
        for (TournamentPlacementPoint row : rows) {
            table.put(row.getPlacement().intValue(), row.getPoints().intValue());
        }
        return table;
    }

    /** คะแนนอันดับ อันดับที่ไม่อยู่ในตารางได้ 0 */
    public int placementPoints(int placement, Map<Integer, Integer> placementTable) {
        return placementTable.getOrDefault(placement, 0);
    }

    public int killPoints(int kills, int pointsPerKill) {
        return kills * pointsPerKill;
    }

    public int gamePoints(int placement, int kills, Map<Integer, Integer> placementTable, int pointsPerKill) {
        return placementPoints(placement, placementTable) + killPoints(kills, pointsPerKill);
    }

    /**
     * สร้างตารางคะแนนรวม
     * participants: ทีมทั้งหมดในรายการ (ทีมที่ยังไม่มีผลก็ต้องแสดง)
     * results: ผลทุกเกมที่บันทึกแล้วของรายการ
     */
    public List<StandingRowResponse> standings(List<Team> participants, List<FreeFireGameResult> results,
            Map<Integer, Integer> placementTable, int pointsPerKill, int totalGames) {

        Map<Long, Tally> tallies = new LinkedHashMap<>();
        for (Team team : participants) {
            tallies.put(team.getId(), new Tally(team, totalGames));
        }

        for (FreeFireGameResult result : results) {
            Tally tally = tallies.get(result.getTeam().getId());
            if (tally == null) {
                continue; // กันข้อมูลแปลก ๆ ปกติ composite FK ไม่ยอมให้เกิดอยู่แล้ว
            }
            int gameNumber = result.getGame().getGameNumber();
            int placement = result.getPlacement();
            int kills = result.getKills();
            int points = gamePoints(placement, kills, placementTable, pointsPerKill);

            tally.totalPoints += points;
            tally.kills += kills;
            if (placement == 1) {
                tally.booyahs++;
            }
            if (gameNumber >= 1 && gameNumber <= totalGames) {
                tally.pointsPerGame[gameNumber - 1] = points;
            }
            if (gameNumber > tally.latestGame) {
                tally.latestGame = gameNumber;
                tally.latestPlacement = placement;
            }
        }

        List<Tally> sorted = new ArrayList<>(tallies.values());
        sorted.sort(Comparator
                .comparingInt((Tally t) -> t.totalPoints).reversed()
                .thenComparing(Comparator.comparingInt((Tally t) -> t.booyahs).reversed())
                .thenComparing(Comparator.comparingInt((Tally t) -> t.kills).reversed())
                .thenComparingInt(t -> t.latestPlacement)
                .thenComparing(t -> t.team.getName(), Comparator.nullsLast(Comparator.naturalOrder())));

        List<StandingRowResponse> rows = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Tally t = sorted.get(i);
            rows.add(new StandingRowResponse(i + 1, t.team.getId(), t.team.getName(),
                    t.totalPoints, t.booyahs, t.kills, Arrays.asList(t.pointsPerGame)));
        }
        return rows;
    }

    /** ตัวนับคะแนนของหนึ่งทีม ใช้ภายในคลาสนี้เท่านั้น */
    private static final class Tally {
        final Team team;
        final Integer[] pointsPerGame; // null = เกมที่ยังไม่แข่ง
        int totalPoints;
        int booyahs;
        int kills;
        int latestGame;
        int latestPlacement = Integer.MAX_VALUE; // ยังไม่มีผล = แย่ที่สุด

        Tally(Team team, int totalGames) {
            this.team = team;
            this.pointsPerGame = new Integer[totalGames];
        }
    }
}