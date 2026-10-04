package com.example.tournament.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "free_fire_game_results",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_ffr_team",
            columnNames = {"game_id", "team_id"}
        ),
        @UniqueConstraint(
            name = "uq_ffr_placement",
            columnNames = {"game_id", "placement"}
        )
    }
)
public class FreeFireGameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private FreeFireGame game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Short placement;

    @Column(nullable = false)
    private Short kills;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FreeFireGameResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FreeFireGame getGame() {
        return game;
    }

    public void setGame(FreeFireGame game) {
        this.game = game;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Short getPlacement() {
        return placement;
    }

    public void setPlacement(Short placement) {
        this.placement = placement;
    }

    public Short getKills() {
        return kills;
    }

    public void setKills(Short kills) {
        this.kills = kills;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}