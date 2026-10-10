# Component Diagram: ส่วนประกอบของระบบ

ระบบแบ่งเป็น 3 ส่วนใหญ่: Frontend (Vue 3 + Vite), Backend (Spring Boot 4, Java 21) และ PostgreSQL ภายใน Backend แบ่งชั้นเป็น Controller → Service → Repository ตามแพ็กเกจใน `code/backend/src/main/java/com/example/tournament/`

```mermaid
flowchart TB
    Browser["🌐 Web Browser"]

    subgraph FE["Frontend: frontend/ (Vue 3 + Vue Router + Vite)"]
        direction TB
        PublicViews["หน้าผู้ชม<br/>views/*View.vue"]
        AdminViews["หน้าหลังบ้าน<br/>views/admin/*"]
        Components["UI Components<br/>components/*"]
        ApiClient["API Client<br/>api/public.js, api/tournaments.js"]
        Mock["Mock Data<br/>mock/*"]
        AuthStore["Auth Store<br/>stores/auth.js (sessionStorage)"]
        PublicViews --> Components
        AdminViews --> Components
        PublicViews --> ApiClient
        AdminViews -.->|"ยังใช้ข้อมูลจำลอง"| Mock
        AdminViews --> AuthStore
    end

    subgraph BE["Backend: Spring Boot (tournament.jar)"]
        direction TB

        subgraph Web["controller/api (REST /api/v1)"]
            TournamentAPI["Tournament API<br/>TournamentController<br/>TournamentRosterController"]
            TeamAPI["Team / Player API<br/>TeamController, PlayerController<br/>TeamMembershipController"]
            LogoAPI["Logo API<br/>TeamLogoController, LogoController"]
            ScheduleAPI["Schedule / Match API<br/>ScheduleController, MatchController"]
            ResultAPI["Result API<br/>MatchResultController<br/>FreeFireResultController"]
        end

        ErrorHandler["exception<br/>GlobalExceptionHandler → ApiError"]

        subgraph Svc["service"]
            TournamentSvc["Tournament Module<br/>TournamentService<br/>TournamentTeamService<br/>TournamentRosterSnapshotService"]
            Rules["service/rule<br/>TeamJoinRuleChain (8 กฎ)"]
            TeamSvc["Team / Player Module<br/>TeamService, PlayerService<br/>TeamMembershipService, TeamLogoService"]
            Storage["service/storage<br/>FileStorageService"]
            ScheduleSvc["Schedule Module<br/>ScheduleService, MatchService"]
            Format["service/format<br/>FormatStrategy<br/>SingleElimination / Points"]
            ResultSvc["Result Module<br/>MatchResultService<br/>FreeFireResultService"]
            Calc["service/freefire<br/>PointsCalculator"]
        end

        Events["event<br/>BracketProgressionListener<br/>FreeFireCompletionListener"]
        Mapper["mapper + dto"]
        Repo["repository<br/>Spring Data JPA"]
        Entity["domain/entity + enums"]
        Flyway["Flyway<br/>db/migration V1–V11"]
        Actuator["Actuator<br/>/actuator/health"]
        Swagger["springdoc<br/>/swagger-ui.html"]

        TournamentAPI --> TournamentSvc
        TeamAPI --> TeamSvc
        LogoAPI --> TeamSvc
        LogoAPI --> Storage
        ScheduleAPI --> ScheduleSvc
        ResultAPI --> ResultSvc

        TournamentSvc --> Rules
        TeamSvc --> Storage
        ScheduleSvc --> Format
        ResultSvc --> Calc
        ResultSvc -->|"publishEvent"| Events

        Svc --> Mapper
        Svc --> Repo
        Events --> Repo
        Repo --> Entity
        Web -.->|"exception"| ErrorHandler
    end

    DB[("PostgreSQL 17")]
    Files[("Logo Files<br/>uploads/logos")]

    Browser --> FE
    ApiClient -->|"HTTP JSON /api/v1/*<br/>(Vite proxy ตอน dev)"| Web
    Browser -->|"&lt;img src=logoUrl&gt;"| LogoAPI
    Repo -->|"JDBC"| DB
    TournamentSvc -->|"JdbcTemplate (roster snapshot)"| DB
    Flyway -->|"migrate ตอนเริ่มแอป"| DB
    Storage --> Files
```

## หน้าที่ของแต่ละ Component

| Component | หน้าที่ | ตำแหน่ง |
| --- | --- | --- |
| หน้าผู้ชม | หน้าแรก, รายการแข่ง, รายละเอียดรายการ (สาย / ตารางคะแนน), รายละเอียดแมตช์ | `code/frontend/src/views/` |
| หน้าหลังบ้าน | จัดการรายการ ทีม ผู้เล่น แมตช์ และผล Free Fire | `code/frontend/src/views/admin/` |
| API Client | เรียก `/api/v1/*` แล้วแปลงข้อมูลให้หน้าเว็บใช้ | `code/frontend/src/api/` |
| REST Controller | รับ HTTP request, ตรวจ DTO ด้วย `@Valid`, เรียก Service | `controller/api/` |
| Service | Business logic และ transaction | `service/`, `service/impl/` |
| Rule Chain | ตรวจ 8 เงื่อนไขก่อนเพิ่มทีมเข้ารายการ | `service/rule/` |
| Format Strategy | สร้างสายแพ้คัดออก หรือสร้างเกม Free Fire ตามรูปแบบรายการ | `service/format/` |
| Points Calculator | คำนวณคะแนนต่อเกมและตารางคะแนนรวม | `service/freefire/` |
| Event Listener | งานหลังบันทึกผล: ส่งผู้ชนะต่อ, ปิดรายการ | `event/` |
| File Storage | เก็บและอ่านไฟล์โลโก้ | `service/storage/` |
| Repository | เข้าถึงฐานข้อมูลผ่าน JPA | `repository/` |
| GlobalExceptionHandler | แปลง exception เป็น 400 / 404 / 409 รูปแบบ `ApiError` | `exception/` |
| Flyway | สร้างและอัปเดต schema ตอนแอปเริ่ม (`ddl-auto=validate`) | `code/backend/src/main/resources/db/migration/` |

## Interface ระหว่าง Component

| ผู้เรียก | ผู้ให้บริการ | Interface |
| --- | --- | --- |
| Frontend | Backend | REST JSON `/api/v1/*` (ดูทั้งหมดที่ `/swagger-ui.html`) |
| Browser | Backend | `GET /api/v1/logos/{filename}` สำหรับรูปโลโก้ |
| Service | Listener | Spring `ApplicationEventPublisher` (`MatchResultRecordedEvent`, `FreeFireGameRecordedEvent`) |
| Repository | PostgreSQL | JDBC (`spring.datasource.*` จาก Environment Variable) |
| TeamLogoService | ที่เก็บไฟล์ | `FileStorageService` (ตอนนี้มี `LocalFileStorageService` ตัวเดียว) |
