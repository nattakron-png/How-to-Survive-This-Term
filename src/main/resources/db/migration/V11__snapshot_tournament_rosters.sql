ALTER TABLE tournament_teams
    ADD COLUMN team_name VARCHAR(150),
    ADD COLUMN team_description TEXT,
    ADD COLUMN team_logo_url VARCHAR(500);

-- Existing registrations can only be backfilled from today's data. Earlier edits cannot be recovered.
UPDATE tournament_teams tt
SET team_name = t.name,
    team_description = t.description,
    team_logo_url = t.logo_url
FROM teams t
WHERE t.id = tt.team_id;

ALTER TABLE tournament_teams
    ALTER COLUMN team_name SET NOT NULL;

CREATE TABLE tournament_team_rosters (
    tournament_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    player_name VARCHAR(150) NOT NULL,
    player_role VARCHAR(100) NOT NULL,
    PRIMARY KEY (tournament_id, team_id, player_id),
    CONSTRAINT fk_tournament_team_rosters_registration
        FOREIGN KEY (tournament_id, team_id)
        REFERENCES tournament_teams (tournament_id, team_id)
        ON DELETE CASCADE
);

-- Keep the copied identity and text even when the live player is edited, moved or deleted.
INSERT INTO tournament_team_rosters
    (tournament_id, team_id, player_id, player_name, player_role)
SELECT tt.tournament_id, tt.team_id, p.id, p.name, p.role
FROM tournament_teams tt
JOIN players p ON p.team_id = tt.team_id;

-- A team with tournament history must not disappear through the old CASCADE rule.
ALTER TABLE tournament_teams DROP CONSTRAINT fk_tournament_teams_team;
ALTER TABLE tournament_teams
    ADD CONSTRAINT fk_tournament_teams_team
    FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT;
