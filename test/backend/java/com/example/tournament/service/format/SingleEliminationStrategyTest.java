package com.example.tournament.service.format;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.ValidationException;

class SingleEliminationStrategyTest {

    SingleEliminationStrategy strategy = new SingleEliminationStrategy(null); // buildBracket ไม่ใช้ repository
    Tournament tournament = new Tournament();

    @Test
    void seedOrderForEight() {
        assertArrayEquals(new int[] {1, 8, 4, 5, 2, 7, 3, 6}, SingleEliminationStrategy.seedOrder(8));
    }

    @Test
    void twoTeamsMakeOneFinal() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(2));

        assertEquals(1, bracket.size());
        assertNull(bracket.get(0).getNextMatch());
        assertEquals("SCHEDULED", bracket.get(0).getStatus());
    }

    @Test
    void eightTeamsMakeSevenMatchesWithStandardSeeding() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(8));

        assertEquals(7, bracket.size());
        assertEquals(4, countRound(bracket, 1));
        Match first = find(bracket, 1, 1);
        assertEquals(1L, first.getTeamA().getId());
        assertEquals(8L, first.getTeamB().getId());
    }

    @Test
    void winnersOfAdjacentMatchesMeetInTheSameNextMatch() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(8));

        assertSame(find(bracket, 2, 1), find(bracket, 1, 1).getNextMatch());
        assertSame(find(bracket, 2, 1), find(bracket, 1, 2).getNextMatch());
        assertSame(find(bracket, 2, 2), find(bracket, 1, 3).getNextMatch());
    }

    @Test
    void fiveTeamsGiveThreeByes() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(5));

        assertEquals(4, bracket.size()); // จำนวนทีม − 1
        assertEquals(1, countRound(bracket, 1));

        Match onlyFirstRound = find(bracket, 1, 2);
        assertEquals(4L, onlyFirstRound.getTeamA().getId());
        assertEquals(5L, onlyFirstRound.getTeamB().getId());

        Match semi1 = find(bracket, 2, 1);
        assertEquals(1L, semi1.getTeamA().getId());
        assertNull(semi1.getTeamB());
        assertEquals("PENDING", semi1.getStatus());

        Match semi2 = find(bracket, 2, 2);
        assertEquals(2L, semi2.getTeamA().getId());
        assertEquals(3L, semi2.getTeamB().getId());
        assertEquals("SCHEDULED", semi2.getStatus());
    }

    @Test
    void finalComesFirstSoItIsSavedFirst() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(8));

        assertNull(bracket.get(0).getNextMatch());
    }

    @Test
    void rejectsFewerThanTwoTeams() {
        assertThrows(ValidationException.class, () -> strategy.buildBracket(tournament, teams(1)));
    }

    private static List<Team> teams(int count) {
        List<Team> teams = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            Team team = new Team();
            team.setId(id);
            teams.add(team);
        }
        return teams;
    }

    private static long countRound(List<Match> bracket, int round) {
        return bracket.stream().filter(m -> m.getRoundNumber() == round).count();
    }

    private static Match find(List<Match> bracket, int round, int number) {
        return bracket.stream()
                .filter(m -> m.getRoundNumber() == round && m.getMatchNumber() == number)
                .findFirst()
                .orElseThrow();
    }
}
