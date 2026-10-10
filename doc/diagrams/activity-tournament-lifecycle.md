# Activity Diagram: วงจรการจัดรายการแข่งขัน

ขั้นตอนตั้งแต่ผู้จัดเตรียมข้อมูล สร้างรายการ ไปจนจบการแข่งขัน แบ่ง swimlane เป็น **ผู้จัด** และ **ระบบ** ส่วนผู้ชมดูข้อมูลได้ทุกช่วงโดยไม่กระทบขั้นตอนนี้

```mermaid
flowchart TB
    Start([เริ่มต้น])

    subgraph Admin["ผู้จัด (Admin)"]
        A1[เข้าสู่ระบบหลังบ้าน]
        A2[สร้าง / แก้ผู้เล่น]
        A3[สร้างทีม เลือกเกม<br/>อัปโหลดโลโก้]
        A4[เพิ่มผู้เล่นเข้าทีม]
        A5[สร้างรายการแข่งขัน<br/>เลือกเกม รูปแบบ วันที่]
        A6[เลือกทีมเพิ่มเข้ารายการ]
        A7{เพิ่มทีมครบแล้ว?}
        A8[สั่งสร้างตารางการแข่ง]
        A9[บันทึกผลแมตช์ / ผลเกม]
        A10[แก้ไขข้อมูลแล้วลองใหม่]
    end

    subgraph Sys["ระบบ"]
        S1{ข้อมูลรายการถูกต้อง?<br/>ชื่อไม่ซ้ำ, end ≥ start<br/>Free Fire = POINTS + totalGames}
        S2[บันทึกรายการ<br/>status = UPCOMING]
        S3{ผ่าน 8 กฎ<br/>TeamJoinRuleChain?}
        S4[บันทึก tournament_teams<br/>+ snapshot ชื่อทีมและ roster]
        S5{UPCOMING, ยังไม่มีแมตช์<br/>และมีทีม ≥ 2?}
        S6{format?}
        S7[SingleEliminationStrategy<br/>สร้างแมตช์ทุกรอบ จัดบาย]
        S8[PointsStrategy<br/>สร้างเกม 1..totalGames]
        S9[status = ONGOING]
        S10{ครบเงื่อนไขจบ?<br/>บันทึกนัดชิง / ครบทุกเกม}
        S11[status = COMPLETED]
        Err[ตอบ 400 / 404 / 409]
    end

    Start --> A1 --> A2 --> A3 --> A4 --> A5 --> S1
    S1 -- ไม่ผ่าน --> Err
    S1 -- ผ่าน --> S2 --> A6 --> S3
    S3 -- ไม่ผ่าน --> Err
    S3 -- ผ่าน --> S4 --> A7
    A7 -- ยัง --> A6
    A7 -- ครบ --> A8 --> S5
    S5 -- ไม่ผ่าน --> Err
    S5 -- ผ่าน --> S6
    S6 -- SINGLE_ELIMINATION --> S7 --> S9
    S6 -- POINTS --> S8 --> S9
    S9 --> A9 --> S10
    S10 -- ยัง --> A9
    S10 -- ครบ --> S11 --> End([สิ้นสุด])
    Err --> A10
```

## หมายเหตุ

- ผู้จัดแก้ / ลบรายการได้ระหว่าง `UPCOMING` เท่านั้น เมื่อเป็น `ONGOING` แล้วลบรายการ (409), เพิ่มหรือถอนทีม และสร้างตารางซ้ำไม่ได้
- ทีมที่เข้ารายการแล้วลบไม่ได้ (`Team has tournament history`) และเปลี่ยนเกมของทีมไม่ได้
- ขั้นบันทึกผลแยกละเอียดไว้ใน [activity-record-result.md](activity-record-result.md)
- เงื่อนไข 8 ข้อของการเพิ่มทีมเข้ารายการอยู่ใน Activity Diagram ของ [../design-patterns.md](../design-patterns.md) หัวข้อ 2
- สถานะ `UPCOMING → ONGOING → COMPLETED` ดูแบบ State Diagram ได้ที่ [state-diagram-match-tournament.md](state-diagram-match-tournament.md)
