
-- เพิ่มคอลัมน์เก็บชื่อทีม ณ วันที่สมัคร และลำดับ Seed
ALTER TABLE tournament_teams
    ADD COLUMN team_name_snapshot VARCHAR(150),
    ADD COLUMN seed INTEGER;

-- เติมชื่อทีมให้รายการสมัครเก่าที่มีอยู่แล้ว
UPDATE tournament_teams tt
SET team_name_snapshot = t.name
FROM teams t
WHERE tt.team_id = t.id
  AND tt.team_name_snapshot IS NULL;

-- บังคับให้รายการสมัครทุกแถวมีชื่อทีม snapshot
ALTER TABLE tournament_teams
    ALTER COLUMN team_name_snapshot SET NOT NULL;