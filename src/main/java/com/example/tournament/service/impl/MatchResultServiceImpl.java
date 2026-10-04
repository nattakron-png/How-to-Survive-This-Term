package com.example.tournament.service.impl;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.tournament.service.MatchResultService;

@Service
@Transactional
public class MatchResultServiceImpl implements MatchResultService {

    private final MatchRepository matches;
    private final MatchResultRepository results;
    private final MatchResultMapper mapper;
    private final ApplicationEventPublisher events;

    public MatchResultServiceImpl(MatchRepository matches, MatchResultRepository results,
            MatchResultMapper mapper, ApplicationEventPublisher events) {
        this.matches = matches;
        this.results = results;
        this.mapper = mapper;
        this.events = events;
    }

    @Override
    public MatchResultResponse record(Long matchId, CreateMatchResultRequest request) {
        // 1. แมตช์ต้องมีอยู่จริง → 404
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));

        // 2. แมตช์ต้องยังไม่มีผล → 409
        if (results.existsByMatchId(matchId)) {
            throw new BusinessException("Match already has a result");
        }

        // 3. ต้องมีทีมครบทั้งสองฝั่ง → 409
        Team teamA = match.getTeamA();
        Team teamB = match.getTeamB();
        if (teamA == null || teamB == null) {
            throw new BusinessException("Both teams must be set before recording a result");
        }

        // 4. คะแนนไม่ติดลบ ตรวจแล้วใน DTO ด้วย @Min(0) → 400
        int scoreA = request.teamAScore();
        int scoreB = request.teamBScore();

        // 5. แพ้คัดออกห้ามเสมอ → 400
        if (scoreA == scoreB) {
            throw new ValidationException("Single elimination match cannot end in a draw");
        }

        // 6. ผู้ชนะต้องเป็นทีมในแมตช์ → 400
        Long winnerId = request.winnerTeamId();
        if (!winnerId.equals(teamA.getId()) && !winnerId.equals(teamB.getId())) {
            throw new ValidationException("Winner must be one of the teams in this match");
        }

        // 7. ผู้ชนะต้องมีคะแนนมากกว่า → 400
        Team winner = scoreA > scoreB ? teamA : teamB;
        if (!winner.getId().equals(winnerId)) {
            throw new ValidationException("Winner must have the higher score");
        }

        MatchResult result = new MatchResult();
        result.setMatch(match);
        result.setTeamAScore(scoreA);
        result.setTeamBScore(scoreB);
        result.setWinnerTeam(winner);
        result.setCreatedAt(LocalDateTime.now());
        MatchResult saved = results.save(result);

        match.setStatus(MatchStatus.COMPLETED.name());

        // Observer: ประกาศว่ามีผลใหม่ ให้ Listener ส่งผู้ชนะไปแมตช์ถัดไป
        events.publishEvent(new MatchResultRecordedEvent(match.getId(), winner.getId()));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResultResponse get(Long matchId) {
        return results.findByMatchId(matchId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for match: " + matchId));
    }
}
