# Sequence Diagram: การสร้างตารางการแข่ง

ลำดับการทำงานของ `POST /api/v1/tournaments/{id}/schedule` ทั้งสองรูปแบบ (แพ้คัดออกและเก็บคะแนน) ส่วนที่ต่างกันคือขั้น "สร้างตาราง" ซึ่งถูกเลือกโดย Strategy Pattern

```mermaid
sequenceDiagram
    actor Admin
    participant C as ScheduleController
    participant S as ScheduleServiceImpl
    participant TR as TournamentRepository
    participant MR as MatchRepository
    participant TT as TournamentTeamRepository
    participant F as FormatStrategy
    participant DB as Database

    Admin->>C: POST /tournaments/{id}/schedule
    C->>S: createSchedule(id)

    S->>TR: findById(id)
    TR-->>S: Tournament
    Note over S: ไม่พบรายการ → 404

    Note over S: สถานะต้องเป็น UPCOMING → ไม่ใช่ตอบ 409
    S->>MR: existsByTournamentId(id)
    MR-->>S: false
    Note over S: มีแมตช์อยู่แล้ว → 409

    S->>TT: findByTournamentIdOrderByJoinedAtAsc(id)
    TT-->>S: ทีมเรียงตามลำดับเข้าร่วม
    Note over S: น้อยกว่า 2 ทีม → 409

    Note over S: เลือก Strategy จาก tournament.format
    Note over S: ไม่มี Strategy รองรับ → 409

    alt format = SINGLE_ELIMINATION
        S->>F: createSchedule(tournament, teams) [SingleEliminationStrategy]
        Note over F: buildBracket: สร้างแมตช์ทุกรอบ<br/>ผูก next_match, วางทีมตาม seed, จัดบาย
        F->>DB: matches.saveAll(เรียงจากนัดชิงลงมา)
        DB-->>F: บันทึกแล้ว
        F-->>S: จำนวนแมตช์ (จำนวนทีม - 1)
    else format = POINTS
        S->>F: createSchedule(tournament, teams) [PointsStrategy]
        Note over F: totalGames ต้องไม่ว่าง → ไม่ใช่ตอบ 400<br/>สร้างเกม 1..totalGames สถานะ SCHEDULED
        F->>DB: freeFireGames.saveAll(เกมทั้งหมด)
        DB-->>F: บันทึกแล้ว
        F-->>S: จำนวนเกม (totalGames)
    end

    S->>S: tournament.status = ONGOING
    S-->>C: ScheduleResponse(tournamentId, format, created)
    C-->>Admin: 201 Created + Location /tournaments/{id}/matches
```

## จุดที่ควรชี้ตอนนำเสนอ

- **Strategy Pattern:** `ScheduleServiceImpl` เรียก `FormatStrategy.createSchedule` โดยไม่รู้ว่าเป็นรูปแบบไหน ตัวที่ถูกเลือกขึ้นกับ `tournament.format` ส่วน `alt` ในแผนภาพคือสองคลาสที่ implement interface เดียวกัน
- **ทุกขั้นตอนอยู่ใน Transaction เดียว** (`@Transactional` ที่ `ScheduleServiceImpl`) ถ้าขั้นใดโยน exception ข้อมูลที่บันทึกไปแล้วจะ rollback ทั้งหมด และสถานะรายการไม่เปลี่ยนเป็น `ONGOING`
- **Exception → HTTP status:** แต่ละกฎโยน `ResourceNotFoundException` (404), `BusinessException` (409) หรือ `ValidationException` (400) แล้ว `GlobalExceptionHandler` แปลงเป็น response ให้เอง Controller ไม่ต้องจับ exception
- **รายการที่เป็น `ONGOING` แล้ว** สร้างตารางซ้ำไม่ได้ (ตอบ 409) ซึ่งกันการสร้างสายซ้ำทั้งสองรูปแบบ

## ลำดับการ save แมตช์ (แพ้คัดออก)

`buildBracket` คืนรายการเรียงจากนัดชิงลงมา เพื่อให้ `saveAll` บันทึกแมตช์ปลายทางก่อน แมตช์รอบก่อนหน้าจึงอ้าง `next_match_id` ได้ ถ้าเรียงกลับด้านจะอ้างแมตช์ที่ยังไม่มี id
