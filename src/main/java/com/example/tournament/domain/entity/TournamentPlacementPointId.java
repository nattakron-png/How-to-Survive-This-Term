package com.example.tournament.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class TournamentPlacementPointId implements Serializable {

    private Long tournament;
    private Short placement;

    public TournamentPlacementPointId() {
    }

    public TournamentPlacementPointId(Long tournament, Short placement) {
        this.tournament = tournament;
        this.placement = placement;
    }

    public Long getTournament() {
        return tournament;
    }

    public void setTournament(Long tournament) {
        this.tournament = tournament;
    }

    public Short getPlacement() {
        return placement;
    }

    public void setPlacement(Short placement) {
        this.placement = placement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TournamentPlacementPointId)) {
            return false;
        }

        TournamentPlacementPointId that = (TournamentPlacementPointId) o;

        return Objects.equals(tournament, that.tournament)
                && Objects.equals(placement, that.placement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tournament, placement);
    }
}