package com.example.tournament.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.repository.MatchRepository;

class BracketProgressionListenerTest {

    MatchRepository matches = mock(MatchRepository.class);
    BracketProgressionListener listener = new BracketProgressionListener(matches);

    @Test
    void oddMatchWinnerGoesToSlotA() {
        Match next = new Match();
        givenMatch(1, next);

        listener.onResultRecorded(new MatchResultRecordedEvent(1L, 10L));

        assertEquals(10L, next.getTeamA().getId());
        assertNull(next.getTeamB());
    }

    @Test
    void evenMatchWinnerGoesToSlotB() {
        Match next = new Match();
        givenMatch(2, next);

        listener.onResultRecorded(new MatchResultRecordedEvent(1L, 20L));

        assertEquals(20L, next.getTeamB().getId());
        assertNull(next.getTeamA());
    }

    @Test
    void nextMatchBecomesScheduledWhenBothTeamsKnown() {
        Match next = new Match();
        next.setTeamA(team(99L));
        givenMatch(2, next);

        listener.onResultRecorded(new MatchResultRecordedEvent(1L, 10L));

        assertEquals("SCHEDULED", next.getStatus());
    }

    @Test
    void rejectsWhenSlotIsTakenByAnotherTeam() {
        Match next = new Match();
        next.setTeamA(team(99L));
        givenMatch(1, next);

        assertThrows(BusinessException.class,
                () -> listener.onResultRecorded(new MatchResultRecordedEvent(1L, 10L)));
    }

    @Test
    void finalMatchCompletesTournament() {
        Match fin = givenMatch(1, null);

        listener.onResultRecorded(new MatchResultRecordedEvent(1L, 10L));

        assertEquals(TournamentStatus.COMPLETED, fin.getTournament().getStatus());
    }

    private Match givenMatch(int matchNumber, Match next) {
        Match match = new Match();
        match.setId(1L);
        match.setMatchNumber(matchNumber);
        match.setTeamA(team(10L));
        match.setTeamB(team(20L));
        match.setNextMatch(next);
        match.setTournament(new Tournament());
        when(matches.findById(1L)).thenReturn(Optional.of(match));
        return match;
    }

    private static Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }
}