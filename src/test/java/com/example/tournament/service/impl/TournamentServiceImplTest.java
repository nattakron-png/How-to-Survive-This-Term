package com.example.tournament.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tournament.domain.entity.Game;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentPlacementPoint;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.dto.request.TournamentRequest;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.GameRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentPlacementPointRepository;

@ExtendWith(MockitoExtension.class)
class TournamentServiceImplTest {

        @Mock
        TournamentRepository tournaments;

        @Mock
        GameRepository games;

        @Mock
        TournamentPlacementPointRepository tournamentPlacementPoints;

        TournamentServiceImpl service;

        Game freeFire;
        Game rov;

        @BeforeEach
        void setUp() {
                service = new TournamentServiceImpl(
                                tournaments,
                                games,
                                tournamentPlacementPoints);

                freeFire = game(1L, "FREE_FIRE", 4);
                rov = game(2L, "ROV", 5);
        }

        @Test
        void rejectsWhenEndDateIsBeforeStartDate() {
                TournamentRequest request = request(
                                "Test Tournament",
                                2L,
                                TournamentFormat.SINGLE_ELIMINATION,
                                null,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 19));

                when(tournaments.existsByName("Test Tournament")).thenReturn(false);
                when(games.findById(2L)).thenReturn(Optional.of(rov));

                assertThrows(
                                ValidationException.class,
                                () -> service.create(request));
        }

        @Test
        void rejectsDuplicateTournamentName() {
                TournamentRequest request = request(
                                "Duplicate Tournament",
                                2L,
                                TournamentFormat.SINGLE_ELIMINATION,
                                null,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("Duplicate Tournament")).thenReturn(true);

                assertThrows(
                                BusinessException.class,
                                () -> service.create(request));
        }

        @Test
        void rejectsFreeFireWithSingleElimination() {
                TournamentRequest request = request(
                                "Free Fire Tournament",
                                1L,
                                TournamentFormat.SINGLE_ELIMINATION,
                                null,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("Free Fire Tournament")).thenReturn(false);
                when(games.findById(1L)).thenReturn(Optional.of(freeFire));

                assertThrows(
                                ValidationException.class,
                                () -> service.create(request));
        }

        @Test
        void rejectsNonFreeFireWithPointsFormat() {
                TournamentRequest request = request(
                                "ROV Points Tournament",
                                2L,
                                TournamentFormat.POINTS,
                                (short) 10,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("ROV Points Tournament")).thenReturn(false);
                when(games.findById(2L)).thenReturn(Optional.of(rov));

                assertThrows(
                                ValidationException.class,
                                () -> service.create(request));
        }

        @Test
        void rejectsPointsWithoutTotalGames() {
                TournamentRequest request = request(
                                "Free Fire Tournament",
                                1L,
                                TournamentFormat.POINTS,
                                null,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("Free Fire Tournament")).thenReturn(false);
                when(games.findById(1L)).thenReturn(Optional.of(freeFire));

                assertThrows(
                                ValidationException.class,
                                () -> service.create(request));
        }

        @Test
        void rejectsEliminationWithTotalGames() {
                TournamentRequest request = request(
                                "ROV Tournament",
                                2L,
                                TournamentFormat.SINGLE_ELIMINATION,
                                (short) 10,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("ROV Tournament")).thenReturn(false);
                when(games.findById(2L)).thenReturn(Optional.of(rov));

                assertThrows(
                                ValidationException.class,
                                () -> service.create(request));
        }

        @Test
        void createsFreeFirePointsTournamentSuccessfully() {
                TournamentRequest request = request(
                                "Free Fire Tournament",
                                1L,
                                TournamentFormat.POINTS,
                                (short) 10,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("Free Fire Tournament")).thenReturn(false);
                when(games.findById(1L)).thenReturn(Optional.of(freeFire));

                when(tournaments.saveAndFlush(any(Tournament.class)))
                                .thenAnswer(invocation -> {
                                        Tournament tournament = invocation.getArgument(0);
                                        tournament.setId(1L);
                                        return tournament;
                                });

                var response = service.create(request);

                assertEquals(1L, response.id());
                assertEquals("Free Fire Tournament", response.name());
                assertEquals(TournamentFormat.POINTS, response.format());
                assertEquals((short) 10, response.totalGames());
                assertEquals("FREE_FIRE", freeFire.getCode());

                verify(tournaments).saveAndFlush(any(Tournament.class));
        }

        @Test
        void createsEliminationTournamentSuccessfully() {
                TournamentRequest request = request(
                                "ROV Tournament",
                                2L,
                                TournamentFormat.SINGLE_ELIMINATION,
                                null,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("ROV Tournament")).thenReturn(false);
                when(games.findById(2L)).thenReturn(Optional.of(rov));

                when(tournaments.saveAndFlush(any(Tournament.class)))
                                .thenAnswer(invocation -> {
                                        Tournament tournament = invocation.getArgument(0);
                                        tournament.setId(2L);
                                        return tournament;
                                });

                var response = service.create(request);

                assertEquals(2L, response.id());
                assertEquals("ROV Tournament", response.name());
                assertEquals(TournamentFormat.SINGLE_ELIMINATION, response.format());
                assertEquals(null, response.totalGames());

                verify(tournaments).saveAndFlush(any(Tournament.class));
        }

        @Test
        void createsFfwsPlacementPointsAutomatically() {
                TournamentRequest request = request(
                                "Free Fire FFWS Tournament",
                                1L,
                                TournamentFormat.POINTS,
                                (short) 10,
                                LocalDate.of(2026, 10, 20),
                                LocalDate.of(2026, 10, 21));

                when(tournaments.existsByName("Free Fire FFWS Tournament"))
                                .thenReturn(false);

                when(games.findById(1L))
                                .thenReturn(Optional.of(freeFire));

                when(tournaments.saveAndFlush(any(Tournament.class)))
                                .thenAnswer(invocation -> {
                                        Tournament tournament = invocation.getArgument(0);
                                        tournament.setId(1L);
                                        return tournament;
                                });

                var response = service.create(request);

                assertEquals(1L, response.id());
                assertEquals(TournamentFormat.POINTS, response.format());

                verify(tournamentPlacementPoints, times(12))
                                .save(any(TournamentPlacementPoint.class));
        }

        private static TournamentRequest request(
                        String name,
                        Long gameId,
                        TournamentFormat format,
                        Short totalGames,
                        LocalDate startDate,
                        LocalDate endDate) {

                TournamentRequest request = new TournamentRequest();

                request.setName(name);
                request.setGameId(gameId);
                request.setFormat(format);
                request.setTotalGames(totalGames);
                request.setStartDate(startDate);
                request.setEndDate(endDate);

                return request;
        }

        private static Game game(Long id, String code, int minPlayers) {
                Game game = new Game();

                game.setId(id);
                game.setCode(code);
                game.setMinPlayers(minPlayers);
                game.setIsActive(true);

                return game;
        }
}
