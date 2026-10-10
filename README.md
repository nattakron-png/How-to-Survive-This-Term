# HowToSurviveThisTerm
## ระบบจัดการสายแข่ง E-Sport

ผู้ชมเข้ามาติดตามรายการ ตารางแข่ง และผลการแข่งขันที่ผู้จัดบันทึกไว้ ผู้จัดเป็นผู้เพิ่มข้อมูลเกม ทีม ผู้เล่น รายการ และผลแข่งผ่านหลังบ้าน ระบบไม่ได้เปิดให้ผู้เล่นหรือทีมสมัครเข้ารายการด้วยตนเอง

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Email | หน้าที่ |
|---|---|---|---|---|---|
| 1 | นายคณิศร มาประจักษ์ | 673380031-3 | 2 | kanisorn.m@kkumail.com |   |
| 2 | นายนฤเศรษฐ์ อภิลักขิตพงศ์ | 673380044-4 | 2 | naruset.a@kkumail.com |   |
| 3 | นายสิรภัทร ลีล้าน | 673380067-2 | 2 | Siraphat.l@kkumail.com |   |
| 4 | นายณัฐกร รุ่งฟ้า | 673380512-7 | 2 | nattakron.r@kkumail.com |   |
| 5 | นายวัชรพล ดวงกองเงิน | 673380290-9 | 2 | Vacharapoln.d@kkumail.com |   |

## เริ่มพัฒนา

เปิด Docker Desktop โดยใช้ Linux containers จากนั้นรันในโฟลเดอร์ repo:

```powershell
Copy-Item .env.example .env
docker compose up -d --build --wait
```

คัดลอก `.env` เฉพาะครั้งแรก ถ้ามีไฟล์อยู่แล้วให้ใช้ไฟล์เดิม Docker build ด้วย Java 21 ให้ ไม่ต้องติดตั้ง Java หรือ Maven ในเครื่องสำหรับวิธีนี้

ตรวจแอปที่ <http://localhost:8080/actuator/health> ควรได้ `{"status":"UP"}` หน้า `/` ยังไม่มีหน้าเว็บหลัก จึงอาจได้ 404

เปิด <http://localhost:8080/swagger-ui.html> เพื่อดูและทดลอง API ที่มีอยู่ หรือดู OpenAPI JSON ที่ <http://localhost:8080/v3/api-docs> หลังแก้โค้ดให้ rebuild แอปด้วย `docker compose up -d --build --wait app`

โลโก้ทีมอัปโหลดผ่าน `PUT /api/v1/teams/{id}/logo` แบบ `multipart/form-data` ส่วน `file` (PNG/JPEG ไม่เกิน 2 MB) และดูรูปจาก `logoUrl` ที่ API ตอบ Docker เก็บรูปใน volume `logo_data` ซึ่งยังอยู่หลัง `docker compose down`; หากรันจาก IDE รูปอยู่ที่ `uploads/logos` ในเครื่อง รายละเอียดและข้อจำกัดอยู่ในคู่มือโมดูลด้านล่าง

อ่านวิธี setup, รันจาก IDE, ทดสอบ และรายละเอียด Team API ได้ใน [คู่มือโมดูลทีมและผู้เล่น (คนที่ 2)](doc/team-player-module-guide.md)
