package com.example.tournament.dto.request;

import java.time.LocalDate;

import com.example.tournament.domain.enums.TournamentFormat;

public class TournamentRequest {

    private Long gameId;
    private TournamentFormat format;
    private Short totalGames;
    private Short pointsPerKill;
    private String logoUrl;
    private LocalDate startDate;
    private LocalDate endDate;

    private String name;
    private String description;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public void setFormat(TournamentFormat format) {
        this.format = format;
    }

    public Short getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(Short totalGames) {
        this.totalGames = totalGames;
    }

    public Short getPointsPerKill() {
        return pointsPerKill;
    }

    public void setPointsPerKill(Short pointsPerKill) {
        this.pointsPerKill = pointsPerKill;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
