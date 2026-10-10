package com.example.tournament.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.tournament.domain.entity.Match;
import com.example.tournament.domain.entity.Team;
import com.example.tournament.domain.enums.MatchStatus;
import com.example.tournament.domain.enums.TournamentStatus;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.MatchRepository;

@Component
public class BracketProgressionListener {

    private final MatchRepository matches;

    public BracketProgressionListener(MatchRepository matches) {
        this.matches = matches;
    }

    // ทำงานใน Transaction เดียวกับการบันทึกผล ถ้าส่งต่อไม่สำเร็จ ผลจะ rollback ด้วย
    @EventListener
    public void onResultRecorded(MatchResultRecordedEvent event) {
        Match match = matches.findById(event.matchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + event.matchId()));

        Match next = match.getNextMatch();

        if (next == null) {
            // ไม่มีแมตช์ถัดไป = นัดชิงจบแล้ว รายการจบ
            match.getTournament().setStatus(TournamentStatus.COMPLETED);
            return;
        }

        Team winner = match.getTeamA().getId().equals(event.winnerTeamId())
                ? match.getTeamA()
                : match.getTeamB();

        // match_number เลขคี่ไปช่อง A เลขคู่ไปช่อง B
        boolean goesToSlotA = match.getMatchNumber() % 2 == 1;
        Team occupant = goesToSlotA ? next.getTeamA() : next.getTeamB();

        if (occupant != null && !occupant.getId().equals(winner.getId())) {
            throw new BusinessException("Next match slot is already taken");
        }

        if (goesToSlotA) {
            next.setTeamA(winner);
        } else {
            next.setTeamB(winner);
        }

        // รู้ทีมครบสองฝั่งแล้ว แมตช์ถัดไปพร้อมแข่ง
        if (next.getTeamA() != null && next.getTeamB() != null) {
            next.setStatus(MatchStatus.SCHEDULED.name());
        }
    }
}