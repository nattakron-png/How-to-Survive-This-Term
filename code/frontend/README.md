# Frontend (Vue 3 + Vite)

หน้าผู้ชม (`/`, `/tournaments`, หน้ารายละเอียดทัวร์และแมตช์) อ่าน backend จริงผ่าน Vite proxy; ฝั่ง Admin ยังใช้ข้อมูลตัวอย่างใน `src/mock/`

## รัน
```bash
cd frontend
npm install
npm run dev
```
เปิด http://localhost:5173

หน้า `/tournaments` เรียก `GET /api/v1/tournaments` ผ่าน Vite proxy ไปยัง backend ที่ `http://127.0.0.1:8080` จึงต้องเปิด backend local ก่อน หากฐานข้อมูลยังไม่มีรายการ หน้าจะแสดงรายการว่างตามข้อมูลจริง ข้อมูลชื่อเกมบนการ์ดและตัวกรองยังใช้ `src/mock/games.js` ชั่วคราวจนมี API รายชื่อเกม

หน้ารายละเอียดเรียก `GET /api/v1/tournaments/{id}/teams` เพื่ออ่านชื่อทีมและ roster snapshot รายทัวร์, GET แมตช์/ผล หรือเกม Free Fire/คะแนนตาม format; หน้าแมตช์อ่านรายชื่อผู้เล่นจาก snapshot เดียวกัน หน้าแรกอ่านรายการและแมตช์จาก API จริง หากรัน Vite ใน Docker ให้ตั้ง `VITE_API_PROXY_TARGET=http://host.docker.internal:8080` ปัจจุบัน API แมตช์/Free Fire ยังส่งชื่อทีมสด จึงให้ frontend ใช้ชื่อ snapshot เมื่อมี ID ทีมตรงกัน; หากทีมไม่ได้อยู่ใน snapshot จะใช้ชื่อจาก response เดิม

`npm run build` ตรวจว่า frontend build ได้ แต่ Dockerfile ที่ root ยังไม่รวม Vue dist เข้ากับ Spring Boot ดังนั้น Railway ยังแสดง 404 ที่ `/` จนกว่าจะจัดขั้นตอน build/deploy frontend

## โครงสร้าง
- `src/views/` — แต่ละหน้า
- `src/components/` — ชิ้นส่วนที่ใช้ซ้ำ
- `src/mock/` — ข้อมูลตัวอย่าง
- `src/api/` — ตัวเรียก backend สำหรับหน้าที่เริ่มใช้ข้อมูลจริง
- `src/router/index.js` — กำหนด URL ของแต่ละหน้า
