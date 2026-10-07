ALTER TABLE players
ADD COLUMN team_id BIGINT;

ALTER TABLE players
ADD CONSTRAINT fk_players_team
FOREIGN KEY (team_id)
REFERENCES teams(id)
ON DELETE SET NULL;

CREATE INDEX idx_players_team_id
ON players(team_id);