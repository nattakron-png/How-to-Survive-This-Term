# Deployment Diagram

ระบบรันได้ 3 สภาพแวดล้อม: เครื่องนักพัฒนาด้วย Docker Compose, CI บน GitHub Actions และ Production บน Railway

## 1. เครื่องนักพัฒนา (Docker Compose)

ตาม `compose.yaml` และ `Dockerfile` พอร์ตทั้งหมด bind แค่ `127.0.0.1`

```mermaid
flowchart TB
    subgraph Dev["💻 เครื่องนักพัฒนา (Windows / macOS + Docker Desktop)"]
        Browser["Web Browser"]
        Vite["Vite Dev Server :5173<br/>«node» frontend/<br/>proxy /api → :8080"]

        subgraph Compose["Docker Compose project: tournament"]
            subgraph AppC["«container» app<br/>eclipse-temurin:21-jre-jammy<br/>user: app"]
                Jar["«artifact»<br/>app.jar<br/>(tournament-0.0.1-SNAPSHOT)"]
            end
            subgraph DbC["«container» db<br/>postgres:17"]
                PG[("database: tournament")]
            end
            VolPg[("«volume»<br/>postgres_data")]
            VolLogo[("«volume»<br/>logo_data")]

            subgraph TestProfile["profile: test (รันเมื่อสั่ง --profile test)"]
                Tests["«container» tests<br/>maven:3.9-temurin-21<br/>mvn -B test"]
                TestDb[("«container» test-db<br/>postgres:17 บน tmpfs")]
            end
        end
    end

    Browser -->|"HTTP :5173"| Vite
    Vite -->|"HTTP 127.0.0.1:8080"| Jar
    Browser -->|"HTTP 127.0.0.1:8080<br/>/swagger-ui.html"| Jar
    Jar -->|"JDBC db:5432"| PG
    PG --- VolPg
    Jar -->|"/app/uploads/logos"| VolLogo
    Tests -->|"JDBC test-db:5432"| TestDb
```

| Node | Image / Runtime | พอร์ต | ข้อมูลถาวร |
| --- | --- | --- | --- |
| `app` | build จาก `Dockerfile` target `runtime` (JRE 21) | `127.0.0.1:${APP_PORT:-8080}` | `logo_data` → `/app/uploads/logos` |
| `db` | `postgres:17` | `127.0.0.1:${DB_PORT:-5432}` | `postgres_data` |
| `tests` | `Dockerfile` target `development` (Maven + JDK 21) | – | – |
| `test-db` | `postgres:17` | – | `tmpfs` (หายเมื่อหยุด) |
| Vite | Node.js บนเครื่อง (`npm run dev`) | `5173` | – |

`app` รอจน `db` ผ่าน healthcheck (`pg_isready`) ก่อนเริ่ม และ healthcheck ของ `app` เรียก `/actuator/health`

## 2. CI (GitHub Actions)

```mermaid
flowchart LR
    Dev["นักพัฒนา"] -->|"push / PR เข้า develop, main"| GH["GitHub Repository"]
    GH --> Runner

    subgraph Runner["«execution environment» ubuntu-latest runner"]
        JDK["Temurin JDK 21<br/>./mvnw -B verify"]
        CIdb[("«service» postgres:17<br/>tournament_test :5432")]
        JDK -->|"JDBC localhost:5432"| CIdb
    end

    Runner -->|"upload-artifact"| Report["«artifact»<br/>test-reports<br/>(target/surefire-reports)"]
```

## 3. Production (Railway)

ตาม `doc/deployment-prep.md` backend รุ่น V10 deploy และผ่าน health check แล้ว ส่วน V11 และ frontend ยังไม่ได้ deploy

```mermaid
flowchart TB
    User["👤 ผู้ชม / ผู้จัด<br/>Web Browser"]

    subgraph Railway["☁️ Railway Project"]
        subgraph AppSvc["«service» app<br/>build จาก Dockerfile ใน GitHub repo"]
            Jar2["«artifact» app.jar<br/>Java 21, port = $PORT"]
        end
        subgraph DbSvc["«service» Postgres<br/>Railway PostgreSQL"]
            PG2[("database")]
        end
        Private{{"private network"}}
    end

    GH2["GitHub Repository<br/>(branch ที่เลือก deploy)"] -->|"auto build + deploy"| AppSvc
    User -->|"HTTPS public domain"| Jar2
    Jar2 --- Private
    Private -->|"JDBC<br/>(DB_* จาก Reference Variable)"| PG2
    Railway -.->|"healthcheck /actuator/health"| Jar2
```

| Environment Variable ของ `app` | ค่าบน Railway |
| --- | --- |
| `DB_HOST` | `${{Postgres.PGHOST}}` |
| `DB_PORT` | `${{Postgres.PGPORT}}` |
| `DB_NAME` | `${{Postgres.PGDATABASE}}` |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `PORT` | Railway กำหนดให้เอง (อย่าตั้ง `APP_PORT` ทับ) |

## ข้อควรระวังก่อนเปิดใช้งานจริง

- **โลโก้บน Railway:** ตอนนี้เขียนไฟล์ลงดิสก์ของ container ไฟล์จะหายเมื่อ redeploy จนกว่าจะเลือก Railway Volume หรือ object storage
- **Auth:** API ที่เขียนข้อมูลยังไม่มีการป้องกัน ต้องทำ Spring Security ก่อนเผยแพร่ URL
- **Frontend:** ยังไม่มี node สำหรับ deploy frontend ต้องตัดสินใจว่าจะ build เป็นไฟล์ static แล้วให้ Spring Boot เสิร์ฟ หรือแยก service
