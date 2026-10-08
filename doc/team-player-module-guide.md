# คู่มือโมดูลทีมและผู้เล่น (คนที่ 2: วัชรพล)

โมดูลนี้ดูแลข้อมูลทีม ผู้เล่น และการสังกัดทีม รวมถึงสภาพแวดล้อม Docker, Swagger UI และการทดสอบ API ที่เกี่ยวข้อง คู่มือนี้เป็นจุดอ่านร่วมของทีมสำหรับส่วนคนที่ 2 โดยแยก **สิ่งที่ทำแล้ว** ออกจาก **งานที่ยังต้องพัฒนา**; อย่านำตัวอย่างของงานที่ยังไม่ทำไปอ้างว่าเป็น API ปัจจุบัน

**ขอบเขตผลิตภัณฑ์:** ผู้ชมเปิดดูการแข่งขันและผลที่ผู้จัดจัดเตรียม ผู้จัดกรอก/จัดการข้อมูลในหลังบ้านเอง รวมถึงเลือกทีมเข้ารายการแข่งขัน ไม่มีขั้นตอนสมัครแข่งด้วยตัวเองของทีม/ผู้เล่นในขอบเขตนี้

## 1. ขอบเขตงานและสถานะ

| ส่วน | สถานะปัจจุบัน | ไฟล์หลัก |
| --- | --- | --- |
| Team CRUD + จัดสมาชิก | ทำแล้ว: ชื่อ/คำอธิบายทีม, แบ่งหน้า, เพิ่ม ย้าย ถอดผู้เล่น | `TeamController`, `TeamServiceImpl`, `TeamMapper`, `TeamApiTests` |
| Docker + PostgreSQL + Flyway | ทำแล้ว: build Java 21, รัน PostgreSQL 17, ฐานข้อมูลทดสอบแยก | `Dockerfile`, `compose.yaml`, `application.properties` |
| Swagger/OpenAPI | ทำแล้ว: เปิด Swagger UI และ JSON spec ได้ | `pom.xml`, `README.md` |
| Player CRUD | ทำแล้ว: รายการ/ค้นชื่อ, ดูตาม id, สร้าง, แก้, ลบ และกำหนดทีมแบบไม่บังคับ | `PlayerController`, `PlayerServiceImpl`, `PlayerApiTests` |
| Team API รองรับเกมและโลโก้ | ยังไม่ทำใน API; V9 และ `Team` Entity มี `game_id`/`logo_url` แล้ว | `V9__add_games_and_tournament_format.sql`, `TeamRequest`, `TeamResponse` |
| Upload โลโก้ + Deploy | ยังไม่ทำ; ต้องตกลงที่เก็บไฟล์และเป้าหมาย deploy กับทีม | งานต่อใน `REMAINING-WORK.md` |
| Unit test Service + Use Case | ยังไม่ทำ; ปัจจุบันมี Team/Player API integration tests | `TeamApiTests`, `PlayerApiTests`, `use-case-description.md` |

งาน Game API/Auth, Tournament, สายการแข่งขัน และผล Free Fire มีเจ้าของโมดูลอื่นตาม `REMAINING-WORK.md` ให้ประสานก่อนแก้ไฟล์หรือกฎร่วม

### API ที่มีแล้ว

Base path `/api/v1/teams`:

| Method | Endpoint | หน้าที่ | ผลปกติ |
| --- | --- | --- | --- |
| GET | `/api/v1/teams?name=phoenix&page=0&size=20&sort=name,asc` | ค้นหาชื่อบางส่วนและแบ่งหน้า | 200 |
| GET | `/api/v1/teams/{id}` | ดูทีมเดียว | 200 |
| POST | `/api/v1/teams` | สร้างทีม | 201 พร้อม `Location` |
| PUT | `/api/v1/teams/{id}` | แก้ชื่อ/คำอธิบาย | 200 |
| DELETE | `/api/v1/teams/{id}` | ลบทีม | 204 |
| GET | `/api/v1/teams/{id}/players?page=0&size=20` | ดูสมาชิกทีม | 200 |
| PUT | `/api/v1/teams/{id}/players/{playerId}` | เพิ่มหรือย้ายผู้เล่นที่มีอยู่เข้าทีม | 200 |
| DELETE | `/api/v1/teams/{id}/players/{playerId}` | ถอดผู้เล่นออกจากทีม โดยไม่ลบผู้เล่น | 204 |

Request สำหรับสร้าง/แก้ทีมตอนนี้:

```json
{ "name": "Team Phoenix", "description": "ทีมจากคณะวิศวกรรมศาสตร์" }
```

`name` ต้องไม่เป็นช่องว่างและยาวไม่เกิน 150 ตัวอักษร Service ตัดช่องว่างหัวท้ายและปฏิเสธชื่อทีมซ้ำแบบไม่สนตัวพิมพ์ใหญ่เล็ก `description` เว้นว่างได้ PUT แทนค่าทั้งชุด จึงส่ง `description` มาด้วยหากต้องการเก็บค่าเดิม ทีมสร้างได้โดยยังไม่มีผู้เล่น ชื่อทีมเหมือนชื่อผู้เล่นได้เพราะเป็นคนละตาราง

กฎชื่อทีมไม่ซ้ำนี้ตรงกับ Service และคอลัมน์ `teams.name UNIQUE` ใน schema ปัจจุบัน; ยังไม่มีหลักฐานในเอกสาร Figma ว่ากำหนดกฎชื่อซ้ำละเอียดถึงตัวพิมพ์ใหญ่เล็ก ฐานข้อมูลกันชื่อที่เหมือนกันเป๊ะ ส่วนการกันต่างตัวพิมพ์เป็นการตรวจใน Service ก่อนบันทึก จึงยังไม่ได้พิสูจน์กรณีมีคำขอสร้างชื่อที่ต่างตัวพิมพ์เข้ามาพร้อมกัน

รายการแบบแบ่งหน้าตอบเป็น `{ "content": [...], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }` ค่าเริ่มต้นเรียงตามชื่อ รูปแบบ error กลางคือ `{ "message": "...", "timestamp": "..." }`: request ไม่ถูกต้อง → 400, ไม่พบทีม/ผู้เล่น → 404, ชื่อทีมซ้ำหรือข้อมูลขัดกัน → 409

Base path `/api/v1/players`:

| Method | Endpoint | หน้าที่ | ผลปกติ |
| --- | --- | --- | --- |
| GET | `/api/v1/players?name=Alice&page=0&size=20` | ค้นหาชื่อบางส่วนแบบไม่สนตัวพิมพ์และแบ่งหน้า | 200 |
| GET | `/api/v1/players/{id}` | ดูผู้เล่นเดียว | 200 |
| POST | `/api/v1/players` | สร้างผู้เล่น มีหรือไม่มีทีมก็ได้ | 201 พร้อม `Location` |
| PUT | `/api/v1/players/{id}` | แก้ข้อมูลผู้เล่นและทีม | 200 |
| DELETE | `/api/v1/players/{id}` | ลบผู้เล่น | 204 |

ตัวอย่าง request: `{ "name": "Alice", "role": "Captain", "description": "", "teamId": null }` ต้องมี `name` ไม่ว่าง/ไม่เกิน 150 ตัวอักษร และ `role` ไม่ว่าง/ไม่เกิน 100 ตัวอักษร ระบบตัดช่องว่างหัวท้ายสองฟิลด์นี้ `teamId` เว้นว่างได้ ถ้าระบุ id ทีมที่ไม่มีจริงตอบ 404 ส่วน PUT แทนค่าทั้งชุด: ไม่ส่ง `teamId` หรือส่ง `null` จะถอดผู้เล่นออกจากทีม ผู้จัดยังใช้ Team API เพิ่ม/ย้าย/ถอดผู้เล่นได้ และทั้งสอง API อ่านความสัมพันธ์เดียวกัน

### API ที่ต้องทำต่อ

Team API ยังไม่รับ `gameId`/`logoUrl`; งาน Auth, อัปโหลดโลโก้ และกฎเพิ่มเติมของทีมในรายการแข่งยังต้องประสานเจ้าของโมดูล

**แนวทางตัวตนซ้ำสำหรับ Player CRUD:** ไม่บังคับรหัสนักศึกษา เพราะจำกัดการรับผู้เล่นที่ไม่มีรหัสหรือมาจากสถาบันอื่น ตาราง `players` ปัจจุบันมีชื่อ/ตำแหน่ง/คำอธิบาย แต่ไม่มีคีย์ระบุคนจริง GET รายการใช้ค้นชื่อบางส่วนแบบไม่สนตัวพิมพ์เพื่อให้ผู้จัดเห็นรายการที่อาจซ้ำ แต่ **ยังไม่มีการ normalize ช่องว่างภายในชื่อหรือแจ้งเตือนความคล้ายโดยอัตโนมัติ** ไม่ใช้ชื่อเป็น `UNIQUE` หรือปฏิเสธการสร้างอัตโนมัติ เพราะคนละคนอาจชื่อเหมือนกันและคนเดียวกันอาจกรอกชื่อหลายแบบ การเรียงตัวอักษรในชื่อก่อนเทียบก็ไม่ใช่หลักฐานตัวตน วิธีค้นชื่อยังรับประกันว่า 1 คนมีเพียง 1 id ไม่ได้ หากภายหลังต้องการรับประกัน ต้องตกลงตัวระบุที่ยืดหยุ่นและได้รับอนุญาตให้เก็บกับทีมก่อน แล้วให้ผู้ดูแล DB เพิ่ม migration เวอร์ชันใหม่ ไม่แก้ V2 ย้อนหลัง

**ขั้นตอนหลังบ้านที่เสนอ:** ผู้จัด/ผู้ดูแลกรอกข้อมูลผู้เล่นเอง ค้นชื่อจาก API แล้วตรวจรายการที่อาจซ้ำด้วยคน หากทีมยืนยันว่าจะเก็บวันเกิด ให้ใช้วันเกิดเป็นข้อมูลประกอบการเปรียบเทียบ ไม่ใช่คีย์ UNIQUE ผู้จัดตัดสินใจว่าจะใช้ player id เดิมหรือสร้างคนใหม่ วันที่แข่งขันใช้กับกฎการแข่งขัน ไม่ใช้ระบุตัวตน ปัจจุบัน `players` ยังไม่มี `date_of_birth` และยังไม่มีสิทธิ์ผู้จัดที่บังคับใช้จริง จึงต้องตกลงก่อนว่าวันเกิดบังคับหรือไม่ ใครดู/แก้ได้ และเพิ่ม migration/API/Auth ตามเจ้าของโมดูลเมื่อได้ข้อสรุป

## 2. Setup บนเครื่องและวิธีลอง API

เปิด Docker Desktop ในโหมด Linux containers แล้วรันจาก root ของ repo ครั้งแรก:

```powershell
Copy-Item .env.example .env
docker compose up -d --build --wait
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

ถ้ามี `.env` อยู่แล้ว **ไม่ต้องคัดลอกทับ** Docker Compose อ่าน `.env` อัตโนมัติ; ไฟล์นี้เป็นค่าของแต่ละเครื่องและถูก ignore ใน Git ตั้งค่า `DB_PORT` ตามพอร์ตที่ว่างในเครื่อง เช่น 5433 หาก 5432 ถูกใช้ แอปใน Docker ติดต่อ DB ผ่าน host `db` และ port 5432 เสมอ ขณะที่โปรแกรมบนเครื่องติดต่อ `localhost` กับพอร์ตที่ตั้งใน `.env`

Dockerfile ใช้ Maven + Java 21 build JAR และรันแอปด้วย user `app` ใน container แอปรอ DB healthy; Flyway apply migration V1–V10 ตามไฟล์ปัจจุบัน แล้ว Hibernate ตรวจ schema ด้วย `ddl-auto=validate` PostgreSQL เก็บข้อมูลพัฒนาใน named volume `postgres_data`

| สิ่งที่เปิดดู | URL/คำสั่ง |
| --- | --- |
| Health | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Log แอป | `docker compose logs --tail=100 app` |

หลังแก้ Java, config หรือ `pom.xml` ให้รัน `docker compose up -d --build --wait app` เพื่อให้ container ใช้ JAR ใหม่ หน้า `/` ยังไม่มีเว็บหลัก จึงอาจตอบ 404 แม้ API และ health ใช้งานได้

ถ้าจะ debug จาก VS Code/IDE ให้ใช้ **JDK 21**, หยุด app container ด้วย `docker compose stop app`, เปิด DB ด้วย `docker compose up -d --wait db` แล้วรัน `TournamentApplication.java` หรือ `.\mvnw.cmd spring-boot:run` Spring Boot ที่รันจาก IDE **ไม่อ่าน `.env` อัตโนมัติ** ให้ตั้ง `DB_HOST=localhost`, `DB_PORT` และค่า DB อื่นใน Run Configuration ให้ตรง `.env`

ตรวจฐานข้อมูลด้วย `docker compose exec db psql -U <DB_USERNAME> -d <DB_NAME>` แล้วใช้ `\dt` ดูตาราง หรือ query `flyway_schema_history` เพื่อตรวจ migration หลัง migration ถูก apply แล้ว ให้สร้างไฟล์เวอร์ชันถัดไปเมื่อจำเป็น อย่าแก้ V1–V10 ย้อนหลัง

ปัญหาที่พบบ่อย: Docker daemon ติดต่อไม่ได้ → เปิด Docker Desktop; พอร์ตชน → เปลี่ยน `APP_PORT`/`DB_PORT` ใน `.env`; health ไม่ผ่าน → ดู `docker compose logs --tail=150 app db` เพื่อแยกปัญหา DB/Flyway/schema; เปลี่ยนรหัสใน `.env` แล้วเข้าฐานข้อมูลเดิมไม่ได้ → ค่าบัญชี PostgreSQL ใน volume ถูกสร้างตั้งแต่ครั้งแรก การเปลี่ยน `.env` ไม่เปลี่ยนบัญชีเดิม; เปิด Swagger แล้วไม่เห็นโค้ดใหม่ → rebuild app

## 3. โครงสร้างโค้ดและการทำงาน

```text
HTTP/Swagger → Controller + @Valid Request DTO
             → Service (กฎธุรกิจ, @Transactional)
             → Repository/JPA → PostgreSQL
             → Mapper → Response DTO → HTTP response
```

ตัวอย่าง `POST /api/v1/teams`: `TeamController.create` รับ `TeamRequest`, `TeamServiceImpl.create` trim ชื่อและตรวจซ้ำ, `TeamRepository.saveAndFlush` บันทึก, `TeamMapper.toResponse` เลือกฟิลด์ตอบ, Controller ตอบ 201 และ `Location` `GlobalExceptionHandler` รับ exception จาก validation/service/ฐานข้อมูลและแปลงเป็น 400/404/409

`teams.id` และ `players.id` สร้างโดยฐานข้อมูล ผู้เรียกไม่ต้องกำหนดเอง `players.name`/`role` บังคับ แต่ `team_id` เว้นว่างได้ `PUT /teams/{id}/players/{playerId}` ตั้ง `player.team` เพื่อเพิ่มหรือย้ายทีม; DELETE ความสัมพันธ์ตั้ง `player.team = null` โดยเก็บผู้เล่นไว้ หากลบทีม migration V4 ตั้ง `players.team_id` เป็น NULL ด้วย ผู้เล่น **หนึ่ง id** มี `team_id` ได้ค่าเดียว จึงไม่ปรากฏพร้อมกันในสองทีมจากความสัมพันธ์นี้ แต่คนจริงคนเดียวอาจถูกบันทึกเป็นผู้เล่นสองแถวคนละ id ได้จนกว่าจะกำหนดวิธีตรวจตัวตนตามหัวข้อด้านบน

`TeamResponse` ปัจจุบันตอบเพียง `id`, `name`, `description`, `createdAt`; Entity มี `game`/`logoUrl` แล้วแต่ API ยังไม่ใช้ ส่วน `tournament_teams` (V6) และ `matches` (V7) อ้างทีม ไม่ได้อ้างผู้เล่นโดยตรง เกม Fighting Game มี `min_players = 1` ใน V9 หากใช้ schema เดิม ผู้จัดอาจสร้างทีมสมาชิก 1 คนแล้วเลือกเข้ารายการ; ถ้าต้องการให้คู่แข่งเป็นผู้เล่นเดี่ยวโดยตรง ต้องตกลงกับทีมก่อนเปลี่ยน schema/หน้าแสดงผล

## 4. ต้องตกลงกับเพื่อนก่อนเชื่อมระบบ

| เรื่อง | คุยกับใคร | ผลต่อโมดูลนี้ |
| --- | --- | --- |
| `Game` และ `gameId` ในทีม | คนทำ Game API/ฐานข้อมูล และ Tournament | ทีมใหม่ต้องเลือกเกมอย่างไร, เกมไม่มีจริง → 404, ทีมเก่าที่ `game_id` ยัง NULL, ห้ามทีมเข้า Tournament เกมไม่ตรง |
| การแก้สมาชิกหลังผู้จัดเพิ่มทีมเข้ารายการ | คนทำ Tournament/สายการแข่งขัน | `addPlayer/removePlayer` ตอนนี้ไม่เช็กสถานะรายการหรือจำนวนสมาชิกขั้นต่ำ ต้องตกลงช่วงเวลาที่ล็อก roster |
| การลบทีม | คนทำ Tournament/Match | V6 ลบความสัมพันธ์ทีมในรายการแบบ cascade แต่ match มี FK แบบ restrict ต้องกำหนดนโยบายก่อนให้ลบทีมที่เคยแข่ง |
| Upload โลโก้ | คนทำ Game/Tournament และ frontend | ตกลงที่เก็บไฟล์, ชนิด/ขนาด, URL และสิทธิ์ร่วมกันก่อนทำ `FileStorageService` |
| Auth/Admin | คนทำ Auth | หลังเปิด Security ต้องให้ Swagger, OpenAPI, health ใช้ได้ตามกฎทีม และปรับ tests ที่เรียก API เขียนข้อมูล |
| ย้ายโครงโปรเจกต์/Deploy | คนทำ DB/CI และทีม | หากย้าย source ไป `code/` ต้องแก้ Dockerfile/Compose/CI/setup พร้อมกัน; ตกลง cloud และ DB ก่อน deploy |

**ช่องว่างที่เห็นจากโค้ดตอนนี้:** `TeamServiceImpl.create` ยังไม่ตั้ง `game` ทำให้ทีมที่สร้างผ่าน API มี `game_id = NULL`; การลบทีมเรียก Repository ตรงโดยยังไม่มีกฎตามประวัติแข่ง ส่วน roster, Auth, upload และ deploy เป็นเรื่องที่ต้องตรวจเมื่อโมดูลเหล่านั้นพร้อม อย่าอ้างว่าเกิด bug จริงแล้วหากยังไม่ได้ลองร่วมกัน

### ความยืดหยุ่นเมื่อเพิ่มเกม กติกา และรูปแบบตารางแข่ง

| สิ่งที่อยากเพิ่ม | โครงปัจจุบันรองรับแค่ไหน | งานที่ต้องทำต่อ |
| --- | --- | --- |
| เกมใหม่ที่แข่งเป็นทีมสองฝ่ายและใช้แพ้คัดออกแบบเดิม | **รองรับทางข้อมูลเป็นส่วนใหญ่:** `games` มี `code`, `name`, `min_players`; `teams`/`tournaments` มี `game_id` และผลแมตช์เก็บคะแนนสองทีมกับผู้ชนะ | เพิ่มข้อมูลเกมและต่อ Game/Tournament/Team API ที่ยังไม่ครบ; ตรวจจำนวนผู้เล่น/กติกาเฉพาะเกม |
| เปลี่ยนแต้มอันดับหรือแต้มต่อ kill ของรายการแบบ Free Fire | **รองรับบางส่วน:** V10 มี `tournament_placement_points` ต่อรายการและ `points_per_kill`; ผลดิบมี `placement`, `kills` | ทำ Service/API คำนวณและจัดอันดับให้ตรงกติกาที่ทีมยืนยัน; กติกาอื่นเช่น objective, time หรือ penalty ยังไม่มีฟิลด์ผลดิบ |
| เกมหลายทีมต่อเกมที่ใช้ข้อมูลผลต่างจากอันดับและ kills | **ยังไม่พอ:** `free_fire_game_results` เป็นโมเดลเฉพาะผลแบบ Free Fire | ออกแบบตารางผลเฉพาะชนิดหรือโมเดลผลที่ตกลงกัน แล้วเพิ่ม migration ใหม่; อย่าฝืนยัดค่าใหม่ใน `kills`/`placement` |
| ตารางแบบพบกันหมด, Swiss, แบ่งกลุ่ม, แพ้คัดออกสองครั้ง | **ยังไม่รองรับครบ:** `TournamentFormat` และ CHECK ใน V9 รับแค่ `SINGLE_ELIMINATION`/`POINTS`; `matches` มีสองทีมและ `next_match_id` ตัวเดียวสำหรับทางเดินผู้ชนะ | เพิ่ม format/กฎสร้างตาราง/ทางเดินผลและการจัดอันดับตามชนิด; บางแบบใช้ `matches` เก็บคู่ได้ แต่ความสัมพันธ์รอบ/ผลรวมอาจต้องตารางหรือคอลัมน์ใหม่ |
| ผู้เล่นเดี่ยวเป็นคู่แข่งโดยตรง | **ยังไม่รองรับ:** ตารางเข้ารายการและแมตช์อ้าง `team_id` | ใช้ทีมสมาชิก 1 คนตามนโยบายปัจจุบัน หรือออกแบบ participant แบบบุคคลใหม่โดยตกลงกับเจ้าของ Tournament/DB |

สรุปด้านการขยายฟีเจอร์: core ของเกม/รายการ/ทีมใช้ต่อได้ แต่ **ตารางปัจจุบันไม่ใช่ schema อเนกประสงค์สำหรับทุกกติกา** ให้เพิ่ม migration ใหม่และแยก logic ตามรูปแบบเมื่อมี requirement จริง ไม่แก้ V1–V10 ย้อนหลัง ด้านปริมาณข้อมูลยังไม่มีผลวัดความเร็ว; แม้มี index และ pagination บางจุด ก็ต้องทดสอบกับข้อมูล/จำนวนผู้ชมจริงก่อนอ้างว่ารองรับโหลดมาก

## 5. ลำดับลงมือทำของคนที่ 2

1. **ต่อ Team API กับเกมและโลโก้:** เพิ่ม `gameId`/`logoUrl` ใน Request/Response/Mapper/Service ตรวจ id เกมที่ไม่มีจริง → 404 และรักษาข้อมูลทีมเก่าที่เกมยังว่างตามข้อตกลงกับทีม
2. **เพิ่ม unit tests ของ TeamServiceImpl:** ทดสอบกฎชื่อซ้ำ, เกมไม่มีจริง, ย้าย/ถอดผู้เล่น และกรณี conflict ที่ตกลงกับทีม โดยไม่แทน integration tests เดิม
3. **Upload/Deploy/Use Case:** ลงมือหลังตกลงขอบเขตกับเจ้าของ Game/Tournament/frontend; ทำ Use Case Diagram และ `use-case-description.md` จากพฤติกรรม API ที่เสร็จจริง
4. **ตรวจคนซ้ำให้ละเอียดขึ้นถ้าทีมยืนยัน:** ปัจจุบันมีเพียงค้นชื่อบางส่วน; แนวทาง normalize ชื่อ/แจ้งเตือนและข้อมูลประกอบตัวตนยังไม่ทำ

Player CRUD ใช้รูปแบบเดียวกับ Team CRUD แต่ความสัมพันธ์กับทีมเป็น optional; `PlayerRepository` สืบทอด `JpaRepository` เมธอด `findByTeamId` ใช้แสดงสมาชิกทีม และ `findByNameContainingIgnoreCase` ใช้ค้นชื่อผู้เล่น

## 6. วิธีทดสอบและเกณฑ์ส่งมอบ

```powershell
docker compose --profile test build tests
docker compose --profile test run --rm tests
docker compose --profile test stop test-db
```

ชุดทดสอบใช้ `test-db` แยกจากฐานข้อมูลพัฒนาและใช้ tmpfs `TeamApiTests` ตรวจ Team API, `PlayerApiTests` ตรวจ Player CRUD และ `PlayerTeamIntegrationTests` ตรวจการทำงานร่วมกันของสอง API ทั้งสามไฟล์เป็น **integration test** ด้วย MockMvc: request เดินผ่าน Controller, Service และฐานข้อมูลทดสอบจริง ไม่ใช่ Mockito unit test

| กรณี Player API | ผลที่คาดหวัง | ผลจริงเมื่อรัน 8 ต.ค. 2026 |
| --- | --- | --- |
| GET ตาม id และค้นชื่อ | 200; ฟิลด์ตรงข้อมูลเดิม; ค้นชื่อไม่สนตัวพิมพ์ได้ผู้เล่นที่บันทึก | ผ่าน |
| POST ไม่มีทีม | 201; ตัดช่องว่างชื่อ/role; GET กลับได้ค่าที่ส่งและ `teamId` ว่าง | ผ่าน |
| PUT เปลี่ยนข้อมูลและกำหนด/ถอดทีม | 200; GET ผู้เล่นเห็นค่าใหม่; ถอดทีมแล้ว `teamId` ว่าง | ผ่าน |
| DELETE ผู้เล่นในทีม | 204; GET ผู้เล่นเป็น 404 และแถวผู้เล่นถูกลบ | ผ่าน |
| ชื่อ/role ว่าง หรืออ้างทีมที่ไม่มี | 400 สำหรับข้อมูลบังคับ; 404 สำหรับทีม; ไม่มีผู้เล่นใหม่ถูกบันทึก | ผ่าน |
| PUT อ้างทีมที่ไม่มี | 404; ข้อมูลผู้เล่นเดิมไม่เปลี่ยน | ผ่าน |
| สร้างผู้เล่นสอง id ชื่อเดียวกัน | สร้างได้ทั้งสอง; ค้นชื่อพบสองรายการ ไม่ถือว่าคนจริงคนเดียวกันอัตโนมัติ | ผ่าน |
| `PlayerTeamIntegrationTests`: สร้างผู้เล่นผ่าน Player API แล้วเพิ่ม/ย้าย/ถอดทีมผ่าน Team API | GET Player และ GET สมาชิกทีมสะท้อน `teamId` เดียวกันทุกขั้น; หลังถอดผู้เล่นยังอยู่แต่ไม่มีทีม | ผ่าน |

`TeamApiTests` ผ่าน 9/9, `PlayerApiTests` ผ่าน 7/7 และ `PlayerTeamIntegrationTests` ผ่าน 1/1; รอบล่าสุดรันทั้งโปรเจกต์ **31 tests, 0 failures/errors/skipped** ด้วย `docker compose --profile test run --rm --build tests` หลัง rebuild แอปพัฒนา ตรวจ `/actuator/health` ได้ `UP` และ `/v3/api-docs` มี `/api/v1/players` กับ `/api/v1/players/{id}` แล้ว ยังไม่ได้ยิง Player POST/PUT/DELETE กับฐานข้อมูลพัฒนาเพื่อไม่เพิ่มข้อมูลทดสอบใน volume หลัก

ก่อนส่ง PR ให้ตรวจ: Swagger แสดง endpoint ใหม่จริง, status/error ตรงกฎ, `git diff --check`, tests ผ่านบนโค้ดที่ส่ง, README/คู่มือนี้ตรงพฤติกรรมล่าสุด และแจ้งเจ้าของโมดูลที่ใช้ไฟล์ร่วม `.env` เป็นค่า local ไม่ commit และไม่ใส่รหัสผ่านหรือ token ในเอกสาร/PR
