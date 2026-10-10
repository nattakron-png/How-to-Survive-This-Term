# SOLID Analysis

## คนที่ 2: โมดูล Team และ Player

### S: Single Responsibility Principle (หลักเด่นของโมดูลนี้)

**หลัก:** คลาสหนึ่งควรมีเหตุผลหลักที่ทำให้ต้องแก้เพียงเรื่องเดียว

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/impl/TeamServiceImpl.java` | 25, 56–102 | จัดการข้อมูลทีมปัจจุบัน ไม่เพิ่ม/ย้าย/ถอดผู้เล่น |
| `service/impl/TeamMembershipServiceImpl.java` | 19, 34–70 | ค้นสมาชิก เพิ่ม/ย้าย และถอดผู้เล่นออกจากทีม |
| `service/impl/TeamLogoServiceImpl.java` | 16, 30–46 | ตรวจทีม เก็บโลโก้ และบันทึก URL |

**เหตุผล:** กฎสมาชิกหรือการเก็บโลโก้เปลี่ยนได้โดยไม่ต้องยัด logic กลับใน Team CRUD; Controller ส่งคำขอต่อ Service และ Mapper แปลง DTO แยกหน้าที่กัน

**ข้อจำกัด:** `TournamentRosterSnapshotService` (บรรทัด 28–57) ยังรวมการคัดลอก/อ่าน snapshot และ SQL ในคลาสเดียว

### O: Open/Closed Principle

**หลัก:** เปิดให้เพิ่มพฤติกรรมใหม่โดยลดการแก้ส่วนที่ใช้งานอยู่

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/storage/FileStorageService.java` | 5–11 | กำหนดสัญญาเก็บ อ่าน และลบโลโก้ |
| `service/impl/TeamLogoServiceImpl.java` | 20–25 | พึ่ง `FileStorageService` ไม่รู้รายละเอียดการเก็บไฟล์ |
| `service/storage/LocalFileStorageService.java` | 24 | implementation สำหรับไฟล์ local |

**เหตุผล:** หากเพิ่ม cloud storage สามารถเขียน implementation ใหม่ตาม interface โดยกฎ upload ใน `TeamLogoServiceImpl` ไม่ต้องเปลี่ยน

**ข้อจำกัด:** ปัจจุบันมี implementation จริงตัวเดียว; การเพิ่ม cloud ยังต้องตั้งค่า Spring bean และทดสอบพฤติกรรมให้ตรงกัน จึงไม่อ้างว่าทั้งโมดูลไม่ต้องแก้เมื่อเพิ่ม feature

### D: Dependency Inversion Principle

**หลัก:** ส่วนที่ประสานงานควรพึ่งสัญญา มากกว่าผูกกับ implementation ที่เปลี่ยนได้

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `controller/api/TeamController.java` | 30–34 | รับ `TeamService` ผ่าน constructor ไม่สร้าง `TeamServiceImpl` เอง |
| `controller/api/TeamMembershipController.java` | 22–26 | รับ `TeamMembershipService` ผ่าน constructor |
| `service/impl/TeamLogoServiceImpl.java` | 20–25 | รับ `FileStorageService` และ `TeamRepository` ผ่าน constructor |

**เหตุผล:** แต่ละส่วนทดสอบกับ mock dependency ได้โดยไม่ต้องเปิดฐานข้อมูลหรือเขียนไฟล์จริงทุกกรณี

**ข้อจำกัด:** `TournamentTeamServiceImpl` ยังพึ่ง `TournamentRosterSnapshotService` แบบ concrete และ SnapshotService ใช้ `JdbcTemplate` พร้อม SQL โดยตรง

### L: Liskov Substitution Principle

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/storage/FileStorageService.java` | 5–11 | สัญญาของการเก็บ อ่าน และลบโลโก้ |
| `service/storage/LocalFileStorageService.java` | 24 | ทำตาม interface ที่ `TeamLogoServiceImpl` เรียก |

**เหตุผล:** โค้ดเรียกใช้ผ่าน `FileStorageService` จึงมีจุดให้แทน implementation ได้ แต่ตอนนี้มีตัวจริงเพียงตัวเดียว ยังไม่มี contract test เปรียบเทียบ local กับ cloud จึง **ยังไม่อ้างว่าพิสูจน์ LSP ครบ**

### I: Interface Segregation Principle

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/TeamService.java` | 9–20 | เฉพาะ Team CRUD |
| `service/TeamMembershipService.java` | 8–14 | เฉพาะดู/เพิ่ม/ถอดสมาชิก |
| `service/TeamLogoService.java` | 7–10 | เฉพาะอัปโหลดโลโก้ |
| `service/PlayerService.java` | 9–20 | เฉพาะ Player CRUD |

**เหตุผล:** Controller ของแต่ละงานพึ่งเฉพาะเมธอดที่ต้องใช้; ไม่มีเมธอดสมาชิกหรือโลโก้ยัดอยู่ใน `TeamService` ข้อจำกัดคือ `TeamLogoService` รับ `MultipartFile` จึงยังผูกกับ Spring Web

**Design Patterns/แนวทางที่ใช้ในพาร์ทนี้:** Service Layer, Repository และ DTO/Mapper แยก HTTP–กฎ–ข้อมูล; `FileStorageService` เป็นขอบเขตสำหรับเปลี่ยน storage; V11 เก็บ historical snapshot ต่อทัวร์เพื่อรักษาข้อมูลย้อนหลัง โดย snapshot นี้ไม่ใช่ GoF Memento ส่วน Chain of Responsibility, Strategy และ Observer อยู่ในโมดูล Tournament/Schedule/Result ของสมาชิกอื่น


## คนที่ 3: โมดูล Tournament และ TournamentTeam

### S: Single Responsibility Principle (หลักเด่นของโมดูลนี้)

**หลัก:** คลาสหนึ่งควรมีเหตุผลหลักที่ทำให้ต้องแก้เพียงเรื่องเดียว

| ไฟล์ | สิ่งที่ทำ |
| --- | --- |
| `service/impl/TournamentServiceImpl.java` | สร้าง ค้นหา แก้ไข และลบ Tournament พร้อมตรวจสอบกฎของข้อมูลการแข่งขัน |
| `service/impl/TournamentTeamServiceImpl.java` | เมธอด `addTeam` เพิ่มทีมเข้า Tournament และ `removeTeam` นำทีมออกจาก Tournament |
| `service/rule/TeamJoinRuleChain.java` | เมธอด `validate` เรียกกฎตรวจสอบการเข้าร่วม Tournament ตามลำดับ |
| `service/rule/TeamExistsRule.java` | ตรวจสอบว่าทีมที่ต้องการเพิ่มมีอยู่จริง |
| `service/rule/TeamGameMatchRule.java` | ตรวจสอบว่าเกมของทีมตรงกับเกมของ Tournament |

**เหตุผล:** การแยกการจัดการ Tournament การเพิ่มและนำทีมออก และกฎตรวจสอบการเข้าร่วมออกจากกัน ทำให้การเปลี่ยนกฎของทีมไม่จำเป็นต้องนำเงื่อนไขทั้งหมดไปเขียนรวมใน `TournamentTeamServiceImpl`

### O: Open/Closed Principle

**หลัก:** เปิดให้เพิ่มพฤติกรรมใหม่โดยลดการแก้ส่วนที่ใช้งานอยู่

| ไฟล์ | สิ่งที่ทำ |
| --- | --- |
| `service/rule/TeamJoinRule.java` | กำหนด interface กลางสำหรับกฎตรวจสอบการเข้าร่วม Tournament |
| `service/rule/TeamJoinRuleChain.java` | รวมกฎและเรียกใช้ตามลำดับ |
| `service/rule/TeamExistsRule.java` | ตรวจสอบว่าทีมมีอยู่จริง |
| `service/rule/TeamNotAlreadyJoinedRule.java` | ป้องกันการเพิ่มทีมเดิมซ้ำใน Tournament |
| `service/rule/TeamMinimumPlayersRule.java` | ตรวจสอบจำนวนผู้เล่นขั้นต่ำของทีมตามเกม |
| `service/rule/TeamPointsLimitRule.java` | จำกัดจำนวนทีมใน Tournament แบบ `POINTS` ไม่ให้เกิน 12 ทีม |
| `service/rule/TeamTournamentDateRule.java` | ตรวจสอบว่าช่วงเวลาแข่งขันของทีมไม่ทับซ้อนกับ Tournament อื่น |

**เหตุผล:** เมื่อเพิ่มกฎตรวจสอบใหม่ สามารถสร้างคลาสที่ implement `TeamJoinRule` แล้วนำไปเชื่อมกับ `TeamJoinRuleChain` ได้ แทนที่จะเพิ่มเงื่อนไขทั้งหมดไว้ใน Service เดียว

**ข้อจำกัดที่ตรงไปตรงมา:** การเพิ่มกฎใหม่ยังต้องนำกฎเข้า Chain และเขียน Test ให้ครอบคลุม จึงไม่ใช่การเพิ่มคลาสใหม่แล้วระบบจะเรียกใช้งานเองโดยอัตโนมัติ

### D: Dependency Inversion Principle

**หลัก:** ส่วนที่ประสานงานควรพึ่งสัญญา มากกว่าผูกกับ implementation ที่เปลี่ยนได้

| ไฟล์ | สิ่งที่ทำ |
| --- | --- |
| `controller/api/TournamentController.java` | เรียกใช้ Service สำหรับจัดการ Tournament แทนการเขียนกฎทางธุรกิจไว้ใน Controller |
| `controller/api/TournamentTeamController.java` | ส่งคำขอเพิ่มหรือนำทีมออกไปให้ Service จัดการ |
| `service/impl/TournamentTeamServiceImpl.java` | รับ `TeamJoinRuleChain` ผ่าน Dependency Injection และเรียก `validate` ก่อนบันทึกทีม |
| `service/rule/TeamJoinRule.java` | กำหนดสัญญากลางให้กฎตรวจสอบแต่ละคลาส |

**เหตุผล:** การแยก Controller ออกจาก Service และเรียกใช้กฎผ่าน Chain ช่วยให้ทดสอบแต่ละส่วนแยกกันได้ โดยสามารถใช้ mock dependency แทนการเรียกใช้ระบบจริงใน Test

### L: Liskov Substitution Principle

| ไฟล์ | สิ่งที่ทำ |
| --- | --- |
| `service/rule/TeamJoinRule.java` | กำหนดสัญญากลางสำหรับกฎตรวจสอบการเข้าร่วม Tournament |
| `service/rule/TeamExistsRule.java` | implementation สำหรับตรวจสอบการมีอยู่ของทีม |
| `service/rule/TournamentExistsRule.java` | implementation สำหรับตรวจสอบการมีอยู่ของ Tournament |
| `service/rule/TournamentStatusRule.java` | implementation สำหรับตรวจสอบสถานะและเงื่อนไขก่อนเพิ่มทีม |
| `service/rule/TeamGameMatchRule.java` | implementation สำหรับตรวจสอบเกมของทีมกับ Tournament |

**เหตุผล:** กฎตรวจสอบแต่ละคลาสถูกเรียกใช้ผ่าน `TeamJoinRule` ได้ หากทุกคลาสทำตามสัญญาเดียวกัน `TeamJoinRuleChain` ก็สามารถเรียกกฎแต่ละตัวได้โดยไม่จำเป็นต้องรู้รายละเอียดการทำงานภายใน

**ข้อจำกัดที่ตรงไปตรงมา:** การ implement interface เดียวกันยังไม่เพียงพอที่จะพิสูจน์ LSP ต้องทดสอบด้วยว่าแต่ละกฎให้ผลการตรวจสอบและจัดการข้อผิดพลาดตามสัญญาที่กำหนด

### I: Interface Segregation Principle

| ไฟล์ | สิ่งที่ทำ |
| --- | --- |
| `service/TournamentService.java` | แยกสัญญาการทำงานของ Tournament ออกจากส่วนอื่น |
| `service/TournamentTeamService.java` | แยกสัญญาการจัดการทีมที่เข้าร่วม Tournament |
| `repository/TournamentRepository.java` | จัดการการเข้าถึงข้อมูล Tournament |
| `repository/TournamentTeamRepository.java` | จัดการข้อมูลความสัมพันธ์ระหว่าง Tournament กับทีม |
| `repository/TournamentPlacementPointRepository.java` | จัดการข้อมูลคะแนนตามอันดับของ Tournament แบบ `POINTS` |

**เหตุผล:** การแยก Service และ Repository ตามหน้าที่ทำให้แต่ละส่วนเรียกใช้ความสามารถที่เกี่ยวข้องกับงานของตัวเอง โดยการจัดการ Tournament การจัดการทีม และการจัดการคะแนนตามอันดับไม่จำเป็นต้องรวมอยู่ใน interface เดียวกัน

**Design Patterns/แนวทางที่ใช้ในพาร์ทนี้:** ใช้ Service Layer และ Repository เพื่อแยกกฎทางธุรกิจออกจากการเข้าถึงข้อมูล และใช้ Chain of Responsibility ผ่าน `TeamJoinRule` กับ `TeamJoinRuleChain` เพื่อแยกกฎตรวจสอบการเข้าร่วม Tournament เป็นคลาสย่อย เช่น การตรวจสอบทีม การตรวจสอบเกม จำนวนผู้เล่น จำนวนทีม และช่วงเวลาแข่งขัน นอกจากนี้ Tournament แบบ `POINTS` จะสร้าง Placement Points เริ่มต้นตามอันดับ 1–12 โดยใช้คะแนน `12, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0`

































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

## คนที่ 5: โมดูลผลการแข่ง

### S: Single Responsibility Principle (หลักเด่นของโมดูลนี้)

**หลัก:** คลาสหนึ่งควรมีเหตุผลให้ต้องแก้เพียงเรื่องเดียว

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/impl/MatchResultServiceImpl.java` | 97 | ตรวจกฎและบันทึกผลเสร็จแล้ว แค่ `publishEvent` ออกไป ไม่จัดการสายการแข่งเอง |
| `event/BracketProgressionListener.java` | 24–25 | รับผิดชอบเรื่องเดียว คือส่งผู้ชนะไปแมตช์ถัดไป หรือจบรายการเมื่อเป็นนัดชิง |
| `event/FreeFireCompletionListener.java` | 24–25 | รับผิดชอบเรื่องเดียว คือเช็กว่าแข่งครบทุกเกมแล้วหรือยัง |
| `service/freefire/PointsCalculator.java` | 19, 82–86 | คำนวณคะแนนและเรียงอันดับ (Booyah → Kills → อันดับเกมล่าสุด) โดยไม่ยุ่งกับฐานข้อมูลเลย |
| `mapper/MatchResultMapper.java` | 12 | แปลง Entity เป็น DTO อย่างเดียว |

**เหตุผล:** ถ้าเกณฑ์คะแนน Free Fire เปลี่ยน จะแก้แค่ `PointsCalculator` ถ้ากติกาการเลื่อนช่องในสายเปลี่ยน จะแก้แค่ `BracketProgressionListener` และ Service บันทึกผลไม่ต้องถูกแก้ในทั้งสองกรณี ผลที่เห็นจริงคือ `PointsCalculatorTest` ทดสอบการตัดสินเสมอได้ครบโดยไม่ต้อง mock repository เลย

### O: Open/Closed Principle

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `service/impl/MatchResultServiceImpl.java` | 31, 97 | พึ่ง `ApplicationEventPublisher` และประกาศ event โดยไม่รู้ว่ามี Listener กี่ตัว |
| `service/impl/FreeFireResultServiceImpl.java` | 127 | ประกาศ `FreeFireGameRecordedEvent` แบบเดียวกัน |

**เหตุผล:** ถ้าต้องการงานใหม่หลังบันทึกผล เช่น แจ้งเตือนผู้ชม ให้เพิ่มคลาส Listener ใหม่ที่มี `@EventListener` รับ event เดิมได้เลย โดยไม่ต้องแก้ Service (ดูหัวข้อ Observer ใน [design-patterns.md](design-patterns.md))

**ข้อจำกัดที่ตรงไปตรงมา:** ถ้างานใหม่ต้องใช้ข้อมูลที่ยังไม่มีใน event (ตอนนี้ event มีแค่ id) ก็ต้องแก้ record ของ event ด้วย

### D: Dependency Inversion Principle

| ไฟล์ | บรรทัด | สิ่งที่ทำ |
| --- | --- | --- |
| `controller/api/MatchResultController.java` | 24–26 | พึ่ง `MatchResultService` (interface) ไม่ใช่ `MatchResultServiceImpl` |
| `controller/api/FreeFireResultController.java` | 27–29 | พึ่ง `FreeFireResultService` (interface) |
| `service/impl/MatchResultServiceImpl.java` | 31 | พึ่ง `ApplicationEventPublisher` (interface ของ Spring) ไม่ได้เรียก Listener ตรงๆ |

**เหตุผล:** Service ไม่รู้จัก `BracketProgressionListener` เลย จึงทดสอบ Service แยกได้ `MatchResultServiceImplTest` ส่ง mock ของ `ApplicationEventPublisher` เข้าไปแล้ว `verify(events, never()).publishEvent(any())` ในกรณีที่กฎไม่ผ่าน (บรรทัด 104, 118)

### L: Liskov Substitution Principle

| ไฟล์ | สิ่งที่ทำ |
| --- | --- |
| `service/MatchResultService.java` / `service/impl/MatchResultServiceImpl.java` | Implementation ทำตามสัญญาของ interface ครบ ทั้งค่าที่คืนและ Exception ที่โยน (404, 409, 400) |
| `service/FreeFireResultService.java` / `service/impl/FreeFireResultServiceImpl.java` | ทุก method โยน `ResourceNotFoundException` เมื่อไม่พบข้อมูล และ `BusinessException` เมื่อรายการไม่ใช่แบบเก็บคะแนน เหมือนกันทุกตัว |

**เหตุผล:** Controller เรียกผ่าน interface และ `GlobalExceptionHandler` แปลง Exception เป็น HTTP status ได้ถูกต้องเสมอ ถ้าเปลี่ยนไปใช้ implementation อื่นที่ทำตามสัญญาเดียวกัน Controller ก็ไม่ต้องแก้

### I: Interface Segregation Principle

| ไฟล์ | สิ่งที่ทำ |
| --- | --- |
| `service/MatchResultService.java` | มีแค่ 2 method (`record`, `get`) สำหรับผลแบบแพ้คัดออก |
| `service/FreeFireResultService.java` | แยกเป็น interface ของแบบเก็บคะแนนโดยเฉพาะ (`record`, `getResults`, `standings`, `listGames`) |

**เหตุผล:** แยก interface ตามรูปแบบการแข่ง ทำให้ `MatchResultController` ไม่ต้องพึ่ง method ของ Free Fire ที่ไม่ได้ใช้ และกลับกันด้วย


