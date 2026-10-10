package com.example.tournament.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_placement_points")
@IdClass(TournamentPlacementPointId.class)
public class TournamentPlacementPoint {

    @Id
    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Id
    @Column(nullable = false)
    private Short placement;

    @Column(nullable = false)
    private Short points;

    public TournamentPlacementPoint() {
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Short getPlacement() {
        return placement;
    }

    public void setPlacement(Short placement) {
        this.placement = placement;
    }

    public Short getPoints() {
        return points;
    }

    public void setPoints(Short points) {
        this.points = points;
    }
}