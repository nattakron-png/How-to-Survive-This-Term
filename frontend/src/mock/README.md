# Mock data

ข้อมูลตัวอย่างที่จัดรูปแบบให้ตรงกับ **Entity / ตารางในฐานข้อมูลจริง** (ดู `src/main/resources/db/migration`)
ชื่อ field เป็น camelCase แบบเดียวกับ Entity และ DTO ฝั่ง Spring Boot เช่น `start_date` → `startDate`

| ไฟล์ | ตรงกับตาราง / Entity |
|---|---|
| `games.js` | `games` → `Game` |
| `teams.js` | `teams` → `Team` |
| `players.js` | `players` → `Player` |
| `tournaments.js` | `tournaments` → `Tournament`, `tournament_teams` → `TournamentTeam` |
| `matches.js` | `matches` → `Match`, `match_results` → `MatchResult`, `free_fire_games` → `FreeFireGame` |
| `freeFireStandings.js` | รูปแบบเดียวกับ `FreeFireStandingsResponse` |

- ความสัมพันธ์ใช้ foreign key เป็น id (`gameId`, `tournamentId`, `teamAId` ...) เหมือนในตาราง
- `queries.js` ทำหน้าที่เหมือน backend ชั่วคราว: join ข้อมูลแล้วส่งให้หน้าเว็บ
  **ตอนเชื่อม API จริง ให้แก้แค่ไฟล์นี้** (เปลี่ยนเป็น `fetch('/api/v1/...')`) หน้าเว็บไม่ต้องแก้
- การแปลงเป็นข้อความภาษาไทย (วันที่, รูปแบบการแข่ง, ชื่อรอบ, ตัวย่อทีม) อยู่ที่ `src/utils/format.js`

## สายการแข่ง (แพ้คัดออก)
- แมตช์เชื่อมกันด้วย `nextMatchId` ผู้ชนะของแมตช์ที่มี `matchNumber` น้อยกว่าไปอยู่ช่อง `teamA` ของแมตช์ถัดไป
- BYE คือแมตช์รอบแรกที่ `status = 'COMPLETED'` และ `teamAId` หรือ `teamBId` เป็น `null` (ไม่มีแถวใน `match_results`)
- ตัวอย่างใน mock: KKU ROV Cup (กำลังแข่ง), CP Valorant Showdown (6 ทีม มี BYE), Summer Valorant (จบแล้ว มีแชมป์)

## ค่าที่ทีมต้องตกลงกัน
- `tournaments.status` ในฐานข้อมูลเป็น VARCHAR อิสระ (ยังไม่มี enum) — mock ใช้ `UPCOMING` | `ONGOING` | `FINISHED`
- ยังไม่มีคอลัมน์ "แชมป์" — mock คำนวณจากผู้ชนะนัดชิง (`match_results.winner_team_id` ของรอบสุดท้าย)
  หรืออันดับ 1 ของ Free Fire standings
