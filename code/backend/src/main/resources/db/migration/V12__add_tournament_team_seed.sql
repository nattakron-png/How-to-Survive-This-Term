
ALTER TABLE tournament_teams
    ADD COLUMN seed INTEGER CHECK (seed IS NULL OR seed > 0);
