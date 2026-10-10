package com.example.tournament.service.format;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.FreeFireGame;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.domain.enums.TournamentFormat;
import com.example.tournament.exception.ValidationException;
import com.example.tournament.repository.FreeFireGameRepository;

@Component
public class PointsStrategy implements FormatStrategy {

    private final FreeFireGameRepository games;

    public PointsStrategy(FreeFireGameRepository games) {
        this.games = games;
    }

    @Override
    public TournamentFormat format() {
        return TournamentFormat.POINTS;
    }

    // แบบเก็บคะแนนไม่มีสายการแข่ง สร้างเกมย่อย 1..totalGames ให้กรอกผลทีละเกม
    @Override
    public int createSchedule(Tournament tournament, List<Team> teamsInSeedOrder) {
        Short totalGames = tournament.getTotalGames();
        if (totalGames == null || totalGames < 1) {
            throw new ValidationException("Points tournament requires totalGames");
        }

        LocalDateTime now = LocalDateTime.now();
        List<FreeFireGame> created = new ArrayList<>();
        for (short number = 1; number <= totalGames; number++) {
            FreeFireGame game = new FreeFireGame();
            game.setTournament(tournament);
            game.setGameNumber(number);
            game.setStatus("SCHEDULED");
            game.setCreatedAt(now);
            created.add(game);
        }
        games.saveAll(created);
        return created.size();
    }
}
