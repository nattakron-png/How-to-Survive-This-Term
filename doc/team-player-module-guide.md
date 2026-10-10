# คู่มือโมดูลทีมและผู้เล่น (คนที่ 2: วัชรพล)

โมดูลนี้ดูแลข้อมูลทีม ผู้เล่น และการสังกัดทีม รวมถึงสภาพแวดล้อม Docker, Swagger UI และการทดสอบ API ที่เกี่ยวข้อง คู่มือนี้เป็นจุดอ่านร่วมของทีมสำหรับส่วนคนที่ 2 โดยแยก **สิ่งที่ทำแล้ว** ออกจาก **งานที่ยังต้องพัฒนา**; อย่านำตัวอย่างของงานที่ยังไม่ทำไปอ้างว่าเป็น API ปัจจุบัน

**ขอบเขตผลิตภัณฑ์:** ผู้ชมเปิดดูการแข่งขันและผลที่ผู้จัดจัดเตรียม ผู้จัดกรอก/จัดการข้อมูลในหลังบ้านเอง รวมถึงเลือกทีมเข้ารายการแข่งขัน ไม่มีขั้นตอนสมัครแข่งด้วยตัวเองของทีม/ผู้เล่นในขอบเขตนี้

## 1. ขอบเขตงานและสถานะ

| ส่วน | สถานะปัจจุบัน | ไฟล์หลัก |
| --- | --- | --- |
| Team CRUD | ทำแล้ว: ชื่อ/คำอธิบาย/เกมของทีม, แบ่งหน้า, สร้าง/แก้/ลบ | `TeamController`, `TeamServiceImpl`, `TeamMapper`, `TeamApiTests` |
| จัดสมาชิกทีม | ทำแล้ว: ดูสมาชิกพร้อมกรองชื่อ/role, เพิ่ม/ย้าย/ถอดผู้เล่น โดยคง URL เดิม | `TeamMembershipController`, `TeamMembershipServiceImpl`, `TeamMembershipMapper`, `TeamApiTests` |
| Docker + PostgreSQL + Flyway | ทำแล้ว: build Java 21, รัน PostgreSQL 17, ฐานข้อมูลทดสอบแยก | `Dockerfile`, `compose.yaml`, `application.properties` |
| Swagger/OpenAPI | ทำแล้ว: เปิด Swagger UI และ JSON spec ได้ | `pom.xml`, `README.md` |
| Player CRUD | ทำแล้ว: รายการ/ค้นชื่อ, ดูตาม id, สร้าง, แก้, ลบ และกำหนดทีมแบบไม่บังคับ | `PlayerController`, `PlayerServiceImpl`, `PlayerApiTests` |
| Team API รองรับเกมและโลโก้ | สร้างทีมใหม่ต้องมี `gameId`; สร้าง/แก้/อ่านทีมส่งค่าเกมกลับ พร้อมอัปโหลดและแสดง `logoUrl` | `TeamLogoController`, `TeamRequest`, `TeamResponse`, `TeamMapper`, `TeamServiceImpl` |
| Upload โลโก้ + Deploy | โลโก้ทีมเก็บใน local volume สำหรับพัฒนาแล้ว; Game/Tournament และ cloud storage ยังต้องตกลงกับทีม | `FileStorageService`, `LocalFileStorageService`, `compose.yaml` |
| Unit test Service + Use Case | Mockito ของ Team CRUD/เกม 5 กรณี, สมาชิกทีม 3 กรณี, Player CRUD 4 กรณี; Use Case ยังไม่ทำ | `TeamServiceImplTest`, `TeamMembershipServiceImplTest`, `PlayerServiceImplTest`, `use-case-description.md` |

งาน Game API/Auth, Tournament, สายการแข่งขัน และผล Free Fire มีเจ้าของโมดูลอื่นตาม `REMAINING-WORK.md` ให้ประสานก่อนแก้ไฟล์หรือกฎร่วม

การประเมิน SOLID และหลักการออกแบบของโมดูลนี้พร้อมจุดที่ยังอ้างว่าเคร่งครัดไม่ได้ ดูที่ [solid-analysis.md](solid-analysis.md) ส่วนของคนที่ 2

### API ที่มีแล้ว

Base path `/api/v1/teams`:

| Method | Endpoint | หน้าที่ | ผลปกติ |
| --- | --- | --- | --- |
| GET | `/api/v1/teams?name=phoenix&page=0&size=20&sort=name,asc` | ค้นหาชื่อบางส่วนและแบ่งหน้า | 200 |
| GET | `/api/v1/teams/{id}` | ดูทีมเดียว | 200 |
| POST | `/api/v1/teams` | สร้างทีม | 201 พร้อม `Location` |
| PUT | `/api/v1/teams/{id}` | แก้ชื่อ/คำอธิบาย; กำหนดเกมครั้งแรกให้ทีมเก่าที่เกมว่างได้ แต่เปลี่ยนเกมที่กำหนดแล้วไม่ได้ | 200 |
| DELETE | `/api/v1/teams/{id}` | ลบทีม | 204 |
| GET | `/api/v1/teams/{id}/players?name=alice&role=Captain&page=0&size=20` | ดูสมาชิกทีม; `name` และ `role` ไม่บังคับ กรองร่วมกันได้ | 200 |
| PUT | `/api/v1/teams/{id}/players/{playerId}` | เพิ่มหรือย้ายผู้เล่นที่มีอยู่เข้าทีม | 200 |
| DELETE | `/api/v1/teams/{id}/players/{playerId}` | ถอดผู้เล่นออกจากทีม โดยไม่ลบผู้เล่น | 204 |
| PUT | `/api/v1/teams/{id}/logo` | อัปโหลดโลโก้ทีมแบบ `multipart/form-data` ส่วน `file` | 200 พร้อม `logoUrl` |
| GET | `/api/v1/logos/{filename}` | อ่านรูปจาก `logoUrl` ของทีม | 200 พร้อมภาพ |

Request สำหรับสร้างทีมตอนนี้:

```json
{ "name": "Team Phoenix", "description": "ทีมจากคณะวิศวกรรมศาสตร์", "gameId": 1 }
```

`name` ต้องไม่เป็นช่องว่างและยาวไม่เกิน 150 ตัวอักษร Service ตัดช่องว่างหัวท้ายและปฏิเสธชื่อทีมซ้ำแบบไม่สนตัวพิมพ์ใหญ่เล็ก `description` เว้นว่างได้ ทีมใหม่ต้องส่ง `gameId` ที่มีอยู่ในตาราง `games` (ไม่ส่ง → 400, id ไม่มี → 404); ตัวเลข 1 ในตัวอย่างเป็นเพียงตัวอย่าง ให้ใช้ id ที่มีอยู่จริงในฐานข้อมูลนั้น ทีมสร้างได้โดยยังไม่มีผู้เล่น ชื่อทีมเหมือนชื่อผู้เล่นได้เพราะเป็นคนละตาราง

`TeamResponse` จาก POST/GET/list/PUT ส่ง `gameId` พร้อม `logoUrl` ด้วย PUT ที่ไม่ส่ง `gameId` จะคงเกมเดิมไว้ รวมถึงทีมเก่าที่ `game_id` เป็น NULL; ทีมเก่าที่เกมว่างกำหนดเกมครั้งแรกได้ แต่ทีมที่มีเกมแล้วส่ง id เกมอื่นจะตอบ 409 โดยไม่แก้ข้อมูลทีม ต้องสร้างทีมใหม่สำหรับอีกเกม แม้ใช้ชุดผู้เล่นเดิม หากส่ง id เกมที่ไม่มีจริงตอบ 404 ส่วน `description` ยังแทนค่าตาม request จึงต้องส่งซ้ำหากต้องการเก็บค่าเดิม

การใช้ผู้เล่นคนเดิมในทีมใหม่คนละเกมยังมีข้อจำกัด: `players.team_id` ให้ผู้เล่น 1 record อยู่ได้เพียงทีมเดียวในขณะหนึ่ง การย้าย record เดิมไปทีมใหม่จะทำให้สมาชิกทีมเดิมหายจากมุมมองปัจจุบัน และยังไม่มี snapshot รายชื่อแยกตามทัวร์ ต้องออกแบบความสัมพันธ์สมาชิก/ประวัติกับเจ้าของ DB และ Tournament ก่อนรองรับผู้เล่นชุดเดิมในหลายทีมพร้อมกัน ไม่ควรสร้าง player id ซ้ำเพื่อเลี่ยงข้อจำกัดนี้

อัปโหลดโลโก้หลังสร้างทีมด้วย `PUT /api/v1/teams/{id}/logo` ส่ง `multipart/form-data` ชื่อส่วน `file` (PNG หรือ JPEG, ไม่เกิน 2 MB และ 4096 × 4096 พิกเซล) เช่น `curl -X PUT -F "file=@logo.png" http://localhost:8080/api/v1/teams/1/logo` ผลตอบมี `logoUrl` เป็น path `/api/v1/logos/{filename}` ให้เว็บนำไปแสดงด้วย GET; `GET /api/v1/teams/{id}` และ list ก็ส่ง URL นี้ด้วย ไฟล์ผิดรูปแบบตอบ 400, ทีมไม่พบตอบ 404 ไฟล์เก่าหลังเปลี่ยนโลโก้ยังอยู่ใน storage เพื่อไม่ทำลาย URL ที่อาจถูกอ้างอิง ต้องกำหนดนโยบาย cleanup ภายหลัง

Docker Compose เก็บรูปใน named volume `logo_data` ที่ `/app/uploads/logos` แยกจาก volume ฐานข้อมูล จึงยังอยู่หลัง `docker compose down` และแอป user `app` เขียนได้ หากรันจาก IDE ใช้ `uploads/logos` ใน repo ซึ่งถูก `.gitignore`; เปลี่ยนได้ผ่าน `LOGO_STORAGE_DIR` ค่า path นี้เป็นของเครื่องที่รัน ไม่ใช่ค่าที่สมาชิกทุกคนต้องใช้ร่วมกัน ฝั่ง deploy ยังต้องเลือกที่เก็บถาวรและเพิ่ม implementation ของ `FileStorageService`; URL ปัจจุบันเป็น path ภายในแอปและยังไม่มีการจำกัดสิทธิ์ upload เพราะระบบ Auth ยังไม่เสร็จ

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

### API และกฎที่ต้องทำต่อ

Team API รับ/ตอบ `gameId` แล้ว; `logoUrl` แสดงใน TeamResponse และตั้งผ่าน upload endpoint เท่านั้น เพื่อไม่ให้ PUT ข้อมูลทีมลบ URL เดิมโดยไม่ตั้งใจ งาน Auth, ที่เก็บไฟล์สำหรับ deploy และกฎเปลี่ยนเกม/สมาชิกของทีมในรายการแข่งยังต้องประสานเจ้าของโมดูล

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

**ขอบเขตคลาสหลังแยกหน้าที่:** `TeamController` → `TeamService`/`TeamServiceImpl` → `TeamRepository`/`TeamMapper` ดูแลข้อมูลทีม; `TeamMembershipController` → `TeamMembershipService`/`TeamMembershipServiceImpl` → `PlayerRepository`/`TeamMembershipMapper` ดูแลความสัมพันธ์สมาชิก; `PlayerController` → `PlayerService`/`PlayerServiceImpl` → `PlayerRepository`/`PlayerMapper` ดูแลข้อมูลผู้เล่นและ `teamId` ปัจจุบัน Controller ไม่เรียก Repository ตรง ส่วน endpoint `/api/v1/teams/{id}/players` ทั้งสาม method ยังใช้ URL และ response เดิมเพื่อไม่กระทบผู้เรียก

`teams.id` และ `players.id` สร้างโดยฐานข้อมูล ผู้เรียกไม่ต้องกำหนดเอง `players.name`/`role` บังคับ แต่ `team_id` เว้นว่างได้ `PUT /teams/{id}/players/{playerId}` ตั้ง `player.team` เพื่อเพิ่มหรือย้ายทีม; DELETE ความสัมพันธ์ตั้ง `player.team = null` โดยเก็บผู้เล่นไว้ หากลบทีม migration V4 ตั้ง `players.team_id` เป็น NULL ด้วย ผู้เล่น **หนึ่ง id** มี `team_id` ได้ค่าเดียว จึงไม่ปรากฏพร้อมกันในสองทีมจากความสัมพันธ์นี้ แต่คนจริงคนเดียวอาจถูกบันทึกเป็นผู้เล่นสองแถวคนละ id ได้จนกว่าจะกำหนดวิธีตรวจตัวตนตามหัวข้อด้านบน

`TeamResponse` ตอบ `id`, `name`, `description`, `logoUrl`, `createdAt`; `logoUrl` เป็น `null` ก่อนอัปโหลด ส่วน `game` ใน Entity ยังไม่อยู่ใน Team API `tournament_teams` (V6) และ `matches` (V7) อ้างทีม ไม่ได้อ้างผู้เล่นโดยตรง เกม Fighting Game มี `min_players = 1` ใน V9 หากใช้ schema เดิม ผู้จัดอาจสร้างทีมสมาชิก 1 คนแล้วเลือกเข้ารายการ; ถ้าต้องการให้คู่แข่งเป็นผู้เล่นเดี่ยวโดยตรง ต้องตกลงกับทีมก่อนเปลี่ยน schema/หน้าแสดงผล

## 4. ต้องตกลงกับเพื่อนก่อนเชื่อมระบบ

| เรื่อง | คุยกับใคร | ผลต่อโมดูลนี้ |
| --- | --- | --- |
| `Game` และ `gameId` ในทีม | คนทำ Game API/ฐานข้อมูล และ Tournament | Team API ล็อกเกมหลังตั้งค่าแล้ว; ต้องตกลงวิธีให้ผู้เล่นชุดเดิมอยู่ในทีมใหม่คนละเกมและเก็บประวัติ roster โดยไม่สร้าง player id ซ้ำ |
| การแก้สมาชิกหลังผู้จัดเพิ่มทีมเข้ารายการ | คนทำ Tournament/สายการแข่งขัน | `addPlayer/removePlayer` ตอนนี้ไม่เช็กสถานะรายการหรือจำนวนสมาชิกขั้นต่ำ ต้องตกลงช่วงเวลาที่ล็อก roster |
| การลบทีม | คนทำ Tournament/Match | V6 ลบความสัมพันธ์ทีมในรายการแบบ cascade แต่ match มี FK แบบ restrict ต้องกำหนดนโยบายก่อนให้ลบทีมที่เคยแข่ง |
| Upload โลโก้ | คนทำ Game/Tournament และ frontend | Team API ใช้ local storage แล้ว; ตกลงการใช้ร่วมกัน, cloud storage และสิทธิ์ก่อนต่อ Game/Tournament |
| Auth/Admin | คนทำ Auth | หลังเปิด Security ต้องให้ Swagger, OpenAPI, health ใช้ได้ตามกฎทีม และปรับ tests ที่เรียก API เขียนข้อมูล |
| ย้ายโครงโปรเจกต์/Deploy | คนทำ DB/CI และทีม | หากย้าย source ไป `code/` ต้องแก้ Dockerfile/Compose/CI/setup พร้อมกัน; ตกลง cloud และ DB ก่อน deploy |

**ช่องว่างที่เห็นจากโค้ดตอนนี้:** ทีมที่สร้างผ่าน API ใหม่มี `game_id` และเปลี่ยนเกมเดิมไม่ได้แล้ว แต่ `players.team_id` ยังรองรับสมาชิกปัจจุบันเพียงทีมเดียว; การลบทีมเรียก Repository ตรงโดยยังไม่มีกฎตามประวัติแข่ง ส่วน roster, Auth, upload ของ Game/Tournament และ deploy เป็นเรื่องที่ต้องตรวจเมื่อโมดูลเหล่านั้นพร้อม อย่าอ้างว่าเกิด bug จริงแล้วหากยังไม่ได้ลองร่วมกัน

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

1. **Team API กับเกมทำแล้ว:** เพิ่ม `gameId` ใน Request/Response/Mapper/Service ตรวจเกมไม่มีจริง → 404, สร้างใหม่ขาดเกม → 400, รักษาข้อมูลทีมเก่าที่เกมยังว่าง และปฏิเสธการเปลี่ยนเกมที่กำหนดแล้ว → 409; งานต่อคือรูปแบบสมาชิกหลายทีม/ประวัติ roster ข้ามเกม
2. **Unit tests ตามหน้าที่ทำแล้ว:** `TeamServiceImplTest` ตรวจชื่อซ้ำ/เกมขาด/เกมไม่มีจริง; `TeamMembershipServiceImplTest` ตรวจการเพิ่ม ย้าย ถอดสมาชิก; `PlayerServiceImplTest` ตรวจ Player CRUD โดยคง integration tests เดิม
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

`TeamApiTests` ผ่าน 9/9, `PlayerApiTests` ผ่าน 7/7 และ `PlayerTeamIntegrationTests` ผ่าน 1/1; เมื่อ 8 ต.ค. รันทั้งโปรเจกต์ **31 tests, 0 failures/errors/skipped** ด้วย `docker compose --profile test run --rm --build tests` หลัง rebuild แอปพัฒนา ตรวจ `/actuator/health` ได้ `UP` และ `/v3/api-docs` มี `/api/v1/players` กับ `/api/v1/players/{id}` แล้ว ยังไม่ได้ยิง Player POST/PUT/DELETE กับฐานข้อมูลพัฒนาเพื่อไม่เพิ่มข้อมูลทดสอบใน volume หลัก

หลังแยก Team CRUD ออกจากการจัดสมาชิก วันที่ 9 ต.ค. 2026 รัน Mockito ของ `TeamServiceImplTest`, `TeamMembershipServiceImplTest` และ `PlayerServiceImplTest` ผ่าน 9/9 ด้วย Maven/JDK 26 ในเครื่อง จากนั้นรัน `docker compose --profile test run --rm --build tests` กับ PostgreSQL test-db บน Java 21 ผ่านทั้งโปรเจกต์ 66 tests, 0 failures/errors/skipped แล้วหยุด test-db; URL และผลตอบกลับของ Team/Player API ยังผ่าน integration tests เดิม ไม่ได้เพิ่มกฎสมาชิกประจำทัวร์หรือแก้โมดูล Tournament

หลังรับ `origin/develop` ที่ `23691b9` เข้าสู่ branch refactor เดียวกัน ตัวกรอง `name`/`role` ที่เพิ่มใน develop ถูกย้ายจาก Team CRUD ไป `TeamMembershipServiceImpl` โดยคงพฤติกรรมเดิม: ชื่อค้นบางส่วนแบบไม่สนตัวพิมพ์, role เทียบตรงแบบไม่สนตัวพิมพ์, ส่งทั้งคู่ต้องตรงทั้งคู่ และตัดช่องว่างหัวท้าย ก่อนแก้ เทสรวม 111 กรณีล้ม 2 กรณีเพราะ refactor ยังไม่ส่งตัวกรอง; หลังแก้รัน Docker + PostgreSQL ผ่าน **111/111**, 0 failures/errors/skipped และหยุด test-db แล้ว เทสที่ล้มตรวจ requirement ใหม่ถูกต้อง สาเหตุมาจากโค้ด refactor

เมื่อเพิ่มโลโก้ทีม รัน `TeamLogoApiTests` บน Docker + PostgreSQL ผ่าน 4/4: อัปโหลด/อ่านภาพและ URL จากฐานข้อมูล, PUT ทีมแล้วโลโก้ไม่หาย, ปฏิเสธไฟล์ปลอม/ภาพกว้างเกินกำหนดโดยไม่เปลี่ยนทีม และปฏิเสธทีมที่ไม่มีจริง รันทั้งโปรเจกต์บนฐานทดสอบใหม่ผ่าน **115/115**, 0 failures/errors/skipped; runtime image build ผ่านและตรวจ user `app` เขียน named volume โลโก้ได้ การอัปโหลดผ่าน HTTP ของ runtime container จริงยังไม่ได้ลองแยกจาก MockMvc

เมื่อเพิ่ม `gameId` และล็อกเกมของทีมวันที่ 9 ต.ค. 2026 รัน Docker + PostgreSQL บน branch ปัจจุบันผ่าน **121/121** และจำลองรวม source/test กับ `origin/develop` ที่ `59a826c` ใน worktree แยกผ่าน **137/137**, 0 failures/errors/skipped; ตรวจสร้างทีมแล้ว DB/GET/list ตอบเกมตรงกัน, ขาดเกม 400, id เกมไม่มี 404 โดยไม่บันทึกทีม, PUT ไม่ส่งเกมคงค่าเดิม, เกมเดิมส่งซ้ำได้, เกมอื่นตอบ 409 โดยไม่แก้ชื่อ/เกมใน DB และทีมเก่า `game_id = NULL` ยังแก้ข้อมูลอื่นหรือกำหนดเกมภายหลังได้ การรวมนี้เป็นการทดสอบชั่วคราว ยังไม่ได้ merge develop รอบใหม่ลง branch งานจริง

`TeamPlayerTournamentFlowTests` ทดสอบ flow ผู้จัดสร้างทีม ROV → เพิ่มผู้เล่น 5 คน → สร้างทัวร์ ROV → ลงทีม → ปฏิเสธทีมต่างเกม → ปฏิเสธการเปลี่ยนเกมของทีมเดิมและตรวจข้อมูลเดิมยังอยู่ ขั้นลงทีมเรียก `TournamentTeamService` โดยตรงเพราะยังไม่มี HTTP endpoint สำหรับขั้นนี้ บนฐาน PostgreSQL ทดสอบใหม่ flow ผ่าน **1/1** และชุดเต็มผ่าน **122/122** วันที่ 9 ต.ค. 2026 ขณะทดสอบพบว่า POST ทัวร์ที่เว้น `pointsPerKill` ได้ 409 จากค่า NULL ในคอลัมน์ NOT NULL แม้ DB มี default 1; fixture จึงส่ง `pointsPerKill: 1` เพื่อทดสอบ flow ที่เหลือ ประเด็นค่าเริ่มต้นนี้เป็นงานของเจ้าของ Tournament ที่ต้องแก้และเพิ่ม test แยก

วันที่ 10 ต.ค. 2026 เพิ่ม `TeamTournamentHistoryBehaviorTests` เพื่อยืนยัน **พฤติกรรมปัจจุบัน** กับ PostgreSQL จริง 3 กรณี: ทีมเดียวลงสองทัวร์วันไม่ซ้อนกันแล้วเปลี่ยนชื่อทีม/ถอดผู้เล่นหลังตั้งสถานะทัวร์ `COMPLETED` ได้ โดยทั้งสองรายการยังอ้างทีม id เดียวและจำนวนสมาชิกปัจจุบันลดลง; ลบทีมที่มีเพียงการลงทะเบียนแล้วรายการลงทะเบียนของทั้งสองทัวร์หาย (ทัวร์ยังอยู่); ถ้ามีแมตช์อ้างทีม การลบได้ 409 จาก FK และข้อมูลทีม/การลงทะเบียน/แมตช์ยังอยู่ ชุดเต็มบนฐานทดสอบใหม่ผ่าน **125/125** การทดสอบนี้บันทึกช่องว่างเรื่องประวัติ ไม่ได้แปลว่าพฤติกรรมดังกล่าวเป็น requirement ที่ต้องคงไว้; ต้องออกแบบ snapshot ทีม/roster รายทัวร์และนโยบายลบกับเจ้าของ Tournament/DB ก่อนเปลี่ยนโค้ด

ต่อมาเพิ่มกรณีเทียบสถานะ `UPCOMING` กับ `COMPLETED`: หลังลงทะเบียนทีมแล้ว ทั้งสองสถานะยังเปลี่ยนชื่อทีม/ผู้เล่น เพิ่มผู้เล่นใหม่ และลบผู้เล่นผ่าน API ได้ โดยอ่านกลับแล้วชื่อและจำนวนสมาชิกปัจจุบันตรงกับที่แก้; ทดสอบลบทีมขณะ `UPCOMING` ที่ยังไม่มีแมตช์ ได้ 204 และรายการลงทะเบียนหายเหมือนกรณีทัวร์จบแต่ไม่มีแมตช์ เทสไฟล์นี้ผ่าน **6/6** และชุดเต็มบน PostgreSQL ใหม่ผ่าน **128/128** สถานะ `COMPLETED` ในเทสตั้งผ่าน repository โดยตรง ไม่ได้พิสูจน์ขั้นตอนปิดทัวร์ผ่าน HTTP

## ประวัติ roster รายทัวร์ (10 ต.ค. 2026)

ข้อสังเกตจากเทสก่อนหน้านี้ที่ว่าลบทีมแล้ว registration หายและแก้ชื่อ/สมาชิกแล้วข้อมูลเก่าเปลี่ยนตาม เป็นพฤติกรรมเดิมที่ถูกแก้ใน V11 ข้อมูลของทีมและรายชื่อผู้เล่นถูกคัดลอกเมื่อ `TournamentTeamService.addTeam` ลงทะเบียนทีม: `tournament_teams.team_name/team_description/team_logo_url` และ `tournament_team_rosters` เก็บ `player_id`, ชื่อ และ role โดยไม่ผูก FK กับ `players` ที่แก้/ย้าย/ลบได้ การลงสองทัวร์คนละเวลาได้ snapshot คนละชุด

ฝั่ง Team/Player ยังทำ CRUD กับข้อมูลปัจจุบันได้ตามเดิม แต่ `TeamService.delete` ตอบ 409 หากทีมถูกใช้ในทัวร์ เพื่อไม่ให้ประวัติหายผ่าน FK; ฝั่ง TournamentTeam ไม่ให้ถอนทีมหลังสถานะพ้น `UPCOMING` ส่วนหน้าประวัติอ่าน `GET /api/v1/tournaments/{tournamentId}/teams` หรือ `GET /api/v1/tournaments/{tournamentId}/teams/{teamId}/roster` ไม่ใช้ `GET /api/v1/teams/{teamId}/players` ที่เป็นสมาชิกปัจจุบัน หน้าผู้ชม local เชื่อม API snapshot นี้แล้ว ส่วน Admin mock ยังจำลองข้อมูลในหน่วยความจำ

V11 เติมข้อมูล registration ที่มีอยู่จาก **สถานะปัจจุบัน ณ วันที่ migrate** เท่านั้น จึงกู้รายชื่อ/ชื่อทีมที่ถูกแก้หรือลบก่อน V11 ไม่ได้ การแก้ snapshot โดยผู้จัดแบบเปิดสิทธิ์ชั่วคราวยังไม่ได้ทำ และการลบทัวร์ทั้งรายการยังต้องตกลงนโยบายการเก็บประวัติ

ก่อนส่ง PR ให้ตรวจ: Swagger แสดง endpoint ใหม่จริง, status/error ตรงกฎ, `git diff --check`, tests ผ่านบนโค้ดที่ส่ง, README/คู่มือนี้ตรงพฤติกรรมล่าสุด และแจ้งเจ้าของโมดูลที่ใช้ไฟล์ร่วม `.env` เป็นค่า local ไม่ commit และไม่ใส่รหัสผ่านหรือ token ในเอกสาร/PR
