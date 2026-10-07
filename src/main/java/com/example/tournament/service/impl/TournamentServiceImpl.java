package com.example.tournament.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Game;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.dto.request.TournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.GameRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.TournamentService;

@Service
@Transactional
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final GameRepository gameRepository;

    public TournamentServiceImpl(
            TournamentRepository tournamentRepository,
            GameRepository gameRepository) {
        this.tournamentRepository = tournamentRepository;
        this.gameRepository = gameRepository;
    }

    @Override
    public TournamentResponse create(TournamentRequest request) {
        String name = request.getName().trim();

        if (tournamentRepository.existsByName(name)) {
            throw new BusinessException("Tournament name already exists");
        }

        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        validateTournamentRequest(request, game);

        Tournament tournament = new Tournament();

        tournament.setName(name);
        tournament.setDescription(request.getDescription());
        tournament.setGame(game);
        tournament.setFormat(request.getFormat());
        tournament.setTotalGames(request.getTotalGames());
        tournament.setPointsPerKill(request.getPointsPerKill());
        tournament.setLogoUrl(request.getLogoUrl());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setStatus(TournamentStatus.UPCOMING);
        tournament.setCreatedAt(LocalDateTime.now());

        return toResponse(tournamentRepository.saveAndFlush(tournament));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TournamentResponse> getAll() {
        return tournamentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentResponse getById(Long id) {
        return toResponse(findTournament(id));
    }

    @Override
    public TournamentResponse update(Long id, TournamentRequest request) {
        Tournament tournament = findTournament(id);

        String name = request.getName().trim();

        if (!name.equalsIgnoreCase(tournament.getName())
                && tournamentRepository.existsByName(name)) {
            throw new BusinessException("Tournament name already exists");
        }

        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        validateTournamentRequest(request, game);

        tournament.setName(name);
        tournament.setDescription(request.getDescription());
        tournament.setGame(game);
        tournament.setFormat(request.getFormat());
        tournament.setTotalGames(request.getTotalGames());
        tournament.setPointsPerKill(request.getPointsPerKill());
        tournament.setLogoUrl(request.getLogoUrl());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());

        return toResponse(tournamentRepository.saveAndFlush(tournament));
    }

    @Override
    public void delete(Long id) {
        Tournament tournament = findTournament(id);

        if (tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new BusinessException(
                    "Tournament can only be deleted when it is UPCOMING");
        }

        tournamentRepository.delete(tournament);
        tournamentRepository.flush();
    }

    private void validateTournamentRequest(
            TournamentRequest request,
            Game game) {

        if (request.getStartDate() == null
                || request.getEndDate() == null) {
            throw new ValidationException(
                    "Start date and end date are required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException(
                    "End date must be on or after start date");
        }

        if (request.getFormat() == null) {
            throw new ValidationException(
                    "Tournament format is required");
        }

        boolean isFreeFire =
                "FREE_FIRE".equalsIgnoreCase(game.getCode());

        if (isFreeFire
                && request.getFormat() != TournamentFormat.POINTS) {
            throw new ValidationException(
                    "Free Fire tournaments must use POINTS format");
        }

        if (!isFreeFire
                && request.getFormat() == TournamentFormat.POINTS) {
            throw new ValidationException(
                    "Only Free Fire tournaments can use POINTS format");
        }

        if (request.getFormat() == TournamentFormat.POINTS
                && request.getTotalGames() == null) {
            throw new ValidationException(
                    "POINTS tournaments require totalGames");
        }

        if (request.getFormat() == TournamentFormat.SINGLE_ELIMINATION
                && request.getTotalGames() != null) {
            throw new ValidationException(
                    "SINGLE_ELIMINATION tournaments must not have totalGames");
        }
    }

    private Tournament findTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Tournament not found: " + id));
    }

    private TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getGame().getId(),
                tournament.getFormat(),
                tournament.getTotalGames(),
                tournament.getPointsPerKill(),
                tournament.getLogoUrl(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getStatus());
    }
}
