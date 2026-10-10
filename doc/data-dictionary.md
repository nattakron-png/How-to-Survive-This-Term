# Data Dictionary

## How to Survive This Term — E-Sport Tournament Management System

เอกสารนี้อธิบายโครงสร้างข้อมูลของระบบจัดการการแข่งขัน E-Sport ซึ่งใช้ PostgreSQL เป็นฐานข้อมูลและใช้ Flyway Migration ในการจัดการโครงสร้างตาราง

## 1. ตาราง `users`

จัดเก็บข้อมูลบัญชีผู้ใช้และบทบาทการใช้งาน

| Column | Data Type | Key | Description |
|---|---|---|---|
| `id` | BIGINT | PK | รหัสผู้ใช้ |
| `username` | VARCHAR | | ชื่อบัญชีผู้ใช้ |
| `password` | VARCHAR | | รหัสผ่านที่จัดเก็บตามรูปแบบของระบบ |
| `role` | VARCHAR | | บทบาทของผู้ใช้ |

## 2. ตาราง `players`

จัดเก็บข้อมูลผู้เล่นและทีมที่สังกัด

| Column | Data Type | Key | Description |
|---|---|---|---|
| `id` | BIGINT | PK | รหัสผู้เล่น |
| `team_id` | BIGINT | FK | รหัสทีมที่ผู้เล่นสังกัด |

## 3. ตาราง `teams`

จัดเก็บข้อมูลทีมที่เข้าร่วมการแข่งขัน

| Column | Data Type | Key | Description |
|---|---|---|---|
| `id` | BIGINT | PK | รหัสทีม |
| `name` | VARCHAR | | ชื่อทีม |
| `game_id` | BIGINT | FK | รหัสเกมที่ทีมใช้แข่งขัน |
| `logo_url` | VARCHAR | | ที่อยู่ไฟล์โลโก้ทีม |

## 4. ตาราง `games`

จัดเก็บข้อมูลเกมที่ระบบรองรับ

| Column | Data Type | Key | Description |
|---|---|---|---|
| `id` | BIGINT | PK | รหัสเกม |
| `name` | VARCHAR | | ชื่อเกม |
| `min_players` | INTEGER | | จำนวนผู้เล่นขั้นต่ำตามที่กำหนด |

## 5. ตาราง `tournaments`

จัดเก็บข้อมูลรายการแข่งขัน

| Column | Data Type | Key | Description |
|---|---|---|---|
| `id` | BIGINT | PK | รหัสรายการแข่งขัน |
| `name` | VARCHAR | | ชื่อรายการแข่งขัน |
| `game_id` | BIGINT | FK | รหัสเกมที่ใช้แข่งขัน |
| `status` | VARCHAR | | สถานะของรายการแข่งขัน |
| `format` | VARCHAR | | รูปแบบการแข่งขัน |

## 6. ตาราง `tournament_teams`

เป็นตารางกลางที่เชื่อมความสัมพันธ์ระหว่างทีมกับรายการแข่งขัน

| Column | Data Type | Key | Description |
|---|---|---|---|
| `tournament_id` | BIGINT | PK, FK | รหัสรายการแข่งขัน |
| `team_id` | BIGINT | PK, FK | รหัสทีมที่เข้าร่วมการแข่งขัน |

**Primary Key:** ใช้ `tournament_id` และ `team_id` ร่วมกันเป็น Composite Primary Key

## 7. ตาราง `matches`

จัดเก็บข้อมูลแมตช์การแข่งขันและความเชื่อมโยงของแมตช์

| Column | Data Type | Key | Description |
|---|---|---|---|
| `id` | BIGINT | PK | รหัสแมตช์ |
| `tournament_id` | BIGINT | FK | รหัสรายการแข่งขัน |
| `next_match_id` | BIGINT | FK | รหัสแมตช์ถัดไปในสายการแข่งขัน |

## 8. ตาราง `match_results`

จัดเก็บผลการแข่งขันของแต่ละแมตช์

| Column | Data Type | Key | Description |
|---|---|---|---|
| `id` | BIGINT | PK | รหัสผลการแข่งขัน |
| `match_id` | BIGINT | FK, UNIQUE | รหัสแมตช์ที่บันทึกผลการแข่งขัน |

ตารางนี้มีข้อกำหนด `UNIQUE` ที่ `match_id` เพื่อป้องกันการบันทึกผลมากกว่าหนึ่งรายการต่อแมตช์ รายละเอียดคอลัมน์คะแนนและทีมผู้ชนะต้องตรวจสอบจาก Migration จริง

## 9. ตาราง `tournament_placement_points`

ใช้จัดเก็บคะแนนที่กำหนดตามอันดับของทีมในรายการแข่งขัน

รายละเอียดชื่อคอลัมน์ ชนิดข้อมูล และข้อกำหนดของแต่ละฟิลด์ต้องอ้างอิงจาก `V10__add_free_fire_points.sql`

## 10. ตาราง `free_fire_games`

ใช้จัดเก็บข้อมูลเกมย่อยของการแข่งขัน Free Fire รูปแบบ Points

รายละเอียดคอลัมน์ต้องตรวจสอบจาก Migration จริง

## 11. ตาราง `free_fire_game_results`

ใช้จัดเก็บผลการแข่งขันของทีมในเกมย่อยของ Free Fire เช่น อันดับที่ทำได้และจำนวน Kills

รายละเอียดคอลัมน์ต้องตรวจสอบจาก Migration จริง

## ความสัมพันธ์ระหว่างตาราง

- `teams` และ `players` มีความสัมพันธ์แบบ One-to-Many
- `tournaments` และ `teams` มีความสัมพันธ์แบบ Many-to-Many ผ่าน `tournament_teams`
- `tournaments` และ `matches` มีความสัมพันธ์แบบ One-to-Many
- `matches` และ `match_results` มีข้อกำหนดให้แต่ละแมตช์มีผลการแข่งขันได้ไม่เกินหนึ่งรายการ
- ตาราง Free Fire ใช้จัดเก็บข้อมูลเกมย่อยและผลการแข่งขันที่เกี่ยวข้อง
