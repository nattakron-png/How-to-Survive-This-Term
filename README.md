# HowToSurviveThisTerm — ระบบจัดการสายแข่ง E-Sport
https://frontend-production-c6ca.up.railway.app/
ระบบสำหรับจัดและติดตามการแข่งขัน E-Sport ระดับมหาวิทยาลัย รองรับ ROV, Valorant, Fighting Game และ Free Fire
ผู้จัดเพิ่มเกม ทีม ผู้เล่น และรายการแข่งผ่านหลังบ้าน จากนั้นให้ระบบสร้างตารางแข่งและบันทึกผล
ผู้ชมเข้าดูรายการ สายแข่ง ตารางคะแนน และผลการแข่งได้โดยไม่ต้องเข้าสู่ระบบ
รายการแข่งมี 2 รูปแบบ คือ **แพ้คัดออก (Single Elimination)** และ **เก็บคะแนนหลายเกม (Points)** สำหรับ Free Fire

> **สถานะ:** Backend ทำ API หลักเสร็จแล้ว ได้แก่ รายการแข่ง ทีม ผู้เล่น ตารางแข่ง ผลแข่ง และตารางคะแนน แต่ยังไม่มีระบบยืนยันตัวตน (Spring Security) หน้าผู้ชมและหน้าหลังบ้านเรียก API จริงแล้ว ยกเว้นการ login ของผู้ดูแลที่ยังเป็นข้อมูลจำลอง ดูงานที่เหลือได้ใน [REMAINING-WORK.md](doc/REMAINING-WORK.md)

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Branch | หน้าที่รับผิดชอบ |
| ---: | --- | --- | --- | --- | --- |
| 1 | นายคณิศร มาประจักษ์ | 673380031-3 | 2 | `Kanisorn-maprajuk_6733800313_02` | ฐานข้อมูล, Entity, Auth |
| 2 | นายนฤเศรษฐ์ อภิลักขิตพงศ์ | 673380044-4 | 2 | `Naruset-Apilukkitapong-6733800444_02` | `<โมดูล>` |
| 3 | นายสิรภัทร ลีล้าน | 673380067-2 | 2 | `Siraphat-leelan_6733800672_02` | `<โมดูล>` |
| 4 | นายณัฐกร รุ่งฟ้า | 673380512-7 | 2 | `Nattakron-rungfa_6733805127_02` | ผลการแข่ง (แพ้คัดออก + Free Fire), ตารางคะแนน, Observer Pattern, CI, เชื่อมหน้าหลังบ้านกับ API |
| 5 | นายวัชรพล ดวงกองเงิน | 673380290-9 | 2 | `Vacharapoln-Doungkongngern_6733802909_02` | Team / Player API, Docker, Deploy |

## Tech Stack

| ส่วน | เทคโนโลยี |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1.1, Spring Web MVC, Bean Validation |
| ORM และ Build | Spring Data JPA / Hibernate, Maven Wrapper |
| Database | PostgreSQL 17, Flyway (migration V1–V12) |
| Frontend | Vue 3, Vue Router 4, Vite 6 |
| API Documentation | springdoc-openapi 3.1.1 (Swagger UI) |
| Testing | JUnit 5, Mockito, Spring Boot Test, Node.js test runner |
| DevOps | Docker, Docker Compose, GitHub Actions |
| Deployment | Railway (Spring Boot + Railway PostgreSQL) |

## System Architecture

Backend แบ่งเป็นชั้นตามแพ็กเกจใน `code/backend/src/main/java/com/example/tournament/`

```text
Vue (Browser) ──HTTP /api/v1──▶ Controller ─▶ Service ─▶ Repository ─▶ PostgreSQL
                                     │            │
                               DTO + Mapper    Event Listener
```

- **Controller** รับ request และตรวจ DTO ด้วย `@Valid` แล้วส่งต่อให้ Service ไม่เรียก Repository เอง
- **Service** เก็บ business rule และ transaction ทั้งหมด exception ที่โยนออกมาจะถูก `GlobalExceptionHandler` แปลงเป็น 400 / 404 / 409
- **Design Pattern ที่ใช้**
  - **Strategy:** เลือกวิธีสร้างตารางแข่งตามรูปแบบรายการ
  - **Chain of Responsibility:** ตรวจ 8 เงื่อนไขก่อนเพิ่มทีมเข้ารายการ
  - **Observer:** หลังบันทึกผล ระบบส่งผู้ชนะไปแมตช์ถัดไป และปิดรายการเมื่อแข่งจบ

ดูแผนภาพทั้งหมดได้ที่ [doc/diagrams/](doc/diagrams/README.md) เช่น [Class Diagram](doc/diagrams/class-diagram.md), [Component Diagram](doc/diagrams/component.md), [Deployment Diagram](doc/diagrams/deployment-diagram.md)

## Database Design (ER Diagram)

หลังรัน migration ครบ ฐานข้อมูลมี 13 ตาราง แบ่งเป็น 3 กลุ่ม

| กลุ่ม | ตาราง |
| --- | --- |
| ข้อมูลหลัก | `users`, `games`, `teams`, `players`, `tournaments` |
| การเข้าร่วมรายการ | `tournament_teams`, `tournament_team_rosters` |
| ตารางแข่งและผล | `matches`, `match_results`, `free_fire_games`, `free_fire_game_results`, `tournament_placement_points` |

| ความสัมพันธ์ | ความหมาย |
| --- | --- |
| `games` 1:N `teams`, `tournaments` | ทีมและรายการแข่งผูกกับเกมเดียว |
| `teams` 0..1:N `players` | ผู้เล่นอยู่ได้ทีละไม่เกิน 1 ทีม หรือยังไม่มีทีมก็ได้ |
| `tournaments` N:M `teams` ผ่าน `tournament_teams` | ระบบเก็บชื่อทีมและรายชื่อผู้เล่น ณ วันที่เข้ารายการไว้ ถ้าแก้ข้อมูลทีมภายหลัง ประวัติเดิมก็ไม่เปลี่ยน |
| `matches` → `matches` (`next_match_id`) | ผูกสายแพ้คัดออก ผู้ชนะจะถูกส่งไปแมตช์ถัดไป |
| `free_fire_games` 1:N `free_fire_game_results` | ผลอันดับและจำนวน kill ของทุกทีมในแต่ละเกม |

ระบบไม่เก็บคะแนน Free Fire ลงฐานข้อมูล แต่คำนวณใหม่จากอันดับและจำนวน kill ทุกครั้งที่เรียกดู รายละเอียดอยู่ใน [ER Diagram](doc/diagrams/er-diagram.md), [Data Dictionary](doc/data-dictionary.md) และ [db/migration](code/backend/src/main/resources/db/migration/)

## Installation & Setup

สิ่งที่ต้องมี
- Git
- Docker Desktop (โหมด Linux containers)
- Node.js 22 สำหรับ Frontend
- JDK 21 เฉพาะกรณีรัน Backend จาก IDE (มี Maven Wrapper ให้แล้ว ไม่ต้องลง Maven เพิ่ม)

```bash
git clone <URL ของ repository>
cd How-to-Survive-This-Term
cp .env.example .env
```

บน Windows PowerShell ให้ใช้ `Copy-Item .env.example .env` แทน `cp` และคัดลอกเฉพาะครั้งแรก ห้าม commit ไฟล์ `.env`

| ตัวแปร | ใช้ทำอะไร | ค่าเริ่มต้น |
| --- | --- | --- |
| `APP_PORT` | พอร์ตของ Backend | `8080` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | ที่อยู่ฐานข้อมูล | `localhost`, `5432`, `tournament` |
| `DB_USERNAME`, `DB_PASSWORD` | บัญชีฐานข้อมูล | `tournament`, `tournament_local` |
| `LOGO_STORAGE_DIR` | โฟลเดอร์เก็บโลโก้ทีม | `uploads/logos` |
| `VITE_API_PROXY_TARGET` | Backend ที่ Vite proxy `/api` ไปหา | `http://127.0.0.1:8080` |

## How to Run

**Backend + PostgreSQL** (รันจาก root ของ repo)

```bash
docker compose up -d --build --wait
```

- ตรวจว่า Backend พร้อมที่ <http://localhost:8080/actuator/health> ต้องได้ `{"status":"UP"}`
- Flyway จะสร้างตารางให้เองตอนแอปเริ่ม
- หลังแก้โค้ด ให้สั่ง `docker compose up -d --build --wait app`
- หยุดด้วย `docker compose down` ข้อมูลในฐานข้อมูลและโลโก้ยังอยู่ใน volume

**Frontend** (เปิดอีก terminal)

```bash
cd code/frontend
npm install
npm run dev
```

เปิด <http://localhost:5173> แล้ว Vite จะส่งต่อ request ที่ขึ้นต้นด้วย `/api` ไปที่ Backend ให้เอง

### ข้อมูลตัวอย่าง

ถ้าต้องการให้ฐานข้อมูลในเครื่องมีรายการ ทีม แมตช์ และผล Free Fire ไว้ทดลอง ให้ใช้สคริปต์ใน [code/scripts/](code/scripts/README.md) สคริปต์นี้แปลงข้อมูลใน `code/frontend/src/mock/` เป็นไฟล์ SQL ใช้ได้กับฐานข้อมูลในเครื่องเท่านั้น และไม่ถูกส่งขึ้น Railway

## API Documentation

เมื่อ Backend รันอยู่ เปิด [Swagger UI](http://localhost:8080/swagger-ui.html) เพื่อดูและทดลองเรียก API ส่วน OpenAPI JSON อยู่ที่ `/v3/api-docs` ทุก endpoint ขึ้นต้นด้วย `/api/v1`

| ส่วน | Endpoint หลัก |
| --- | --- |
| Tournaments | `GET/POST /tournaments`, `GET/PUT/DELETE /tournaments/{id}`, `GET /tournaments/{id}/placement-points` |
| Tournament Teams | `GET/POST /tournaments/{id}/teams`, `DELETE /tournaments/{id}/teams/{teamId}`, `GET /tournaments/{id}/teams/{teamId}/roster` |
| Teams | `GET/POST /teams`, `GET/PUT/DELETE /teams/{id}`, `PUT /teams/{id}/logo` |
| Players | `GET/POST /players`, `GET/PUT/DELETE /players/{id}`, `GET /teams/{id}/players`, `PUT/DELETE /teams/{id}/players/{playerId}` |
| Schedule และ Matches | `POST /tournaments/{id}/schedule`, `GET /tournaments/{id}/matches`, `GET /matches`, `GET /matches/{id}` |
| ผลแพ้คัดออก | `POST/GET /matches/{id}/result` |
| ผล Free Fire | `GET /tournaments/{id}/free-fire-games`, `POST/GET /free-fire-games/{id}/results`, `GET /tournaments/{id}/standings` |
| Logo | `GET /logos/{filename}` |

- **โลโก้ทีม:** อัปโหลดเป็น `multipart/form-data` ในฟิลด์ `file` รับเฉพาะ PNG / JPEG ขนาดไม่เกิน 2 MB
- **Error:** ทุก error ตอบกลับในรูปแบบ `ApiError` เดียวกัน
- **Auth:** ยังไม่มี ตอนนี้ API ที่เขียนข้อมูลเรียกได้โดยไม่ต้องเข้าสู่ระบบ

รายละเอียดเพิ่มเติมอยู่ใน [match-result-api.md](doc/match-result-api.md) และ [free-fire-result-api.md](doc/free-fire-result-api.md)

## How to Run Tests

**Backend** (ใช้ PostgreSQL ใน Docker ไม่ต้องลง Java ในเครื่อง)

```bash
docker compose --profile test run --rm tests
```

ถ้าลง JDK 21 แล้ว และมี PostgreSQL สำหรับเทสต์ ก็รันตรงได้ด้วย `./mvnw verify` ในโฟลเดอร์ `code/backend` (บน Windows ใช้ `.\mvnw.cmd verify`) โค้ดเทสต์อยู่ใน `test/backend/java/` และผลเทสต์อยู่ใน `code/backend/target/surefire-reports/`

**Frontend**

```bash
cd code/frontend
node --test ../../test/frontend/*.test.js
npm run build
```

**CI:** GitHub Actions มี 2 job คือ backend รัน `./mvnw -B verify` กับ PostgreSQL 17 และ frontend รัน `npm run build` กับเทสต์ใน `test/frontend/` ทุกครั้งที่ push หรือเปิด PR เข้า `develop` / `main` และเก็บรายงานเทสต์ไว้เป็น artifact ชื่อ `test-reports`

## Deployment URL

| จุดเข้าใช้งาน | URL |
| --- | --- |
| Backend (Railway) | `<https://....up.railway.app>` |
| Health Check | `<URL>/actuator/health` |
| Swagger UI | `<URL>/swagger-ui.html` |
| Frontend | ยังไม่ได้ deploy |

- **Backend:** Railway build จาก `code/backend/Dockerfile` (กำหนดใน `railway.json`) และต่อกับ Railway PostgreSQL ผ่าน private network
- **ตัวแปรฐานข้อมูล:** ตั้ง `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` เป็น Reference Variable ไปที่ service Postgres
- **Healthcheck Path:** ตั้งเป็น `/actuator/health`
- **ที่ deploy แล้ว:** Backend รุ่น migration V10 เท่านั้น ยังไม่ได้ deploy V11 และ Frontend
- **ก่อนเปิดใช้งานจริง:** ต้องทำ Auth และย้ายที่เก็บโลโก้ไปที่ถาวร

ขั้นตอนทั้งหมดอยู่ใน [deployment-prep.md](doc/deployment-prep.md)

## Project Structure

โครงสร้างตามใบงานข้อ 9: `code/`, `test/`, `doc/`, `img/`

```text
How-to-Survive-This-Term/
├── code/                              # Source code + Configuration
│   ├── backend/                       # Spring Boot (pom.xml, mvnw, Dockerfile)
│   │   └── src/main/
│   │       ├── java/com/example/tournament/
│   │       │   ├── controller/api/    # REST endpoints
│   │       │   ├── service/           # Interface + business logic
│   │       │   │   ├── impl/
│   │       │   │   ├── format/        # Strategy: แพ้คัดออก / เก็บคะแนน
│   │       │   │   ├── rule/          # Chain of Responsibility: กฎเพิ่มทีม
│   │       │   │   ├── freefire/      # คำนวณคะแนน Free Fire
│   │       │   │   └── storage/       # เก็บไฟล์โลโก้
│   │       │   ├── event/             # Observer: Listener หลังบันทึกผล
│   │       │   ├── repository/        # Spring Data JPA
│   │       │   ├── domain/entity, enums/
│   │       │   ├── dto/request, response/
│   │       │   ├── mapper/
│   │       │   └── exception/         # GlobalExceptionHandler, ApiError
│   │       └── resources/db/migration/  # Flyway V1–V12
│   ├── frontend/                      # Vue 3 + Vite
│   │   └── src/ views/, components/, api/, mock/, router/, stores/
│   └── scripts/                       # สร้างข้อมูลตัวอย่างสำหรับ local
├── test/                              # การทดสอบทั้งหมด
│   ├── backend/java/                  # Unit + Integration tests (JUnit, Mockito, Spring Boot Test)
│   └── frontend/                      # node:test
├── doc/                               # เอกสารโมดูล, design patterns, SOLID
│   ├── diagrams/                      # Diagram ทั้งหมด
│   └── slide/                         # สไลด์นำเสนอ
├── img/                               # ไฟล์มัลติมีเดีย
├── .github/workflows/ci.yml           # Build + test ทุก PR
├── compose.yaml                       # Docker Compose (db, app, tests)
├── railway.json                       # ตั้งค่า deploy บน Railway
└── README.md
```

## Git Workflow

| Branch | ใช้ทำอะไร |
| --- | --- |
| `main` | รุ่นที่ส่งงานและ deploy |
| `develop` | รวมงานของทุกคน |
| `<branch ส่วนตัว>` | แต่ละคนทำงานของตัวเองใน branch นี้ |

ขั้นตอนการทำงาน
1. `git pull origin develop` ก่อนเริ่มงานทุกครั้ง
2. ทำงานใน branch ส่วนตัว
3. เปิด Pull Request เข้า `develop` ซึ่ง CI ต้องผ่านก่อนรวมงาน
4. เมื่อพร้อมส่งงาน ค่อยรวม `develop` เข้า `main`

ตั้งชื่อ commit แบบ `<type>: <สิ่งที่ทำ>` เช่น `feat: add free fire standings API` หรือ `docs: add ER diagram`
