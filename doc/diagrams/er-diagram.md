# ER Diagram: ฐานข้อมูลระบบจัดการสายแข่ง

โครงสร้างตารางหลังรัน Flyway migration `V1`–`V11` (PostgreSQL 17) ชื่อตารางและคอลัมน์ตรงกับไฟล์ใน `src/main/resources/db/migration/`

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR username UK "NOT NULL"
        VARCHAR password "NOT NULL"
        VARCHAR role "NOT NULL"
        TIMESTAMP created_at
    }

    games {
        BIGINT id PK
        VARCHAR code UK "ROV, FREE_FIRE, VALORANT, FIGHTING_GAME"
        VARCHAR name "NOT NULL"
        INT min_players "> 0"
        VARCHAR logo_url
        BOOLEAN is_active "DEFAULT TRUE"
        TIMESTAMP created_at
    }

    teams {
        BIGINT id PK
        VARCHAR name UK "NOT NULL"
        TEXT description
        BIGINT game_id FK
        VARCHAR logo_url
        TIMESTAMP created_at
    }

    players {
        BIGINT id PK
        VARCHAR name "NOT NULL"
        VARCHAR role "NOT NULL"
        TEXT description
        BIGINT team_id FK "NULL = ยังไม่มีทีม"
        TIMESTAMP created_at
    }

    tournaments {
        BIGINT id PK
        VARCHAR name UK "NOT NULL"
        TEXT description
        DATE start_date "NOT NULL"
        DATE end_date "end_date >= start_date"
        VARCHAR status "UPCOMING, ONGOING, COMPLETED"
        BIGINT game_id FK
        VARCHAR format "SINGLE_ELIMINATION, POINTS"
        VARCHAR logo_url
        SMALLINT total_games "มีค่าเฉพาะ POINTS"
        SMALLINT points_per_kill "DEFAULT 1"
        TIMESTAMP created_at
    }

    tournament_teams {
        BIGINT tournament_id PK, FK
        BIGINT team_id PK, FK
        TIMESTAMP joined_at
        VARCHAR team_name "snapshot ตอนเข้ารายการ"
        TEXT team_description "snapshot"
        VARCHAR team_logo_url "snapshot"
    }

    tournament_team_rosters {
        BIGINT tournament_id PK, FK
        BIGINT team_id PK, FK
        BIGINT player_id PK "ไม่มี FK ไป players"
        VARCHAR player_name "snapshot"
        VARCHAR player_role "snapshot"
    }

    matches {
        BIGINT id PK
        BIGINT tournament_id FK "NOT NULL"
        BIGINT team_a_id FK "NULL = รอผู้ชนะรอบก่อน"
        BIGINT team_b_id FK
        INT round_number ">= 1"
        INT match_number ">= 1"
        BIGINT next_match_id FK "NULL = นัดชิง"
        TIMESTAMP scheduled_at
        VARCHAR status "PENDING, SCHEDULED, COMPLETED"
        TIMESTAMP created_at
    }

    match_results {
        BIGINT id PK
        BIGINT match_id FK, UK "1 แมตช์มีผลได้ 1 ครั้ง"
        INT team_a_score ">= 0"
        INT team_b_score ">= 0"
        BIGINT winner_team_id FK "NOT NULL"
        TIMESTAMP created_at
    }

    tournament_placement_points {
        BIGINT tournament_id PK, FK
        SMALLINT placement PK ">= 1"
        SMALLINT points ">= 0"
    }

    free_fire_games {
        BIGINT id PK
        BIGINT tournament_id FK "NOT NULL"
        SMALLINT game_number ">= 1"
        TIMESTAMP scheduled_at
        VARCHAR status "SCHEDULED, COMPLETED"
        TIMESTAMP created_at
    }

    free_fire_game_results {
        BIGINT id PK
        BIGINT game_id FK "NOT NULL"
        BIGINT tournament_id FK "NOT NULL"
        BIGINT team_id FK "NOT NULL"
        SMALLINT placement ">= 1"
        SMALLINT kills ">= 0"
        TIMESTAMP created_at
    }

    games ||--o{ teams : "ทีมสังกัดเกม"
    games ||--o{ tournaments : "รายการใช้เกม"
    teams |o--o{ players : "มีผู้เล่น"
    tournaments ||--o{ tournament_teams : "มีทีมเข้าร่วม"
    teams ||--o{ tournament_teams : "เข้าร่วมรายการ"
    tournament_teams ||--o{ tournament_team_rosters : "รายชื่อผู้เล่น ณ วันเข้ารายการ"

    tournaments ||--o{ matches : "แพ้คัดออก"
    teams |o--o{ matches : "team_a / team_b"
    matches |o--o{ matches : "next_match"
    matches ||--o| match_results : "ผลแมตช์"
    teams ||--o{ match_results : "winner"

    tournaments ||--o{ tournament_placement_points : "ตารางคะแนนอันดับ"
    tournaments ||--o{ free_fire_games : "เก็บคะแนน"
    free_fire_games ||--o{ free_fire_game_results : "ผลแต่ละทีม"
    tournament_teams ||--o{ free_fire_game_results : "ทีมต้องอยู่ในรายการ"
```

## กฎ ON DELETE ที่สำคัญ

| ความสัมพันธ์ | กฎ | ผล |
| --- | --- | --- |
| `players.team_id` → `teams` | `SET NULL` | ลบทีมแล้วผู้เล่นยังอยู่ แต่ไม่มีทีม |
| `tournament_teams.team_id` → `teams` | `RESTRICT` (V11) | ทีมที่มีประวัติแข่งลบไม่ได้ |
| `tournament_teams.tournament_id` → `tournaments` | `CASCADE` | ลบรายการแล้วลบทีมที่เข้าร่วมและ roster ตาม |
| `matches.tournament_id` → `tournaments` | `CASCADE` | ลบรายการแล้วลบแมตช์ตาม |
| `matches.team_a_id` / `team_b_id` → `teams` | `RESTRICT` | ทีมที่มีแมตช์ลบไม่ได้ |
| `matches.next_match_id` → `matches` | `SET NULL` | |
| `match_results.match_id` → `matches` | `CASCADE` | |
| `match_results.winner_team_id` → `teams` | `RESTRICT` | |
| `free_fire_game_results (game_id, tournament_id)` → `free_fire_games (id, tournament_id)` | `CASCADE` | FK แบบผสมบังคับว่าผลต้องเป็นของเกมในรายการเดียวกัน |
| `free_fire_game_results (tournament_id, team_id)` → `tournament_teams` | `RESTRICT` | บันทึกผลได้เฉพาะทีมที่อยู่ในรายการ |
| `teams.game_id`, `tournaments.game_id` → `games` | `RESTRICT` | เกมที่มีทีมหรือรายการใช้อยู่ลบไม่ได้ |

## Constraint อื่น ๆ

- `matches`: `UNIQUE (tournament_id, round_number, match_number)` และ `team_a_id <> team_b_id`
- `tournaments`: `CHECK ((format = 'POINTS') = (total_games IS NOT NULL))` แบบเก็บคะแนนต้องมี `total_games` ส่วนแบบแพ้คัดออกต้องไม่มี
- `free_fire_games`: `UNIQUE (tournament_id, game_number)`
- `free_fire_game_results`: `UNIQUE (game_id, team_id)` และ `UNIQUE (game_id, placement)` ทีมหนึ่งมีผลได้ครั้งเดียวต่อเกม และอันดับห้ามซ้ำ
- `tournament_team_rosters.player_id` ตั้งใจไม่ผูก FK ไป `players` เพื่อให้รายชื่อเดิมยังอยู่แม้ผู้เล่นถูกแก้ ย้ายทีม หรือถูกลบ

## หมายเหตุ

- ตาราง `users` มีใน schema แล้ว แต่ backend ยังไม่มีโค้ดที่ใช้ (งาน Auth ยังค้างใน `doc/REMAINING-WORK.md`)
- คะแนน Free Fire ไม่ได้เก็บเป็นคอลัมน์ `PointsCalculator` คำนวณจาก `placement`, `kills`, `tournament_placement_points` และ `points_per_kill` ทุกครั้งที่เรียก
