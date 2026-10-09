# SOLID Analysis



## คนที่ 4: โมดูลรูปแบบการแข่ง

### O: Open/Closed Principle (หลักเด่นของโมดูลนี้)

**หลัก:** เปิดให้ขยาย ปิดไม่ให้แก้ไข

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/impl/ScheduleServiceImpl.java` | 34, 37–45 | เก็บ `Map<TournamentFormat, FormatStrategy>` ที่สร้างจาก `List<FormatStrategy>` ที่ Spring ส่งมา |
| `service/impl/ScheduleServiceImpl.java` | 72, 77 | ดึง Strategy ตามรูปแบบแล้วเรียก `createSchedule` ไม่มี `if/switch` ตามชนิดรูปแบบ |
| `service/format/PointsStrategy.java` | 16–17, 26, 32 | Strategy ที่เพิ่มทีหลัง ใส่ `@Component` และ implement `FormatStrategy` ก็ทำงานได้ |

**เหตุผล:** การเพิ่มรูปแบบการแข่งใหม่ทำโดย **เพิ่มคลาสใหม่** ไม่ต้องแก้ `ScheduleServiceImpl` หลักฐานจริงคือ `PointsStrategy` ถูกเขียนหลังจาก `ScheduleServiceImpl` เสร็จและเทสต์ผ่านแล้ว โดยไม่แตะโค้ดของ Service เลย (ดู `git show` ของ commit `feat: implement points strategy for free fire` ซึ่งมีไฟล์เดียว) ถ้าไม่ใช้ Strategy Service นี้จะต้องมี `if (format == POINTS) {...} else {...}` และถูกแก้ทุกครั้งที่มีรูปแบบใหม่

**ข้อจำกัดที่ตรงไปตรงมา:** การเพิ่มรูปแบบใหม่ยังต้องเพิ่มค่าใน enum `TournamentFormat` และ constraint ของคอลัมน์ `format` ใน Migration (V9) ดังนั้นไม่ได้ "ไม่แก้อะไรเลย" ทั้งระบบ แต่ไม่ต้องแก้ตรรกะของ Service

### D: Dependency Inversion Principle

**หลัก:** โมดูลระดับสูงต้องไม่พึ่งโมดูลระดับต่ำ ทั้งสองควรพึ่ง abstraction

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/impl/ScheduleServiceImpl.java` | 38 | constructor รับ `List<FormatStrategy>` (interface) ไม่ใช่คลาสจริง |
| `service/impl/ScheduleServiceImpl.java` | 72 | ตัวแปร `FormatStrategy strategy` ประกาศด้วยชนิด interface |
| `controller/api/ScheduleController.java` | 22–26 | พึ่ง `ScheduleService` (interface) ไม่ใช่ `ScheduleServiceImpl` |
| `controller/api/MatchController.java` | 18–22 | พึ่ง `MatchService` (interface) |

**เหตุผล:** Service ระดับสูงไม่รู้จัก `SingleEliminationStrategy` หรือ `PointsStrategy` เลย รู้จักแค่ `FormatStrategy` จึงสลับ implementation ได้ ผลที่เห็นจริงคือ `ScheduleServiceImplTest` ส่ง mock ของ `FormatStrategy` เข้าไปทดสอบ Service ได้โดยไม่ต้องมีฐานข้อมูลหรือสร้างสายจริง

### S: Single Responsibility Principle

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/format/SingleEliminationStrategy.java` | 41 (`buildBracket`) กับ 34 (`createSchedule`) | แยก "สร้างสายในหน่วยความจำ" ออกจาก "บันทึกลงฐานข้อมูล" |
| `service/format/SingleEliminationStrategy.java` | 110, 122, 127 | helper เล็ก ๆ ที่ทำหน้าที่เดียว (`placeInNextMatch`, `teamAtSeed`, `seedOrder`) |
| `mapper/MatchMapper.java` | ทั้งไฟล์ | แปลง Entity เป็น DTO อย่างเดียว |
| `controller/api/*Controller.java` | ทั้งไฟล์ | รับ request ส่งต่อ Service ไม่มีกฎทางธุรกิจ |

**เหตุผล:** แยก `buildBracket` ออกมาทำให้ทดสอบอัลกอริทึมสายได้ 7 เทสต์โดยไม่ต้อง mock Repository (`SingleEliminationStrategyTest` สร้างตัว Strategy ด้วย `null` แทน repository ได้)

**ข้อจำกัดที่ตรงไปตรงมา:** `ScheduleServiceImpl.createSchedule` (บรรทัด 48–80) ทำหลายอย่างในเมธอดเดียว คือตรวจ 5 กฎ เลือก Strategy และเปลี่ยนสถานะรายการ ยังพอรับได้เพราะเป็นขั้นตอนของ use case เดียว แต่ถ้ากฎโตขึ้นควรแยกกฎตรวจสอบออกเป็นคลาสของตัวเอง

### L: Liskov Substitution Principle

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/format/FormatStrategy.java` | 9–15 | สัญญา: `createSchedule` สร้างตารางแล้วคืนจำนวนที่สร้าง |
| `service/format/SingleEliminationStrategy.java` | 34 | ทำตามสัญญา คืนจำนวนแมตช์ |
| `service/format/PointsStrategy.java` | 32 | ทำตามสัญญา คืนจำนวนเกม |

**เหตุผล:** Strategy ทั้งสองแทนกันได้ใน `ScheduleServiceImpl` โดย Service ไม่ต้องตรวจชนิด ทั้งคู่โยน `ValidationException` ในกรณีข้อมูลไม่ถูกต้อง (ทีมน้อยกว่า 2 / ไม่มี `totalGames`) ซึ่งเป็น exception ชนิดเดียวกับที่ระบบจัดการอยู่แล้ว ไม่มี Strategy ตัวไหนบังคับให้ผู้เรียกต้องทำพิเศษ

### I: Interface Segregation Principle

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/format/FormatStrategy.java` | 9–15 | interface มี 2 เมธอดเท่านั้น (`format`, `createSchedule`) |
| `service/ScheduleService.java` และ `service/MatchService.java` | ทั้งไฟล์ | แยก Service เขียน/สร้างตาราง ออกจาก Service อ่านแมตช์ |

**เหตุผล:** `MatchController` ที่ต้องแค่อ่านแมตช์พึ่ง `MatchService` ที่มีแต่เมธอดอ่าน ไม่ถูกบังคับให้พึ่งเมธอดสร้างตาราง และ Strategy แต่ละตัว implement เมธอดที่ใช้ทั้งหมดจริง ไม่มีเมธอดว่างที่ต้องทิ้งไว้


