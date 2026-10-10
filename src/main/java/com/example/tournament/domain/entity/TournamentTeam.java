package com.example.tournament.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_teams")
public class TournamentTeam {

    @EmbeddedId
    private TournamentTeamId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tournamentId")
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // เก็บชื่อทีม ณ เวลาที่สมัคร เพื่อไม่ให้ผลย้อนหลังเปลี่ยน
    @Column(name = "team_name_snapshot", nullable = false, length = 150)
    private String teamNameSnapshot;

    // ลำดับ Seed ของทีมในทัวร์นาเมนต์
    @Column(name = "seed")
    private Integer seed;

    public TournamentTeam() {
    }

    public TournamentTeamId getId() { return id; }
    public void setId(TournamentTeamId id) { this.id = id; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public String getTeamNameSnapshot() { return teamNameSnapshot; }
    public void setTeamNameSnapshot(String teamNameSnapshot) {
        this.teamNameSnapshot = teamNameSnapshot;
    }

    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
}