# Sequence Diagram: การบันทึกผลการแข่ง

ลำดับการทำงานของการบันทึกผลทั้งสองรูปแบบ ส่วนที่ทำต่อหลังบันทึกผล (ส่งผู้ชนะไปแมตช์ถัดไป หรือจบรายการ) ถูกแยกออกไปเป็น Listener ด้วย Observer Pattern ทุกขั้นทำงานใน transaction เดียวกัน ถ้า Listener ล้มเหลว ผลที่เพิ่งบันทึกจะถูก rollback ไปด้วย

## 1. แบบแพ้คัดออก: `POST /api/v1/matches/{matchId}/result`

```mermaid
sequenceDiagram
    actor Admin
    participant C as MatchResultController
    participant S as MatchResultServiceImpl
    participant MR as MatchRepository
    participant RR as MatchResultRepository
    participant P as ApplicationEventPublisher
    participant L as BracketProgressionListener
    participant DB as Database

    Admin->>C: POST /matches/{id}/result {teamAScore, teamBScore, winnerTeamId}
    Note over C: @Valid ตรวจ DTO (ห้ามว่าง, คะแนนไม่ติดลบ) ไม่ผ่าน → 400
    C->>S: record(matchId, request)
    S->>MR: findById(matchId)
    MR->>DB: SELECT matches
    alt ไม่พบแมตช์
        S-->>C: ResourceNotFoundException
        C-->>Admin: 404
    end
    S->>RR: existsByMatchId(matchId)
    alt มีผลแล้ว หรือยังรู้ทีมไม่ครบสองฝั่ง
        S-->>C: BusinessException
        C-->>Admin: 409
    end
    Note over S: ตรวจคะแนนห้ามเสมอ, ผู้ชนะต้องอยู่ในแมตช์ และมีคะแนนมากกว่า ไม่ผ่าน → 400
    S->>RR: save(MatchResult)
    RR->>DB: INSERT match_results
    S->>S: match.status = COMPLETED
    S->>P: publishEvent(MatchResultRecordedEvent)
    P->>L: onResultRecorded(event)
    L->>MR: findById(matchId)
    alt มีแมตช์ถัดไป (next_match_id)
        L->>L: ใส่ผู้ชนะในช่อง A (match_number คี่) หรือ B (คู่)
        opt รู้ทีมครบสองฝั่งแล้ว
            L->>L: next.status = SCHEDULED
        end
    else เป็นนัดชิง
        L->>L: tournament.status = COMPLETED
    end
    L-->>S: เสร็จ
    Note over S,DB: commit transaction (UPDATE matches, tournaments)
    S-->>C: MatchResultResponse
    C-->>Admin: 201 Created
```

## 2. แบบเก็บคะแนน (Free Fire): `POST /api/v1/free-fire-games/{gameId}/results`

```mermaid
sequenceDiagram
    actor Admin
    participant C as FreeFireResultController
    participant S as FreeFireResultServiceImpl
    participant GR as FreeFireGameRepository
    participant TT as TournamentTeamRepository
    participant RR as FreeFireGameResultRepository
    participant P as ApplicationEventPublisher
    participant L as FreeFireCompletionListener
    participant DB as Database

    Admin->>C: POST /free-fire-games/{id}/results {results: [teamId, placement, kills]}
    Note over C: @Valid ตรวจ DTO (ห้ามว่าง, kills ไม่ติดลบ) ไม่ผ่าน → 400
    C->>S: record(gameId, request)
    S->>GR: findById(gameId)
    alt ไม่พบเกม
        S-->>C: ResourceNotFoundException
        C-->>Admin: 404
    end
    alt รายการไม่ใช่ POINTS หรือเกมนี้มีผลแล้ว
        S-->>C: BusinessException
        C-->>Admin: 409
    end
    S->>TT: findByTournamentId(tournamentId)
    Note over S: ต้องกรอกครบทุกทีม ไม่ซ้ำ ไม่มีทีมนอกรายการ<br/>และอันดับไม่ซ้ำ อยู่ในช่วง 1..จำนวนทีม ไม่ผ่าน → 400
    S->>RR: saveAll(FreeFireGameResult)
    RR->>DB: INSERT free_fire_game_results
    S->>S: game.status = COMPLETED
    S->>P: publishEvent(FreeFireGameRecordedEvent)
    P->>L: onGameRecorded(event)
    L->>GR: countByTournamentIdAndStatus(tournamentId, COMPLETED)
    opt จำนวนเกมที่จบ >= total_games
        L->>L: tournament.status = COMPLETED
    end
    L-->>S: เสร็จ
    Note over S,DB: commit transaction
    S-->>C: FreeFireGameResultsResponse (คะแนนอันดับ + คะแนน kill ของแต่ละทีม)
    C-->>Admin: 201 Created
```

คะแนนไม่ได้เก็บลงฐานข้อมูล แต่ `PointsCalculator` คำนวณจากผลดิบ (อันดับ, kills) ทุกครั้งที่เรียก `GET /tournaments/{id}/standings` และ `GET /free-fire-games/{id}/results`