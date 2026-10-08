package com.example.tournament.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.MatchResult;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.enums.MatchStatus;
import com.example.tournament.dto.request.CreateMatchResultRequest;
import com.example.tournament.dto.response.MatchResultResponse;
import com.example.tournament.event.MatchResultRecordedEvent;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.mapper.MatchResultMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.MatchResultRepository;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceImplTest {

    @Mock
    MatchRepository matches;

    @Mock
    MatchResultRepository results;

    @Mock
    ApplicationEventPublisher events;

    MatchResultServiceImpl service;
    Match match;

    @BeforeEach
    void setUp() {
        service = new MatchResultServiceImpl(
                matches,
                results,
                new MatchResultMapper(),
                events);

        match = new Match();
        match.setId(1L);
        match.setMatchNumber(1);
        match.setTeamA(team(10L));
        match.setTeamB(team(20L));
    }

    @Test
    void rejectsWhenMatchNotFound() {
        when(matches.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.record(1L, request(2, 1, 10L)));
    }

    @Test
    void rejectsWhenResultAlreadyExists() {
        givenMatchWithoutResult();
        when(results.existsByMatchId(1L)).thenReturn(true);

        assertThrows(
                BusinessException.class,
                () -> service.record(1L, request(2, 1, 10L)));
    }

    @Test
    void rejectsWhenTeamIsMissing() {
        match.setTeamB(null);
        givenMatchWithoutResult();

        assertThrows(
                BusinessException.class,
                () -> service.record(1L, request(2, 1, 10L)));
    }

    @Test
    void rejectsWhenMatchIsNotScheduled() {
        givenMatchWithoutResult();

        match.setStatus(MatchStatus.PENDING.name());

        assertThrows(
                BusinessException.class,
                () -> service.record(1L, request(2, 1, 10L)));

        verify(results, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void rejectsWhenMatchIsAlreadyCompleted() {
        givenMatchWithoutResult();

        match.setStatus(MatchStatus.COMPLETED.name());

        assertThrows(
                BusinessException.class,
                () -> service.record(1L, request(2, 1, 10L)));

        verify(results, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void rejectsDraw() {
        givenScheduledMatch();

        assertThrows(
                ValidationException.class,
                () -> service.record(1L, request(1, 1, 10L)));
    }

    @Test
    void rejectsWinnerNotInMatch() {
        givenScheduledMatch();

        assertThrows(
                ValidationException.class,
                () -> service.record(1L, request(2, 1, 30L)));
    }

    @Test
    void rejectsWinnerWithLowerScore() {
        givenScheduledMatch();

        assertThrows(
                ValidationException.class,
                () -> service.record(1L, request(3, 1, 20L)));
    }

    @Test
    void doesNotSaveOrPublishWhenRejected() {
        givenScheduledMatch();

        assertThrows(
                ValidationException.class,
                () -> service.record(1L, request(1, 1, 10L)));

        verify(results, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void savesResultAndPublishesEvent() {
        givenScheduledMatch();

        when(results.save(any(MatchResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MatchResultResponse response =
                service.record(1L, request(2, 1, 10L));

        assertEquals(10L, response.winnerTeamId());
        assertEquals(2, response.teamAScore());
        assertEquals("COMPLETED", match.getStatus());

        verify(events)
                .publishEvent(new MatchResultRecordedEvent(1L, 10L));
    }

    private void givenMatchWithoutResult() {
        when(matches.findById(1L))
                .thenReturn(Optional.of(match));

        lenient()
                .when(results.existsByMatchId(1L))
                .thenReturn(false);
    }

    private void givenScheduledMatch() {
        givenMatchWithoutResult();
        match.setStatus(MatchStatus.SCHEDULED.name());
    }

    private static CreateMatchResultRequest request(
            int scoreA,
            int scoreB,
            long winnerId) {

        return new CreateMatchResultRequest(
                scoreA,
                scoreB,
                winnerId);
    }

    private static Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }
}