package com.example.tournament.service.format;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.MatchStatus;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.MatchRepository;

@Component
public class SingleEliminationStrategy implements FormatStrategy {

    private final MatchRepository matches;

    public SingleEliminationStrategy(MatchRepository matches) {
        this.matches = matches;
    }

    @Override
    public TournamentFormat format() {
        return TournamentFormat.SINGLE_ELIMINATION;
    }

    @Override
    public int createSchedule(Tournament tournament, List<Team> teamsInSeedOrder) {
        List<Match> bracket = buildBracket(tournament, teamsInSeedOrder);
        matches.saveAll(bracket); // เรียงจากนัดชิงลงมาแล้ว
        return bracket.size();
    }

    /** สร้างสายในหน่วยความจำ ผลลัพธ์เรียงจากนัดชิงลงมา */
    public List<Match> buildBracket(Tournament tournament, List<Team> teams) {
        if (teams.size() < 2) {
            throw new ValidationException("At least 2 teams are required to build a bracket");
        }

        int size = 2;
        while (size < teams.size()) {
            size *= 2;
        }
        int rounds = Integer.numberOfTrailingZeros(size);
        LocalDateTime now = LocalDateTime.now();

        // 1. สร้างแมตช์ทุกตำแหน่งของทุกรอบ รอบ r มี size / 2^r แมตช์ match_number นับใหม่ทุกรอบ
        List<List<Match>> byRound = new ArrayList<>();
        for (int r = 1; r <= rounds; r++) {
            List<Match> round = new ArrayList<>();
            for (int k = 1; k <= (size >> r); k++) {
                Match match = new Match();
                match.setTournament(tournament);
                match.setRoundNumber(r);
                match.setMatchNumber(k);
                match.setStatus(MatchStatus.PENDING.name());
                match.setCreatedAt(now);
                round.add(match);
            }
            byRound.add(round);
        }

        // 2. ผูก next_match: รอบ r แมตช์ k → รอบ r+1 แมตช์ ceil(k/2) (นัดชิงเป็น null)
        for (int r = 0; r < rounds - 1; r++) {
            List<Match> current = byRound.get(r);
            List<Match> next = byRound.get(r + 1);
            for (int k = 0; k < current.size(); k++) {
                current.get(k).setNextMatch(next.get(k / 2));
            }
        }

        // 3. ใส่ทีมรอบแรกตามลำดับ seed มาตรฐาน และจัดการบาย
        int[] order = seedOrder(size);
        Set<Match> byes = new HashSet<>();
        List<Match> firstRound = byRound.get(0);
        for (int k = 0; k < firstRound.size(); k++) {
            Match match = firstRound.get(k);
            Team teamA = teamAtSeed(teams, order[2 * k]);
            Team teamB = teamAtSeed(teams, order[2 * k + 1]);
            if (teamA != null && teamB != null) {
                match.setTeamA(teamA);
                match.setTeamB(teamB);
                match.setStatus(MatchStatus.SCHEDULED.name());
            } else {
                // บาย: ส่งทีมที่มีไปรอบถัดไปทันที และไม่บันทึกแมตช์นี้
                placeInNextMatch(match, teamA != null ? teamA : teamB);
                byes.add(match);
            }
        }

        // 4. เรียงจากนัดชิงลงมา เพื่อให้ save แมตช์ปลายทางก่อน
        List<Match> result = new ArrayList<>();
        for (int r = rounds - 1; r >= 0; r--) {
            for (Match match : byRound.get(r)) {
                if (!byes.contains(match)) {
                    result.add(match);
                }
            }
        }
        return result;
    }

    /** เลขคี่ไปช่อง A เลขคู่ไปช่อง B ตรงกับ BracketProgressionListener */
    private static void placeInNextMatch(Match from, Team team) {
        Match next = from.getNextMatch();
        if (from.getMatchNumber() % 2 == 1) {
            next.setTeamA(team);
        } else {
            next.setTeamB(team);
        }
        if (next.getTeamA() != null && next.getTeamB() != null) {
            next.setStatus(MatchStatus.SCHEDULED.name());
        }
    }

    private static Team teamAtSeed(List<Team> teams, int seed) {
        return seed <= teams.size() ? teams.get(seed - 1) : null;
    }

    /** ลำดับ seed ในสาย เช่น 8 ทีม → 1, 8, 4, 5, 2, 7, 3, 6 */
    static int[] seedOrder(int size) {
        int[] order = {1};
        while (order.length < size) {
            int length = order.length * 2;
            int[] next = new int[length];
            for (int i = 0; i < order.length; i++) {
                next[2 * i] = order[i];
                next[2 * i + 1] = length + 1 - order[i];
            }
            order = next;
        }
        return order;
    }
}
