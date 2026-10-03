CREATE TABLE tournament_teams (
    tournament_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_tournament_teams
        PRIMARY KEY (tournament_id, team_id),

    CONSTRAINT fk_tournament_teams_tournament
        FOREIGN KEY (tournament_id)
        REFERENCES tournaments(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tournament_teams_team
        FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_tournament_teams_team_id
ON tournament_teams(team_id);