# Design Patterns ที่ใช้ในโปรเจค

เอกสารนี้ใช้ร่วมกันทั้งทีม แต่ละคนเพิ่มหัวข้อของโมดูลตัวเองต่อท้ายไฟล์ ห้ามแก้ส่วนของคนอื่น

แต่ละแถวต้องมี ปัญหาที่แก้ | ไฟล์/คลาส | Class Diagram

---

## คนที่ 4: โมดูลรูปแบบการแข่ง

### Strategy Pattern (Pattern หลักของโมดูลนี้)

| หัวข้อ | รายละเอียด |
| --- | --- |
| **ปัญหาที่แก้** | รายการแข่งมี 2 รูปแบบที่สร้างตารางต่างกันโดยสิ้นเชิง แพ้คัดออกต้องสร้างสายแมตช์พร้อม `next_match_id` และจัดบาย ส่วนเก็บคะแนน (Free Fire) ต้องสร้างเกมย่อย 1..`totalGames` ถ้าเขียนรวมไว้ใน Service เดียวจะเป็น `if (format == ...)` ยาวขึ้นทุกครั้งที่มีรูปแบบใหม่ และแก้รูปแบบหนึ่งอาจกระทบอีกรูปแบบ |
| **วิธีแก้** | แยกอัลกอริทึมแต่ละแบบเป็นคลาสของตัวเองที่ implement interface เดียวกัน แล้วให้ Service เลือกใช้ตัวที่ตรงกับ `tournament.format` ตอนทำงาน |
| **Class Diagram** | [doc/diagrams/class-diagram-bracket.md](diagrams/class-diagram-bracket.md) |
| **Sequence Diagram** | [doc/diagrams/sequence-diagram-schedule.md](diagrams/sequence-diagram-schedule.md) |

บทบาทของแต่ละคลาส

| บทบาท | คลาส | ไฟล์ |
| --- | --- | --- |
| Strategy (interface) | `FormatStrategy` | `service/format/FormatStrategy.java` |
| Concrete Strategy | `SingleEliminationStrategy` | `service/format/SingleEliminationStrategy.java` |
| Concrete Strategy | `PointsStrategy` | `service/format/PointsStrategy.java` |
| Context | `ScheduleServiceImpl` | `service/impl/ScheduleServiceImpl.java` |

**การเลือก Strategy:** `ScheduleServiceImpl` รับ `List<FormatStrategy>` ทาง constructor (Spring ส่ง `@Component` ทุกตัวให้) แล้วเก็บเป็น `Map<TournamentFormat, FormatStrategy>` โดยใช้ `format()` ของแต่ละตัวเป็น key จากนั้นดึงตัวที่ตรงกับ `tournament.getFormat()` มาเรียก `createSchedule`

**เพิ่มรูปแบบใหม่ในอนาคต** เช่น Round Robin: สร้างคลาสใหม่ที่ implement `FormatStrategy` ใส่ `@Component` และเพิ่มค่าใน `TournamentFormat` โดยไม่ต้องแก้ `ScheduleServiceImpl` (ดู [solid-analysis.md](solid-analysis.md) หลัก Open/Closed) หลักฐานในโปรเจคจริง: `PointsStrategy` ถูกเพิ่มทีหลัง `SingleEliminationStrategy` และ `ScheduleServiceImpl` ไม่ถูกแก้เลย

**การทดสอบ:** `ScheduleServiceImplTest` ส่ง `FormatStrategy` ที่เป็น mock เข้าไปแทนของจริง เพื่อพิสูจน์ว่า Service พึ่งพาแค่ interface

### Enterprise Patterns ที่ใช้ในโมดูลนี้

| Pattern | ปัญหาที่แก้ | ไฟล์/คลาส |
| --- | --- | --- |
| Service Layer | แยกกฎทางธุรกิจ (ตรวจสถานะ, ตรวจจำนวนทีม, เลือก Strategy) ออกจาก Controller | `ScheduleService`, `ScheduleServiceImpl`, `MatchService`, `MatchServiceImpl` |
| Repository | แยกการเข้าถึงฐานข้อมูลออกจากกฎทางธุรกิจ | `MatchRepository`, `TournamentRepository`, `FreeFireGameRepository` |
| DTO | ไม่ส่ง Entity (ที่มี lazy relation) ออกไปให้ client | `MatchResponse`, `ScheduleResponse` |
| Data Mapper | แปลง Entity เป็น DTO ในที่เดียว | `MatchMapper` |
| Dependency Injection | ให้ Spring ประกอบคลาสให้ ไม่สร้างกันเอง ทำให้สลับเป็น mock ตอนทดสอบได้ | constructor ของ `ScheduleServiceImpl`, `ScheduleController` และ `MatchController` |

### Pattern ของเพื่อนที่โมดูลนี้เชื่อมอยู่ด้วย (ไม่ใช่งานของคนที่ 4)

- **Observer** (ณัฐกร): เมื่อมีผลแมตช์ `BracketProgressionListener` ส่งผู้ชนะไปแมตช์ถัดไปตามสายที่ `SingleEliminationStrategy` สร้างไว้ จึงต้องสร้าง `match_number` และ `next_match_id` ให้ตรงกติกาเลขคี่/เลขคู่ของเขา
- **Chain of Responsibility** (คนที่ 3): กฎการเพิ่มทีมเข้ารายการ `ScheduleServiceImpl` ใช้ผลของมัน คือรายการต้องมีทีมอย่างน้อย 2 ทีมก่อนสร้างตาราง

---

## คนที่ 1: ฐานข้อมูลและ Entity

_ยังไม่มีเนื้อหา คนที่ 1 เพิ่มตรงนี้_

## คนที่ 2: Team และ Player

_ยังไม่มีเนื้อหา คนที่ 2 เพิ่มตรงนี้_

## คนที่ 3: Tournament และ TournamentTeam (Chain of Responsibility)

_ยังไม่มีเนื้อหา คนที่ 3 เพิ่มตรงนี้_

## คนที่ 5: ผลการแข่ง (Observer)

_ยังไม่มีเนื้อหา คนที่ 5 เพิ่มตรงนี้_
