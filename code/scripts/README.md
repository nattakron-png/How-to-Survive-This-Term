# ข้อมูลตัวอย่างสำหรับ PostgreSQL ในเครื่อง

`generate-local-mock-seed.mjs` แปลงข้อมูลจาก `code/frontend/src/mock/` เป็น SQL สำหรับฐาน Docker local เท่านั้น ใช้ข้อมูลทีม ผู้เล่น ทัวร์ ทีมในทัวร์ แมตช์ ผลแข่ง และ Free Fire โดยไม่เพิ่มบัญชีผู้ใช้จำลอง หรือแก้ Flyway migration

รันใน PowerShell จากโฟลเดอร์รากของ repository เมื่อ `docker compose` เปิด `db` และ Flyway สร้างตารางถึง V11 แล้ว (ชื่อผู้ใช้/ฐานข้อมูลในคำสั่งเป็นค่าเริ่มต้นจาก `.env.example`; หาก `.env` ของเครื่องต่างออกไปให้ใช้ค่าของเครื่องนั้น):

```powershell
docker run --rm -v "${PWD}:/work" -w /work node:22-alpine node code/scripts/generate-local-mock-seed.mjs target/mock-data.local.sql
docker cp target/mock-data.local.sql tournament-db-1:/tmp/mock-data.local.sql
docker compose exec -T db psql -U tournament -d tournament -v ON_ERROR_STOP=1 -f /tmp/mock-data.local.sql
```

ไฟล์ SQL ที่สร้างอยู่ใน `target/` ซึ่ง Git ignore ไว้ สคริปต์เติม `tournament_teams` พร้อมชื่อ/โลโก้ snapshot และ `tournament_team_rosters` พร้อมผู้เล่นตาม V11 สคริปต์หยุดหากตารางเป้าหมายมีข้อมูลอื่นอยู่แล้ว การรันซ้ำเมื่อมี mock ชุดนี้ครบจะไม่เพิ่มแถวซ้ำ ถ้าแก้ mock ภายหลัง สคริปต์จะไม่อัปเดตข้อมูลเดิมอัตโนมัติ ให้จัดการฐาน local อย่างระมัดระวัง

ตรวจข้อมูลได้ที่ `http://127.0.0.1:8080/api/v1/tournaments` และ `http://127.0.0.1:8080/api/v1/tournaments/6/standings` ข้อมูลนี้ไม่ถูกส่งไป Railway เมื่อ commit หรือ deploy โค้ด
