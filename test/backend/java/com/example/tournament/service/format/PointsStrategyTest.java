package com.example.tournament.service.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tournament.domain.entity.FreeFireGame;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.FreeFireGameRepository;

@ExtendWith(MockitoExtension.class)
class PointsStrategyTest {

    @Mock FreeFireGameRepository games;
    @Captor ArgumentCaptor<List<FreeFireGame>> saved;

    @Test
    void handlesPointsFormat() {
        assertEquals(TournamentFormat.POINTS, new PointsStrategy(games).format());
    }

    @Test
    void createsOneGamePerTotalGamesNumberedFromOne() {
        PointsStrategy strategy = new PointsStrategy(games);
        Tournament tournament = pointsTournament((short) 10);

        int created = strategy.createSchedule(tournament, List.of(new Team(), new Team()));

        assertEquals(10, created);
        verify(games).saveAll(saved.capture());
        List<FreeFireGame> result = saved.getValue();
        assertEquals(10, result.size());
        for (int i = 0; i < result.size(); i++) {
            FreeFireGame game = result.get(i);
            assertEquals((short) (i + 1), game.getGameNumber());
            assertEquals("SCHEDULED", game.getStatus());
            assertSame(tournament, game.getTournament());
        }
    }

    @Test
    void rejectsWhenTotalGamesIsMissing() {
        PointsStrategy strategy = new PointsStrategy(games);
        Tournament tournament = pointsTournament(null);

        assertThrows(ValidationException.class, () -> strategy.createSchedule(tournament, List.of()));
        verify(games, never()).saveAll(anyList());
    }

    private static Tournament pointsTournament(Short totalGames) {
        Tournament tournament = new Tournament();
        tournament.setFormat(TournamentFormat.POINTS);
        tournament.setTotalGames(totalGames);
        return tournament;
    }
}
