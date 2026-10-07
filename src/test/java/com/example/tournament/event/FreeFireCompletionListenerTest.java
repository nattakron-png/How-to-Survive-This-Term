package com.example.tournament.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.FreeFireGameRepository;
import com.example.tournament.repository.TournamentRepository;

class FreeFireCompletionListenerTest {

    TournamentRepository tournaments = mock(TournamentRepository.class);
    FreeFireGameRepository games = mock(FreeFireGameRepository.class);
    FreeFireCompletionListener listener = new FreeFireCompletionListener(tournaments, games);

    Tournament tournament;

    @BeforeEach
    void setUp() {
        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus("ONGOING");
        tournament.setTotalGames((short) 10);
    }

    @Test
    void keepsTournamentOngoingBeforeLastGame() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(games.countByTournamentIdAndStatus(1L, "COMPLETED")).thenReturn(9L);

        listener.onGameRecorded(new FreeFireGameRecordedEvent(5L, 1L));

        assertEquals("ONGOING", tournament.getStatus());
    }

    @Test
    void completesTournamentAfterLastGame() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(games.countByTournamentIdAndStatus(1L, "COMPLETED")).thenReturn(10L);

        listener.onGameRecorded(new FreeFireGameRecordedEvent(5L, 1L));

        assertEquals("COMPLETED", tournament.getStatus());
    }

    @Test
    void ignoresTournamentWithoutTotalGames() {
        tournament.setTotalGames(null);
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));

        listener.onGameRecorded(new FreeFireGameRecordedEvent(5L, 1L));

        assertEquals("ONGOING", tournament.getStatus());
        verify(games, never()).countByTournamentIdAndStatus(anyLong(), anyString());
    }

    @Test
    void rejectsWhenTournamentNotFound() {
        when(tournaments.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listener.onGameRecorded(new FreeFireGameRecordedEvent(5L, 1L)));
    }
}