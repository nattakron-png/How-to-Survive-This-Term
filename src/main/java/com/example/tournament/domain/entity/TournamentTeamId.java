package com.example.tournament.domain.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class TournamentTeamId implements Serializable {

    private Long tournamentId;

    private Long teamId;

    public TournamentTeamId() {
    }

    public TournamentTeamId(Long tournamentId, Long teamId) {
        this.tournamentId = tournamentId;
        this.teamId = teamId;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TournamentTeamId that)) {
            return false;
        }

        return Objects.equals(tournamentId, that.tournamentId)
                && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tournamentId, teamId);
    }
}