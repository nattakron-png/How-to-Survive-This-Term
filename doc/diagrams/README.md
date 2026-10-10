# Diagrams

แผนภาพทั้งหมดเขียนด้วย [Mermaid](https://mermaid.js.org/) GitHub และ VS Code (ส่วนขยาย Markdown Preview Mermaid Support) แสดงเป็นรูปได้ทันที หรือคัดลอกโค้ดไปวางที่ <https://mermaid.live> เพื่อส่งออกเป็น PNG / SVG

| แผนภาพ | ไฟล์ | เนื้อหา |
| --- | --- | --- |
| Use Case Diagram | [use-case-diagram.md](use-case-diagram.md) | ผู้ชมและผู้จัดทำอะไรกับระบบได้บ้าง พร้อม API ที่รองรับ |
| Domain Model | [domain-model.md](domain-model.md) | แนวคิดหลักและความสัมพันธ์ในมุมธุรกิจ |
| ER Diagram | [er-diagram.md](er-diagram.md) | ตารางในฐานข้อมูลหลัง migration V1–V11 และกฎ ON DELETE |
| Class Diagram (ภาพรวม) | [class-diagram.md](class-diagram.md) | Entity และชั้น Controller → Service ของทุกโมดูล |
| Class Diagram (สร้างตาราง) | [class-diagram-bracket.md](class-diagram-bracket.md) | Strategy Pattern ของโมดูลรูปแบบการแข่ง |
| Component Diagram | [component.md](component.md) | ส่วนประกอบ Frontend / Backend / Database และการเชื่อมต่อ |
| Deployment Diagram | [deployment-diagram.md](deployment-diagram.md) | Docker Compose, GitHub Actions CI และ Railway |
| Activity Diagram | [activity-tournament-lifecycle.md](activity-tournament-lifecycle.md) | วงจรตั้งแต่สร้างรายการจนจบการแข่ง |
| Activity Diagram | [activity-record-result.md](activity-record-result.md) | การบันทึกผลแพ้คัดออกและ Free Fire |
| Activity Diagram | [../design-patterns.md](../design-patterns.md) (หัวข้อ 2) | การเพิ่มทีมเข้ารายการผ่าน 8 กฎ |
| Sequence Diagram | [sequence-diagram-schedule.md](sequence-diagram-schedule.md) | `POST /tournaments/{id}/schedule` |
| Sequence Diagram | [sequence-diagram-result.md](sequence-diagram-result.md) | บันทึกผลทั้งสองรูปแบบ (Observer) |
| State Diagram | [state-diagram-match-tournament.md](state-diagram-match-tournament.md) | สถานะของ Match และ Tournament |
