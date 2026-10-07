package com.example.tournament.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Tournament;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.FreeFireGameRepository;
import com.example.tournament.repository.TournamentRepository;

@Component
public class FreeFireCompletionListener {

    private final TournamentRepository tournaments;
    private final FreeFireGameRepository games;

    public FreeFireCompletionListener(TournamentRepository tournaments, FreeFireGameRepository games) {
        this.tournaments = tournaments;
        this.games = games;
    }

    // ทำงานใน Transaction เดียวกับการบันทึกผลเกม
    @EventListener
    public void onGameRecorded(FreeFireGameRecordedEvent event) {
        Tournament tournament = tournaments.findById(event.tournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + event.tournamentId()));

        Short totalGames = tournament.getTotalGames();
        if (totalGames == null) {
            return; // ไม่ใช่แบบเก็บคะแนน หรือยังไม่ได้กำหนดจำนวนเกม
        }

        long completed = games.countByTournamentIdAndStatus(tournament.getId(), "COMPLETED");
        if (completed >= totalGames) {
            tournament.setStatus("COMPLETED");
        }
    }
}