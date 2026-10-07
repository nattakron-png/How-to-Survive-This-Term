# HowToSurviveThisTerm
## ระบบจัดการสายแข่ง E-Sport

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

อ่านวิธีรันจาก IDE ทดสอบ และแก้ปัญหาได้ที่ [คู่มือ setup](doc/setup.md)

API ที่พัฒนาแล้ว: [Team API](doc/team-api.md) สำหรับจัดการทีมและผู้เล่นในทีมตาม schema ปัจจุบัน
