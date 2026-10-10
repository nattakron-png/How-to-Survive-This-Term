# Activity Diagram: การบันทึกผลการแข่ง

แสดงการตัดสินใจทุกขั้นของการบันทึกผลทั้งสองรูปแบบ ลำดับการเรียกระหว่างคลาสดูได้ที่ [sequence-diagram-result.md](sequence-diagram-result.md)

## 1. แบบแพ้คัดออก: `POST /api/v1/matches/{matchId}/result`

ตาม `MatchResultServiceImpl.record` และ `BracketProgressionListener`

```mermaid
flowchart TD
    Start([ผู้จัดส่งคะแนน teamAScore, teamBScore, winnerTeamId]) --> V0{DTO ถูกต้อง?<br/>ห้ามว่าง คะแนน ≥ 0}
    V0 -- ไม่ --> E400([400 Bad Request])
    V0 -- ใช่ --> V1{พบแมตช์?}
    V1 -- ไม่ --> E404([404 Not Found])
    V1 -- พบ --> V2{มีผลอยู่แล้ว?}
    V2 -- มี --> E409([409 Conflict])
    V2 -- ยัง --> V3{มีทีมครบ A และ B?}
    V3 -- ไม่ครบ --> E409
    V3 -- ครบ --> V4{status = SCHEDULED?}
    V4 -- ไม่ใช่ --> E409
    V4 -- ใช่ --> V5{คะแนนเสมอ?}
    V5 -- เสมอ --> E400
    V5 -- ไม่เสมอ --> V6{winnerTeamId เป็นทีมในแมตช์<br/>และมีคะแนนมากกว่า?}
    V6 -- ไม่ --> E400
    V6 -- ใช่ --> S1[บันทึก match_results<br/>match.status = COMPLETED]
    S1 --> S2[/publish MatchResultRecordedEvent/]

    S2 --> L1{มี next_match?}
    L1 -- มี --> L2{match_number คี่?}
    L2 -- คี่ --> L3[ใส่ผู้ชนะในช่อง team A<br/>ของแมตช์ถัดไป]
    L2 -- คู่ --> L4[ใส่ผู้ชนะในช่อง team B<br/>ของแมตช์ถัดไป]
    L3 --> L5{แมตช์ถัดไปมีทีมครบ?}
    L4 --> L5
    L5 -- ครบ --> L6[next.status = SCHEDULED]
    L5 -- ยัง --> Commit
    L6 --> Commit
    L1 -- ไม่มี: นัดชิง --> L7[tournament.status = COMPLETED]
    L7 --> Commit[commit transaction]
    Commit --> Done([201 Created + MatchResultResponse])
```

## 2. แบบเก็บคะแนน Free Fire: `POST /api/v1/free-fire-games/{gameId}/results`

ตาม `FreeFireResultServiceImpl.record` และ `FreeFireCompletionListener`

```mermaid
flowchart TD
    Start([ผู้จัดส่งผลทุกทีม<br/>teamId, placement, kills]) --> V0{DTO ถูกต้อง?<br/>ห้ามว่าง kills ≥ 0}
    V0 -- ไม่ --> E400([400 Bad Request])
    V0 -- ใช่ --> V1{พบเกม?}
    V1 -- ไม่ --> E404([404 Not Found])
    V1 -- พบ --> V2{รายการเป็น POINTS<br/>และเกมนี้ยังไม่มีผล?}
    V2 -- ไม่ --> E409([409 Conflict])
    V2 -- ใช่ --> V3{กรอกครบทุกทีมในรายการ<br/>ไม่ซ้ำ ไม่มีทีมนอกรายการ?}
    V3 -- ไม่ --> E400
    V3 -- ใช่ --> V4{อันดับไม่ซ้ำ<br/>และอยู่ในช่วง 1..จำนวนทีม?}
    V4 -- ไม่ --> E400
    V4 -- ใช่ --> S1[บันทึก free_fire_game_results<br/>game.status = COMPLETED]
    S1 --> S2[/publish FreeFireGameRecordedEvent/]
    S2 --> L1{จำนวนเกม COMPLETED<br/>≥ total_games?}
    L1 -- ใช่ --> L2[tournament.status = COMPLETED]
    L1 -- ยัง --> Calc
    L2 --> Calc[PointsCalculator คำนวณคะแนน<br/>อันดับ + kills × pointsPerKill]
    Calc --> Done([201 Created + FreeFireGameResultsResponse])
```

## หมายเหตุ

- ทุกขั้นอยู่ใน transaction เดียว ถ้า Listener ล้มเหลว ผลที่บันทึกจะถูก rollback ด้วย
- ผลที่บันทึกแล้วแก้ไม่ได้ในตอนนี้ (บันทึกซ้ำตอบ 409)
- คะแนน Free Fire ไม่ได้เก็บลงฐานข้อมูล ตารางคะแนนรวมคำนวณใหม่ทุกครั้งที่เรียก `GET /tournaments/{id}/standings`
