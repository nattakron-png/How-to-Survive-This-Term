package com.example.tournament.service.format;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        // 2. เรียงจากนัดชิงลงมา เพื่อให้ save แมตช์ปลายทางก่อน
        List<Match> result = new ArrayList<>();
        for (int r = rounds - 1; r >= 0; r--) {
            result.addAll(byRound.get(r));
        }
        return result;
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
