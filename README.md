# How to Survive This Term — ระบบจัดการสายแข่ง E-Sport

ระบบจัดการสายแข่ง E-Sport เป็นเว็บแอปสำหรับผู้จัดการแข่งขันที่บันทึกข้อมูลเกม ทีม ผู้เล่น รายการแข่งขัน และผลการแข่งในที่เดียว
ผู้จัดเลือกทีมเข้ารายการ สร้างตารางแข่งอัตโนมัติตามรูปแบบการแข่ง แล้วบันทึกผลเพื่อให้ระบบเลื่อนผู้ชนะหรือคำนวณคะแนนรวมให้
ผู้ชมเข้ามาติดตามรายการ ตารางแข่ง และผลการแข่งขันได้ ระบบไม่เปิดให้ผู้เล่นหรือทีมสมัครเข้ารายการเอง
รองรับ 2 รูปแบบ ได้แก่ **Single Elimination** (แพ้คัดออก) และ **Points** (สะสมคะแนนจากอันดับและจำนวน kill แบบ Free Fire)
ระบบใช้ Spring Boot, PostgreSQL และ Vue 3

> สถานะ: Backend API ของทีม ผู้เล่น รายการแข่งขัน ตารางแข่ง และผลการแข่งใช้งานได้บน local ฝั่ง Frontend หน้าผู้ชมอ่านข้อมูลจริงจาก API ส่วนหน้า Admin ยังใช้ข้อมูลตัวอย่าง ยังไม่มีระบบ Authentication / สิทธิ์ Admin และยังไม่มี endpoint สำหรับเพิ่มทีมเข้ารายการ ดูงานค้างใน [REMAINING-WORK.md](doc/REMAINING-WORK.md)

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Branch | หน้าที่รับผิดชอบ |
| ---: | --- | --- | --- | --- | --- |
| 1 | นายคณิศร มาประจักษ์ | 673380031-3 | 2 | `Kanisorn-maprajuk_6733800313_02` | Database, Entity, Flyway Migration และ Authentication |
| 2 | นายวัชรพล ดวงกองเงิน | 673380290-9 | 2 | `Vacharapoln-
Doungkongngern_6733802909_02` | Team และ Player Management, Docker, Swagger และ Deployment |
| 3 | นายสิรภัทร ลีล้าน | 673380067-2 | 2 | `Siraphat-leelan_6733800672_02` | TODO: Tournament + TournamentTeam (Chain of Responsibility) หรือ รูปแบบการแข่ง |
| 4 | นายนฤเศรษฐ์ อภิลักขิตพงศ์ | 673380044-4 | 2 | `Naruset-Apilukkitapong-6733800444_02` | TODO: รูปแบบการแข่ง (Strategy) หรือ Tournament + TournamentTeam |
| 5 | นายณัฐกร รุ่งฟ้า | 673380512-7 | 2 | `Nattakron-rungfa_6733805127_02` | Match Result, Free Fire Points (Observer) และ CI |

## Tech Stack

| ส่วน | เทคโนโลยี |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.1, Spring MVC, Bean Validation |
| Build และ ORM | Maven Wrapper, Spring Data JPA / Hibernate |
| Database | PostgreSQL 17, Flyway migrations |
| Frontend | Vue 3, Vue Router, Vite 6 |
| API documentation | springdoc-openapi / Swagger UI |
| Testing | JUnit Jupiter, Mockito, Spring Boot Test (MockMvc), Node.js test runner |
| DevOps | Docker, Docker Compose, GitHub Actions, Railway (Backend + PostgreSQL) |

## System Architecture

Frontend เรียก REST API ผ่าน path `/api/v1` ส่วน Backend แยกชั้นตาม Layered Architecture:

```text
Vue UI → REST Controller → Service → Repository → PostgreSQL
                             ↕
                 Request/Response DTO + Mapper
```

Controller รับ HTTP request และส่งต่อให้ Service โดยไม่เรียก Repository ตรง ๆ Service จัดการ business rules และ transaction ส่วน Repository เข้าถึงข้อมูลผ่าน Spring Data JPA ข้อผิดพลาดทั้งหมดผ่าน `GlobalExceptionHandler` และตอบกลับในรูปแบบ `ApiError` เดียวกัน Flyway สร้างและอัปเดต schema ตอนแอปเริ่มทำงาน

ระบบใช้ Design Pattern 3 แบบ:

| Pattern | ใช้ที่ | หน้าที่ |
| --- | --- | --- |
| Strategy | `FormatStrategy` → `SingleEliminationStrategy`, `PointsStrategy` | สร้างตารางแข่งตามรูปแบบของรายการ `ScheduleServiceImpl` เลือก strategy จาก `TournamentFormat` |
| Chain of Responsibility | `TeamJoinRule` + กฎ 8 ตัวใน `TeamJoinRuleChain` | ตรวจเงื่อนไขก่อนเพิ่มทีมเข้ารายการ เช่น สถานะรายการ เกมตรงกัน จำนวนผู้เล่นขั้นต่ำ |
| Observer | `MatchResultRecordedEvent` → `BracketProgressionListener`, `FreeFireGameRecordedEvent` → `FreeFireCompletionListener` | เมื่อบันทึกผลแล้ว ส่งผู้ชนะไปแมตช์ถัดไป หรือปิดรายการเมื่อเล่นครบทุกเกม |

เอกสารสถาปัตยกรรมเพิ่มเติม: [Class Diagram (Bracket)](doc/diagrams/class-diagram-bracket.md), [Sequence Diagram: บันทึกผล](doc/diagrams/sequence-diagram-result.md), [Sequence Diagram: สร้างตาราง](doc/diagrams/sequence-diagram-schedule.md), [State Diagram](doc/diagrams/state-diagram-match-tournament.md), [Design Patterns](doc/design-patterns.md) และ [SOLID Analysis](doc/solid-analysis.md)

## Database Design (ER Diagram)

Schema หลัง Flyway migration V1–V11 มี 12 ตาราง: `users`, `games`, `teams`, `players`, `tournaments`, `tournament_teams`, `tournament_team_rosters`, `tournament_placement_points`, `matches`, `match_results`, `free_fire_games` และ `free_fire_game_results`

| ความสัมพันธ์ | การใช้งาน |
| --- | --- |
| `games` 1:N `teams` และ `tournaments` | ทีมและรายการแข่งผูกกับเกมที่เล่น |
| `teams` 1:N `players` | ผู้เล่นสังกัดได้ทีละหนึ่งทีม |
| `tournaments` N:M `teams` ผ่าน `tournament_teams` | ทีมที่เข้ารายการ เก็บ snapshot ชื่อ คำอธิบาย และโลโก้ทีม ณ วันที่เข้า |
| `tournament_teams` 1:N `tournament_team_rosters` | snapshot รายชื่อผู้เล่นของทีม ณ วันที่เข้ารายการ |
| `tournaments` 1:N `matches`, `matches` 1:0..1 `match_results` | สายแข่งแบบแพ้คัดออก แต่ละแมตช์ชี้ไป `next_match` |
| `tournaments` 1:N `free_fire_games` 1:N `free_fire_game_results` | ผลแต่ละเกมแบบ Points (อันดับ + kill) |
| `tournaments` 1:N `tournament_placement_points` | ตารางคะแนนตามอันดับของแต่ละรายการ |

รายละเอียดคอลัมน์อยู่ใน [Data Dictionary](doc/data-dictionary.md) ส่วน migration อยู่ใน [db/migration](src/main/resources/db/migration/)

## Installation & Setup

ต้องมี Git และ Docker Desktop (Linux containers) พร้อม Docker Compose ถ้าจะรัน Frontend ต้องมี Node.js 22 ด้วย ถ้ารัน Backend ผ่าน Docker ไม่ต้องติดตั้ง Java หรือ Maven ในเครื่อง หากรันจาก IDE ให้ติดตั้ง JDK 21 (โปรเจกต์มี Maven Wrapper ให้แล้ว)

```bash
git clone https://github.com/nattakron-png/How-to-Survive-This-Term.git
cd How-to-Survive-This-Term
cp .env.example .env
```

บน Windows PowerShell ใช้ `Copy-Item .env.example .env` แทน `cp` คัดลอกเฉพาะครั้งแรก ถ้ามีไฟล์อยู่แล้วให้ใช้ไฟล์เดิม และอย่า commit `.env`

| ตัวแปร | ใช้สำหรับ | ค่า local ใน `.env.example` |
| --- | --- | --- |
| `APP_PORT` | port ของ Backend | `8080` |
| `DB_HOST`, `DB_PORT` | ที่อยู่ PostgreSQL | `localhost`, `5432` |
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | ฐานข้อมูล | `tournament`, `tournament`, `tournament_local` |
| `LOGO_STORAGE_DIR` | โฟลเดอร์เก็บโลโก้ทีม | ค่าเริ่มต้น `uploads/logos` (Docker ใช้ `/app/uploads/logos`) |
| `SHOW_SQL` | แสดง SQL ใน log | ค่าเริ่มต้น `false` |
| `VITE_API_PROXY_TARGET` | Backend ที่ Vite proxy ไป | ค่าเริ่มต้น `http://127.0.0.1:8080` |

ดูค่าอื่นได้ใน [application.properties](src/main/resources/application.properties) และ [compose.yaml](compose.yaml)

## How to Run

จาก root ของ repository เปิด Backend และ PostgreSQL ด้วย Docker Compose:

```bash
docker compose up -d --build --wait
```

Backend อยู่ที่ `http://localhost:8080` ตรวจได้ที่ <http://localhost:8080/actuator/health> ควรได้ `{"status":"UP"}` Flyway จะรัน migration ก่อนแอปรับ request หลังแก้โค้ดให้ rebuild ด้วย `docker compose up -d --build --wait app` ดู log ด้วย `docker compose logs -f app` และหยุดด้วย `docker compose down` โดย volume ของ PostgreSQL และโลโก้ (`postgres_data`, `logo_data`) ยังอยู่

หากต้องการรัน Backend จาก IDE ให้เปิดเฉพาะฐานข้อมูลด้วย `docker compose up -d db` แล้วรัน `./mvnw spring-boot:run` (Windows PowerShell ใช้ `.\mvnw.cmd spring-boot:run`)

เปิดอีก terminal แล้วรัน Frontend:

```bash
cd frontend
npm install
npm run dev
```

เปิด `http://localhost:5173` Vite จะ proxy `/api` ไป Backend local หน้าผู้ชม (`/`, `/tournaments`, หน้ารายละเอียดรายการและแมตช์) อ่านข้อมูลจริง ส่วนหน้า Admin ยังใช้ข้อมูลตัวอย่างใน `frontend/src/mock/`

### ข้อมูลตัวอย่างสำหรับ local

หลังเปิด `db` และ Flyway สร้างตารางถึง V11 แล้ว รันจาก root ใน PowerShell:

```powershell
docker run --rm -v "${PWD}:/work" -w /work node:22-alpine node scripts/generate-local-mock-seed.mjs target/mock-data.local.sql
docker cp target/mock-data.local.sql tournament-db-1:/tmp/mock-data.local.sql
docker compose exec -T db psql -U tournament -d tournament -v ON_ERROR_STOP=1 -f /tmp/mock-data.local.sql
```

สคริปต์แปลงข้อมูลจาก `frontend/src/mock/` เป็น SQL สำหรับฐาน local เท่านั้น และหยุดทำงานหากตารางมีข้อมูลอื่นอยู่แล้ว ดูเงื่อนไขใน [scripts/README.md](scripts/README.md)

## API Documentation

เปิด [Swagger UI](http://localhost:8080/swagger-ui.html) เพื่อดูและทดลอง API หรือดู OpenAPI JSON ที่ `http://localhost:8080/v3/api-docs`

| ส่วน | Endpoint หลัก |
| --- | --- |
| Teams | `/api/v1/teams`, `/api/v1/teams/{id}` |
| Team Players | `GET /api/v1/teams/{teamId}/players`, `PUT/DELETE /api/v1/teams/{teamId}/players/{playerId}` |
| Team Logo | `PUT /api/v1/teams/{teamId}/logo` (multipart), `GET /api/v1/logos/{filename}` |
| Players | `/api/v1/players`, `/api/v1/players/{id}` |
| Tournaments | `/api/v1/tournaments`, `/api/v1/tournaments/{id}`, `GET /api/v1/tournaments/{id}/placement-points` |
| Tournament Teams | `GET /api/v1/tournaments/{tournamentId}/teams`, `GET /api/v1/tournaments/{tournamentId}/teams/{teamId}/roster` |
| Schedule | `POST /api/v1/tournaments/{tournamentId}/schedule`, `GET /api/v1/tournaments/{tournamentId}/matches` |
| Matches | `GET /api/v1/matches`, `GET /api/v1/matches/{id}` |
| Match Result | `POST/GET /api/v1/matches/{matchId}/result` |
| Free Fire | `POST/GET /api/v1/free-fire-games/{gameId}/results`, `GET /api/v1/tournaments/{tournamentId}/free-fire-games`, `GET /api/v1/tournaments/{tournamentId}/standings` |

รายการแบบแบ่งหน้าตอบกลับเป็น `PageResponse` ข้อผิดพลาดตอบเป็น `ApiError` พร้อม status 400 (validation), 404 (ไม่พบข้อมูล) หรือ 409 (ขัดกับกฎธุรกิจ) โลโก้ทีมรับ PNG/JPEG ไม่เกิน 2 MB รายละเอียดเพิ่มเติมอยู่ใน [Match Result API](doc/match-result-api.md), [Free Fire Result API](doc/free-fire-result-api.md) และ [คู่มือโมดูลทีมและผู้เล่น](doc/team-player-module-guide.md)

ยังไม่มี Authentication ทุก endpoint จึงเรียกได้โดยไม่ต้องล็อกอิน ห้ามเปิด URL ที่เขียนข้อมูลได้สู่สาธารณะจนกว่าระบบสิทธิ์ Admin จะเสร็จ

## How to Run Tests

Backend ผ่าน Docker (ใช้ฐานข้อมูลทดสอบ `test-db` แยกจากฐานพัฒนา):

```bash
docker compose --profile test run --rm --build tests
docker compose --profile test stop test-db
```

หรือรันในเครื่องเมื่อมี PostgreSQL สำหรับทดสอบอยู่แล้ว:

```bash
./mvnw verify
```

Windows ใช้ `.\mvnw.cmd verify` ผล JUnit อยู่ใน `target/surefire-reports/`

Frontend (จาก `frontend`):

```bash
node --test test/
npm run build
```

ชุดทดสอบ Backend มี 30 ไฟล์ ทั้ง Unit test ด้วย Mockito (Service, Strategy, กฎทั้ง 8 ตัวของ Chain, Listener, PointsCalculator) และ Integration test ด้วย MockMvc กับ PostgreSQL จริง (เช่น `ScheduleResultFlowTests`, `TeamPlayerTournamentFlowTests`) GitHub Actions ([ci.yml](.github/workflows/ci.yml)) รัน `./mvnw -B verify` กับ PostgreSQL 17 ทุกครั้งที่ push หรือเปิด PR เข้า `develop`/`main` และเก็บรายงานเทสต์เป็น artifact

## Deployment URL

| จุดเข้าใช้งาน | URL |
| --- | --- |
| Backend (Railway) | TODO |
| Health check | TODO `/actuator/health` |
| API Documentation | TODO `/swagger-ui.html` |
| Frontend | ยังไม่ deploy |

Backend deploy บน Railway ด้วย `Dockerfile` ที่ root และใช้ Railway PostgreSQL ตั้ง `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` และ `DB_PASSWORD` เป็น Reference Variable ไปยัง service PostgreSQL และตั้ง Healthcheck Path เป็น `/actuator/health` ขั้นตอนทั้งหมดอยู่ใน [deployment-prep.md](doc/deployment-prep.md)

รุ่นที่ deploy และตรวจ health แล้วคือ migration V10 ส่วน V11 และ Frontend ยังไม่ได้ deploy ควรตรวจ URL ซ้ำก่อนส่งงานและก่อนนำเสนอ

## Project Structure

```text
How-to-Survive-This-Term/
├── .github/workflows/ci.yml      # Build + test ด้วย GitHub Actions
├── src/
│   ├── main/java/com/example/tournament/
│   │   ├── controller/api/       # REST endpoints
│   │   ├── service/              # Service interfaces
│   │   │   ├── impl/             # Business logic
│   │   │   ├── format/           # Strategy: รูปแบบการแข่ง
│   │   │   ├── rule/             # Chain of Responsibility: กฎเข้ารายการ
│   │   │   ├── freefire/         # คำนวณคะแนน Free Fire
│   │   │   └── storage/          # เก็บไฟล์โลโก้
│   │   ├── event/                # Observer: Event และ Listener
│   │   ├── repository/           # Spring Data JPA
│   │   ├── domain/entity/        # JPA Entity
│   │   ├── domain/enums/         # TournamentFormat, TournamentStatus, MatchStatus
│   │   ├── dto/request/          # Request contracts
│   │   ├── dto/response/         # Response contracts
│   │   ├── mapper/               # Entity ↔ DTO
│   │   └── exception/            # GlobalExceptionHandler, ApiError
│   ├── main/resources/db/migration/  # Flyway V1–V11
│   └── test/                     # Unit และ Integration tests
├── frontend/                     # Vue 3 + Vite
│   ├── src/views/                # หน้าผู้ชมและ admin/
│   ├── src/api/, src/mock/       # เรียก API จริง / ข้อมูลตัวอย่าง
│   └── test/                     # Node.js tests
├── scripts/                      # สร้างข้อมูลตัวอย่างสำหรับ local
├── doc/                          # เอกสารโมดูล, Design Patterns, SOLID
│   └── diagrams/                 # Use Case, Class, Sequence, State Diagram
├── Dockerfile
├── compose.yaml
├── .env.example
├── pom.xml
└── README.md
```

## Git Workflow

`main` ใช้สำหรับรุ่นส่งมอบ ส่วน `develop` ใช้รวมงาน แต่ละคนทำงานใน branch ของตัวเอง แล้วเปิด Pull Request เข้า `develop` พร้อม reviewer อย่างน้อย 1 คน และต้องผ่าน CI (`build-and-test`) ก่อน merge ก่อนเริ่มงานทุกครั้งให้ `git pull origin develop` เข้า branch ตัวเอง ตั้ง commit message แบบ `<type>: <สิ่งที่ทำ>` เช่น `feat: add schedule API` หรือ `docs: update README`

รูปแบบชื่อ branch ต้องตรงกับที่อาจารย์กำหนด (ผิดรูปแบบหักคนละ 5 คะแนน) ทีมควรยืนยันรูปแบบก่อนส่งงาน ตาม [REMAINING-WORK.md](doc/REMAINING-WORK.md)
