package com.example.tournament.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.entity.TournamentTeam;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.ScheduleResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentTeamRepository;
import com.example.tournament.service.ScheduleService;
import com.example.tournament.service.format.FormatStrategy;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final TournamentRepository tournaments;
    private final TournamentTeamRepository tournamentTeams;
    private final MatchRepository matches;
    private final MatchMapper mapper;
    private final Map<TournamentFormat, FormatStrategy> strategies;

    // Spring ส่ง FormatStrategy ทุกตัวที่เป็น @Component เข้ามาเอง
    public ScheduleServiceImpl(TournamentRepository tournaments, TournamentTeamRepository tournamentTeams,
            MatchRepository matches, MatchMapper mapper, List<FormatStrategy> strategyList) {
        this.tournaments = tournaments;
        this.tournamentTeams = tournamentTeams;
        this.matches = matches;
        this.mapper = mapper;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(FormatStrategy::format, Function.identity()));
    }

    @Override
    public ScheduleResponse createSchedule(Long tournamentId) {
        // 1. รายการต้องมีอยู่จริง → 404
        Tournament tournament = findTournament(tournamentId);

        // 2. ต้องยังไม่เริ่ม → 409
        if (!"UPCOMING".equals(tournament.getStatus())) {
            throw new BusinessException("Schedule can only be created before the tournament starts");
        }

        // 3. ต้องยังไม่เคยสร้างตาราง → 409
        if (matches.existsByTournamentId(tournamentId)) {
            throw new BusinessException("Schedule already exists for this tournament");
        }

        // 4. ต้องมีอย่างน้อย 2 ทีม เรียงตามลำดับที่เข้าร่วม (ทีมแรกได้ seed ดีกว่า) → 409
        List<Team> teams = tournamentTeams.findByTournamentIdOrderByJoinedAtAsc(tournamentId).stream()
                .map(TournamentTeam::getTeam)
                .toList();
        if (teams.size() < 2) {
            throw new BusinessException("At least 2 teams are required");
        }

        // 5. เลือก Strategy ตามรูปแบบของรายการ → 409 ถ้ายังไม่รองรับ
        TournamentFormat format = tournament.getFormat();
        FormatStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new BusinessException("Unsupported tournament format: " + format);
        }

        int created = strategy.createSchedule(tournament, teams);
        tournament.setStatus("ONGOING");
        return new ScheduleResponse(tournamentId, format.name(), created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchResponse> listMatches(Long tournamentId) {
        findTournament(tournamentId);
        return matches.findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(tournamentId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private Tournament findTournament(Long id) {
        return tournaments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
    }
}
