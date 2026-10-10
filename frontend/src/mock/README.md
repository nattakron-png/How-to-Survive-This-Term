# Mock data

ข้อมูลตัวอย่างที่จัดรูปแบบให้ตรงกับ **Entity / ตารางในฐานข้อมูลจริง** (ดู `src/main/resources/db/migration`)
ชื่อ field เป็น camelCase แบบเดียวกับ Entity และ DTO ฝั่ง Spring Boot เช่น `start_date` → `startDate`

| ไฟล์ | ตรงกับตาราง / Entity |
|---|---|
| `games.js` | `games` → `Game` |
| `users.js` | `users` → `User` (ใช้กับหน้า login ผู้ดูแล) |
| `teams.js` | `teams` → `Team` |
| `players.js` | `players` → `Player` |
| `tournaments.js` | `tournaments` → `Tournament`, `tournament_teams` → `TournamentTeam` |
| `matches.js` | `matches` → `Match`, `match_results` → `MatchResult`, `free_fire_games` → `FreeFireGame` |
| `freeFire.js` | `free_fire_game_results` → `FreeFireGameResult`, `tournament_placement_points` → `TournamentPlacementPoint` |

- ความสัมพันธ์ใช้ foreign key เป็น id (`gameId`, `tournamentId`, `teamAId` ...) เหมือนในตาราง
- ตารางคะแนน Free Fire คำนวณใน `queries.js` (`getFreeFireStandings`) จากผลแต่ละเกม แล้วส่งออกในรูปแบบเดียวกับ `FreeFireStandingsResponse`
- `queries.js` ทำหน้าที่เหมือน backend ชั่วคราว: join ข้อมูลแล้วส่งให้หน้าเว็บ
  **ตอนเชื่อม API จริง ให้แก้แค่ไฟล์นี้** (เปลี่ยนเป็น `fetch('/api/v1/...')`) หน้าเว็บไม่ต้องแก้
- การแปลงเป็นข้อความภาษาไทย (วันที่, รูปแบบการแข่ง, ชื่อรอบ, ตัวย่อทีม) อยู่ที่ `src/utils/format.js`

## สายการแข่ง (แพ้คัดออก)
- แมตช์เชื่อมกันด้วย `nextMatchId` ผู้ชนะของแมตช์ที่มี `matchNumber` น้อยกว่าไปอยู่ช่อง `teamA` ของแมตช์ถัดไป
- BYE คือแมตช์รอบแรกที่ `status = 'COMPLETED'` และ `teamAId` หรือ `teamBId` เป็น `null` (ไม่มีแถวใน `match_results`)
- ตัวอย่างใน mock: KKU ROV Cup (กำลังแข่ง), CP Valorant Showdown (6 ทีม มี BYE), Summer Valorant (จบแล้ว มีแชมป์)

## หน้าผู้ดูแล (admin.js)
- `admin.js` จำลอง endpoint ฝั่งผู้ดูแล ทุกฟังก์ชันที่แก้ข้อมูลเป็น `async` เหมือนเรียก API จริง เช่น
  `createTournament` → `POST /api/v1/tournaments`, `updateTournament` → `PUT /api/v1/tournaments/{id}`,
  `addTeamToTournament` → `POST /api/v1/tournaments/{id}/teams`, `generateBracket` → `POST /api/v1/tournaments/{id}/bracket`,
  `generateFreeFireSchedule` → `POST /api/v1/tournaments/{id}/free-fire-games`
- ข้อมูลที่แก้จะอยู่ในหน่วยความจำจนกว่าจะรีเฟรชหน้า (เป็นแค่ mock)
- กฎที่ backend ต้องตรวจซ้ำ: ทีมต้องเป็นเกมเดียวกับรายการ, วันแข่งห้ามทับกับรายการอื่นของทีม, Free Fire ไม่เกิน 12 ทีม,
  สร้างสาย/ตารางเกมแล้วห้ามเพิ่มหรือลบทีม, ลบรายการได้เฉพาะสถานะ `UPCOMING`, ล้างสายได้เมื่อยังไม่มีผลการแข่ง
- สร้างสาย: ขนาดสาย = 2 ยกกำลังที่พอดีกับจำนวนทีม จับคู่ seed แบบมาตรฐาน (1 พบอันดับสุดท้าย) ทีม seed ต้นๆ ได้ BYE
  ลำดับ seed ส่งมาตอนสร้างสายเท่านั้น (ตาราง `tournament_teams` ไม่มีคอลัมน์ seed)
- ทีม: `saveTeam` → `POST /api/v1/teams` หรือ `PUT /api/v1/teams/{id}` ส่งรายชื่อผู้เล่นทั้งหมดของทีมไปพร้อมกัน
  ผู้เล่นที่ถูก "นำออก" จะถูกตั้ง `teamId = null` (ไม่ได้ลบ) ตรงกับ `players.team_id` ที่เป็น nullable
- ผู้เล่นที่ยังไม่มีทีมใน mock คือแถวที่ `teamId: null` ใน `players.js`
- เปลี่ยนเกมของทีมไม่ได้ถ้าเคยลงแข่ง และลบทีมได้เฉพาะทีมที่ยังไม่เคยลงแข่ง

## Login ผู้ดูแล
- `auth.js` จำลองการ login ตอนเชื่อมจริงให้เปลี่ยนเป็น `POST /api/v1/auth/login`
- บัญชีทดสอบใน mock: `admin` / `admin1234` (ในฐานข้อมูลจริงรหัสผ่านต้องเก็บแบบ hash เช่น BCrypt ห้ามเก็บเป็นข้อความตรงๆ)
- หน้า `/admin` กันไว้ด้วย route guard ใน `src/router/index.js` แต่การกันจริงต้องทำที่ backend ด้วย (ตรวจ role ทุก endpoint ที่แก้ไขข้อมูล)

## ค่าที่ทีมต้องตกลงกัน
- `tournaments.status` ในฐานข้อมูลเป็น VARCHAR อิสระ (ยังไม่มี enum) — mock ใช้ `UPCOMING` | `ONGOING` | `FINISHED`
- ยังไม่มีคอลัมน์ "แชมป์" — mock คำนวณจากผู้ชนะนัดชิง (`match_results.winner_team_id` ของรอบสุดท้าย)
  หรืออันดับ 1 ของตารางคะแนน Free Fire
