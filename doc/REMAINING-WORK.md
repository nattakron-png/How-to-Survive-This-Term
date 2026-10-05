# งานที่เหลือของโปรเจค (สถานะ ณ 5 ต.ค. 2026)

เอกสารนี้สรุปจาก `develop` ล่าสุด ว่าอะไรเสร็จแล้ว และแต่ละคนต้องทำอะไรต่อ ก่อนเริ่มงานทุกครั้งให้ `git pull origin develop` และประกาศในกลุ่มว่ากำลังทำชิ้นไหน จะได้ไม่ทำซ้ำกัน

คู่มือละเอียดของบางโมดูลอยู่ในไฟล์แยก ควร commit ไว้ใน `doc/` ทั้งหมด

- `doc/database-module-guide.md`: ฐานข้อมูล, V9–V10, Game API, Auth (คณิศร)
- `doc/bracket-module-guide.md`: สร้างสายการแข่งและ Strategy (คนที่ 4)
- `doc/match-result-api.md`: API บันทึกผลแมตช์ที่เสร็จแล้ว

---

## 1. เสร็จแล้วและอยู่ใน `develop`

| ส่วน | ใครทำ |
| --- | --- |
| Migration V1–V10 และ Entity ทุกตัว (รวม `Game`, `FreeFireGame`, `FreeFireGameResult`, `TournamentPlacementPoint`, `TournamentFormat`) | คณิศร |
| Team API พร้อม Pagination, Validation และ Integration Test | วัชรพล |
| Docker (`Dockerfile`, `compose.yaml` พร้อม PostgreSQL และ test profile), `.env.example`, `doc/setup.md` | วัชรพล |
| `GlobalExceptionHandler` และรูปแบบ error กลาง (`ApiError`) | วัชรพล |
| บันทึกผลแมตช์แพ้คัดออก + ส่งผู้ชนะไปแมตช์ถัดไป (Observer) + Unit Test 13 ข้อ + เอกสาร API | ณัฐกร |
| `ValidationException` (400), `MatchStatus`, `MatchRepository`, `MatchResultRepository` | ณัฐกร |
| CI ด้วย GitHub Actions (build + test ทุก PR เข้า `develop`/`main`) | ณัฐกร |

**อยู่ใน Branch ของณัฐกร รอ PR เข้า `develop`:** ฟิลด์ `totalGames`/`pointsPerKill` ใน `Tournament`, `FreeFireGameRepository`, `FreeFireGameResultRepository`, `TournamentPlacementPointRepository` และ `TournamentTeamRepository` ใครต้องใช้ `TournamentTeamRepository` ให้รอ PR นี้ อย่าสร้างไฟล์ใหม่ซ้ำ

---

## 2. ภาพรวมงานที่เหลือ

| คน | โมดูล | งานหลักที่เหลือ | สถานะ |
| --- | --- | --- | --- |
| 1 คณิศร | ฐานข้อมูล, Entity, Auth | เก็บกวาด repo, Game API, Enum สถานะ, Auth, ER Diagram + Data Dictionary | เริ่มแล้ว |
| 2 วัชรพล | Team + Player | Player CRUD, Swagger, `gameId`/`logoUrl` ใน Team API, อัปโหลดโลโก้, Deploy | เริ่มแล้ว |
| 3 (ยังไม่ระบุ) | Tournament + TournamentTeam | ทั้งโมดูล | **ยังไม่เริ่ม** |
| 4 (ยังไม่ระบุ) | รูปแบบการแข่ง | ทั้งโมดูล (ตาม `bracket-module-guide.md`) | **ยังไม่เริ่ม** |
| 5 ณัฐกร | ผลการแข่ง + CI | ผล Free Fire + ตารางคะแนนรวม | ครึ่งทาง |

**เรื่องด่วนที่สุด:** สิรภัทรกับนฤเศรษฐ์ต้องเลือกโมดูล 3 และ 4 ภายในสัปดาห์นี้ สองโมดูลนี้เป็นแกนกลางที่คนอื่นรออยู่ และนฤเศรษฐ์ยังไม่มี Branch ส่วนตัว ต้องสร้างก่อน

---

## 3. งานของแต่ละคน

### คนที่ 1: คณิศร (ฐานข้อมูล, Entity, Auth)

**เก็บกวาด repo** (ทำก่อนอย่างอื่น และนัดทุกคน push งานค้างก่อนเริ่ม)

- [ ] `git rm --cached .DS_Store` ตอนนี้ยังติดอยู่ที่ root ของ `develop`
- [ ] ย้าย `src/`, `pom.xml`, `mvnw*`, `.mvn/`, `Dockerfile` เข้าโฟลเดอร์ `code/` ตามใบงาน แล้วแก้ path ใน `compose.yaml` และ `.github/workflows/ci.yml` (`working-directory: code` และ path รายงานเทสต์)
- [ ] ลบ `@OneToOne(mappedBy = "match") MatchResult result` ใน `Match.java` (ไม่มีโค้ดไหนใช้แล้ว)

**Game API**

- [ ] `GameRepository`, `GameRequest`/`GameResponse`, `GameMapper`, `GameService` + impl, `GameController`
- [ ] `GET /api/v1/games` (เฉพาะ `is_active = true`), `GET /{id}`, `POST`, `PUT`, `DELETE`
- [ ] กฎ: `code` ซ้ำ → 409, ลบเกมที่มีทีมหรือรายการใช้อยู่ → 409 (ตรวจก่อนลบ อย่าปล่อยให้ FK โยน 500)
- [ ] Unit Test อย่างน้อย 4 กรณี

**Enum สถานะ**

- [ ] สร้าง `TournamentStatus` (`UPCOMING`, `ONGOING`, `COMPLETED`)
- [ ] (แนะนำ) เปลี่ยน `status` ใน `Match` และ `Tournament` จาก `String` เป็น Enum ด้วย `@Enumerated(EnumType.STRING)` **ต้องแจ้งณัฐกรกับคนที่ 4 ก่อน** เพราะโค้ดของสองคนนี้ใช้ `setStatus("...")` และ `MatchStatus.X.name()` อยู่

**Auth** (หลังทีมเลือก Frontend)

- [ ] Spring Security: `GET /api/v1/**` เปิดให้ทุกคน, `POST`/`PUT`/`DELETE` ต้องเป็น `ADMIN`, Swagger และ `/actuator/health` เปิด
- [ ] สร้างบัญชี Admin แรกจาก `ADMIN_USERNAME`/`ADMIN_PASSWORD` ใน Environment Variable ห้ามใส่รหัสผ่านใน Migration
- [ ] แจ้งทีมก่อน merge เพราะเทสต์ที่เรียก `POST` ของทุกคนต้องเพิ่ม `@WithMockUser(roles = "ADMIN")`

**เอกสาร**

- [ ] ER Diagram (`doc/diagrams/`) และ `doc/data-dictionary.md` ตอนนี้ไฟล์ยังว่าง
- [ ] Domain Model

---

### คนที่ 2: วัชรพล (Team + Player)

- [ ] **Player CRUD** `/api/v1/players` (GET list + pagination, GET by id, POST, PUT, DELETE) สร้างผู้เล่นที่ยังไม่มีทีมได้ (`team_id` เป็น NULL) ตามบรีฟข้อ 5
- [ ] **Swagger** เพิ่ม springdoc-openapi เวอร์ชันที่รองรับ Spring Boot ที่ใช้ ให้เปิด `/swagger-ui.html` ได้ (ใบงานบังคับ) และควรทำเป็นอย่างแรก เพราะทุกคนจะได้ใช้ทดสอบ API
- [ ] **Team API รองรับเกมและโลโก้** เพิ่ม `gameId`, `logoUrl` ใน `TeamRequest`/`TeamResponse` ทีมใหม่ต้องมีเกม (เกมไม่มีอยู่ → 404)
- [ ] **อัปโหลดโลโก้** interface `FileStorageService` + implementation (เครื่อง local สำหรับพัฒนา, Supabase Storage หรือ Cloudinary สำหรับ Deploy) ใช้กับทีม, รายการแข่ง และเกม
- [ ] **Unit Test ด้วย Mockito** ของ `TeamServiceImpl` (ตอนนี้มีแต่ Integration Test)
- [ ] **Deploy** ขึ้น Cloud จริงผ่าน URL สาธารณะ (Render, Railway หรืออื่น ๆ) ใช้ PostgreSQL บน Cloud ตามใบงานข้อ 11 ตกลงกับทีมก่อนว่าใครรับ ถ้าวัชรพลถือ Docker อยู่แล้วก็เหมาะที่สุด
- [ ] **เอกสาร:** Use Case Diagram และ `doc/use-case-description.md` (ตอนนี้ไฟล์ยังว่าง)

---

### คนที่ 3: Tournament + TournamentTeam (ยังไม่มีเจ้าของ)

ใช้ `TournamentTeamRepository` จาก PR ของณัฐกร และสร้าง `TournamentRepository`

**Tournament CRUD**

- [ ] `TournamentRequest`/`TournamentResponse` มี `gameId`, `format`, `totalGames`, `pointsPerKill`, `logoUrl`, `startDate`, `endDate`
- [ ] กฎ: `endDate >= startDate` (400), ชื่อซ้ำ (409), เกม Free Fire ต้องเป็น `POINTS` ส่วนเกมอื่นเป็น `SINGLE_ELIMINATION` (400), `POINTS` ต้องมี `totalGames` และแบบแพ้คัดออกต้องไม่มี (400)
- [ ] สร้างรายการ `POINTS` แล้วใส่คะแนนอันดับตั้งต้นแบบ FFWS ใน `tournament_placement_points`: อันดับ 1–12 = 12, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0 (Admin แก้ได้ก่อนรายการเริ่ม)
- [ ] ลบได้เฉพาะรายการที่ยังเป็น `UPCOMING`

**เพิ่ม/ถอนทีมจากรายการ** `POST`/`DELETE /api/v1/tournaments/{tournamentId}/teams/{teamId}`

- [ ] `TeamJoinRule` แยกเป็นคลาสต่อกันแบบ **Chain of Responsibility** (Pattern ของคุณ) ตามลำดับ:
  1. ทีมมีอยู่จริง (404)
  2. รายการมีอยู่จริง (404)
  3. รายการยังเป็น `UPCOMING` และยังไม่สร้างตารางการแข่ง (409)
  4. ทีมยังไม่อยู่ในรายการนี้ (409)
  5. เกมของทีมตรงกับเกมของรายการ (409)
  6. ทีมมีผู้เล่นอย่างน้อย `games.min_players` (409)
  7. รายการแบบ `POINTS` รับไม่เกิน 12 ทีม (409)
  8. **วันแข่งไม่ทับกับรายการอื่นที่ทีมอยู่** `existing.start <= new.end AND existing.end >= new.start` (409) กฎหลักของบรีฟข้อ 9
- [ ] Unit Test ของแต่ละกฎ ต้องมีอย่างน้อย "วันทับกัน → ปฏิเสธ" และ "วันไม่ทับ → ผ่าน" ตามบรีฟข้อ 46
- [ ] **เอกสาร:** Activity Diagram และ Sequence Diagram ของการเพิ่มทีมเข้ารายการ

---

### คนที่ 4: รูปแบบการแข่ง (ยังไม่มีเจ้าของ)

ทำตาม `doc/bracket-module-guide.md` ซึ่งมีโค้ดพร้อมใช้และเทสต์ครบ สรุปงาน:

- [ ] `FormatStrategy` + `SingleEliminationStrategy` (สร้างสาย, จัดบาย, ผูก `next_match_id`) **Strategy Pattern**
- [ ] `ScheduleService`: เลือก Strategy ตาม `tournament.getFormat()` (V9 มีแล้ว ใช้ได้เลย)
- [ ] `POST /api/v1/tournaments/{id}/schedule`, `GET /api/v1/tournaments/{id}/matches`, `GET /api/v1/matches`, `GET /api/v1/matches/{id}` (ผ่าน `MatchService` ห้าม Controller เรียก Repository ตรง)
- [ ] `PointsStrategy`: สร้าง `free_fire_games` จำนวน `totalGames` เกม (ตาราง V10 มีแล้ว ทำได้เลย)
- [ ] เพิ่มเมธอดใน `MatchRepository` ที่มีอยู่ ห้ามสร้างไฟล์ใหม่
- [ ] **ต้องตรงกับโมดูลของณัฐกร:** `match_number` นับใหม่ทุกรอบ, ผู้ชนะจากเลขคี่ไปช่อง A เลขคู่ไปช่อง B, สถานะแมตช์ `PENDING`/`SCHEDULED`/`COMPLETED`
- [ ] **เอกสาร:** Class Diagram (ระบุตำแหน่ง Pattern), State Diagram ของ Match และ Tournament, Sequence Diagram การสร้างตารางการแข่ง

---

### คนที่ 5: ณัฐกร (ผลการแข่ง + CI)

- [ ] เปิด PR ของ Repository และฟิลด์ใน `Tournament` เข้า `develop` (คนที่ 3 และ 4 รออยู่)
- [ ] DTO สำหรับกรอกผลเกมและตารางคะแนน
- [ ] `PointsCalculator`: คะแนน = คะแนนอันดับ + kill × `pointsPerKill` และเรียงอันดับ (คะแนนรวม → จำนวน Booyah → kill รวม → อันดับในเกมล่าสุด) พร้อมเทสต์
- [ ] `FreeFireResultService`: กรอกครบทุกทีมในรายการ, อันดับไม่ซ้ำและอยู่ในช่วง 1..จำนวนทีม, kill ไม่ติดลบ, เกมเดิมกรอกซ้ำไม่ได้ พร้อมเทสต์
- [ ] Event + Listener: ครบทุกเกมแล้วเปลี่ยนรายการเป็น `COMPLETED` (Observer)
- [ ] `POST`/`GET /api/v1/free-fire-games/{gameId}/results` และ `GET /api/v1/tournaments/{id}/standings`
- [ ] **เอกสาร:** Component และ Deployment Diagram, Sequence Diagram การบันทึกผล, Single Elimination Bracket Flow, README

---

## 4. งานร่วมของทั้งทีม

**ต้องตัดสินใจในสัปดาห์นี้**

- [ ] **Frontend ใช้ Thymeleaf หรือ React** ทุกคนต้องทำหน้าเว็บของโมดูลตัวเอง (ตาม Figma) และ Auth ของคณิศรรอเรื่องนี้อยู่
- [ ] **รูปแบบชื่อ Branch** ถามอาจารย์ว่าการใส่นามสกุลแบบ `Kanisorn-maprajuk_6733800313_02` ผ่านไหม ผิดรูปแบบโดนหักคนละ 5 คะแนน

**ตั้งค่า repo** (เจ้าของ repo ทำ)

- [ ] Branch Protection ของ `develop` และ `main`: ต้องผ่าน PR, Approve 1 คน, CI (`build-and-test`) ต้องผ่าน และห้ามข้ามกฎแม้เป็นแอดมิน

**เอกสารที่ทุกคนเขียนส่วนของตัวเอง**

- [ ] `doc/design-patterns.md` (ตอนนี้ว่าง): Strategy (คนที่ 4), Chain of Responsibility (คนที่ 3), Observer (ณัฐกร) และ Enterprise Patterns ตามใบงานข้อ 5.1 แต่ละแถวต้องมี ปัญหาที่แก้ | ไฟล์/คลาส | Class Diagram
- [ ] `doc/solid-analysis.md` (ตอนนี้ว่าง): แต่ละหลักระบุไฟล์ บรรทัด และเหตุผล
- [ ] README: เพิ่มคอลัมน์ Branch และกรอกหน้าที่ให้ครบทุกคน เพิ่มหัวข้อตามใบงานข้อ 10 (Tech Stack, Architecture, ER Diagram, วิธีรัน, API Docs, วิธีรันเทสต์, Deployment URL, Project Structure)
- [ ] สไลด์นำเสนอใน `doc/slide/`

**กติกาที่ทุกคนต้องทำตลอด**

- Commit ด้วยบัญชี GitHub ของตัวเอง อย่างน้อย 15 commit **กระจายหลายวัน** (ตอนนี้หลายคน commit กระจุกในคืนเดียว)
- ทุก PR ต้องมีเพื่อน Approve ก่อน merge อย่า merge PR ของตัวเอง
- ดึง `develop` เข้า Branch ของตัวเองเอง อย่าให้คนอื่นกด merge เข้า Branch ของเรา
- ทุกคนต้องอธิบายโค้ดในโมดูลของตัวเองได้

---

## 5. ลำดับที่ควร merge เข้า `develop`

1. คณิศร: เก็บกวาด repo (ย้ายเข้า `code/`) **ต้องมาก่อน** เพราะกระทบ path ของทุกไฟล์
2. ณัฐกร: Repository + ฟิลด์ `Tournament` / วัชรพล: Swagger
3. คณิศร: Game API + `TournamentStatus` / วัชรพล: Player CRUD + `gameId` ใน Team
4. คนที่ 3: `TournamentRepository` + Tournament CRUD + เพิ่มทีมเข้ารายการ
5. คนที่ 4: สร้างตารางการแข่งทั้งสองแบบ / ณัฐกร: ผล Free Fire + ตารางคะแนน
6. คณิศร: Auth (หลังเลือก Frontend)
7. ทุกคน: Frontend, เอกสาร, Deploy แล้ว merge `develop` เข้า `main` ผ่าน PR ก่อนส่ง

---

## 6. ข้อตกลงที่ต้องใช้ตรงกันทั้งทีม

| เรื่อง | ค่าที่ใช้ |
| --- | --- |
| สถานะรายการ | `UPCOMING` → `ONGOING` → `COMPLETED` |
| สถานะแมตช์แพ้คัดออก | `PENDING` (รอคู่แข่ง) → `SCHEDULED` (รู้ทีมครบ) → `COMPLETED` (มีผล) |
| สถานะเกม Free Fire | `SCHEDULED` → `COMPLETED` |
| รูปแบบการแข่ง | `SINGLE_ELIMINATION` (ROV, Valorant, Fighting Game) / `POINTS` (Free Fire) |
| ช่องของผู้ชนะ | `match_number` เลขคี่ → ช่อง A, เลขคู่ → ช่อง B ของแมตช์ถัดไป |
| Exception | ไม่พบข้อมูล → `ResourceNotFoundException` (404), ขัดกับสถานะข้อมูล → `BusinessException` (409), ข้อมูลที่ส่งมาผิด → `ValidationException` (400) |
| Migration | V1–V10 ห้ามแก้ ไฟล์ใหม่เริ่ม V11 และขอเลขจากคณิศรก่อน |
| ไฟล์ที่ใช้ร่วมกัน | `MatchRepository` และ `TournamentTeamRepository` (ณัฐกร), `GlobalExceptionHandler` (วัชรพล), Enum และ Entity (คณิศร) แก้ได้ แต่บอกเจ้าของก่อน |
