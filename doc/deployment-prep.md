# เตรียม Deploy บน Railway

ทีมเลือก **Railway** สำหรับแอป Spring Boot และ **Railway PostgreSQL** เป็นฐานข้อมูลบน cloud. บรีฟ `Brief Form Chat (1).pdf` ระบุ Docker, Java 21, PostgreSQL และ Flyway แต่ไม่ได้บังคับชื่อผู้ให้บริการ; `doc/REMAINING-WORK.md` ระบุเป้าหมาย URL สาธารณะ. Backend ถูก deploy และตรวจ health แล้วบน V10; V11 และ frontend ใน working tree ยังไม่ได้ deploy

Railway Trial ปัจจุบันให้เครดิตเริ่มต้น **$5 ใช้ได้สูงสุด 30 วัน หรือจนเครดิตหมด** จากนั้นเป็น Free plan ตามเงื่อนไขของ Railway. ระยะเวลาที่ใช้ได้จริงอาจสั้นกว่า 30 วันตามการใช้ทรัพยากร จึงต้องตรวจสถานะและเครดิตคงเหลือในบัญชีของทีม ไม่ตั้งสมมติฐานว่าได้ 7 วันแน่นอน ([Railway Free Trial](https://docs.railway.com/pricing/free-trial))

## สร้างบริการ

1. สร้าง Railway Project และเพิ่ม `PostgreSQL` จาก `+ New` → `Database` → `PostgreSQL`. ให้ DB อยู่ใน private network; ไม่ต้องเปิด Public Access เพื่อให้แอปใน project เดียวกันเชื่อมต่อ ([Railway PostgreSQL](https://docs.railway.com/databases/postgresql))
2. เพิ่ม service แอปจาก GitHub repo นี้ เลือก branch ที่ทีมจะ deploy และตรวจ build log ว่า Railway ใช้ `Dockerfile` ที่ root. `Dockerfile` ปัจจุบัน build ด้วย Maven/Java 21 แล้วรัน JAR ใต้ user `app` ([Railway Dockerfiles](https://docs.railway.com/builds/dockerfiles))
3. ใน service แอป ตั้ง Variables ด้านล่างโดยใช้ **Reference Variable** ไปยัง service PostgreSQL (ตัวอย่างชื่อ service คือ `Postgres`; ถ้าตั้งชื่ออื่น ให้เปลี่ยนส่วนหน้าจุดตามชื่อจริง) ([Railway Variables](https://docs.railway.com/variables)):

   | ตัวแปรของแอป | ค่าใน Railway |
   |---|---|
   | `DB_HOST` | `${{Postgres.PGHOST}}` |
   | `DB_PORT` | `${{Postgres.PGPORT}}` |
   | `DB_NAME` | `${{Postgres.PGDATABASE}}` |
   | `DB_USERNAME` | `${{Postgres.PGUSER}}` |
   | `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |

   `application.properties` สร้าง JDBC URL จาก 5 ค่านี้อยู่แล้ว. ไม่ต้องนำ `DATABASE_URL` รูปแบบ `postgresql://...` ไปใส่แทน JDBC URL. Railway กำหนด `PORT` ให้บริการเว็บ และแอปอ่าน `APP_PORT` → `PORT` → 8080 ตามลำดับ; บน Railway อย่าตั้ง `APP_PORT` ให้ชนกับ `PORT` ([Railway Healthchecks](https://docs.railway.com/deployments/healthchecks))
4. ตั้ง Healthcheck Path ของ service แอปเป็น `/actuator/health`. หลังแอปพร้อมจึงสร้าง public domain ให้ **เฉพาะแอป** และทดสอบ `https://<domain>/actuator/health`. Compose `db` และ volume `postgres_data` เป็นสภาพ local; ไม่ต้อง deploy `compose.yaml` ทั้งไฟล์บน Railway ([Railway Compose guide](https://docs.railway.com/guides/docker-compose))

ห้ามคัดลอก `.env` ของเครื่องพัฒนาไป Railway หรือใส่รหัสผ่านจริงใน Git; `.env.example` เป็นเพียงตัวอย่าง local. ควรเลือก branch deploy ที่ทีมตรวจสอบแล้วเพื่อไม่ให้ทุกการ push งานระหว่างพัฒนากระทบบริการสาธารณะ

## สิ่งที่ต้องจัดการก่อนเปิดให้ใช้งานจริง

- **โลโก้:** ปัจจุบัน `LOGO_STORAGE_DIR` เขียนไฟล์ไว้ในเครื่องแอป. ต้องเลือกที่เก็บถาวรและทดสอบว่าไฟล์ยังอยู่หลัง redeploy. Railway Volume mount เป็น `root` แต่ Docker image นี้รัน user `app` แบบไม่ใช่ root; การ mount `/app/uploads/logos` ตรง ๆ อาจเขียนไม่ได้. คู่มือ Railway เสนอ `RAILWAY_RUN_UID=0` สำหรับ volume ซึ่งจะเปลี่ยนแอปให้รันเป็น root จึง **ยังไม่ตั้งค่านี้ล่วงหน้า**; ให้ทีมเลือกระหว่างการจัดสิทธิ์ volume ที่ทดสอบแล้วกับ object storage และปรับ implementation ให้ตรงกัน ([Railway Volumes](https://docs.railway.com/volumes))
- **สิทธิ์ Admin:** งาน Auth/การป้องกัน write API ยังเป็นงานค้างใน `doc/REMAINING-WORK.md`; ต้องตรวจให้เสร็จก่อนเผยแพร่ URL ที่แก้ข้อมูลได้
- **ฐานข้อมูล:** ให้ Flyway รันบน PostgreSQL ที่สร้างใหม่ก่อน ตรวจ migration และสุขภาพแอป; อย่าใช้ฐานข้อมูลพัฒนาท้องถิ่นเป็นฐาน cloud และเก็บข้อมูลสำคัญไว้ในแผน backup ของบริการ
- **หลัง deploy รุ่นถัดไป:** ตรวจ `/actuator/health`, API อ่าน/เขียนที่อนุญาต, การเก็บ DB/โลโก้หลัง restart, build/deploy logs และจด URL กับผลจริงใน README/PROGRESS. ผลที่ตรวจ backend V10 บน Railway ไม่ยืนยันว่า V11 หรือ frontend รุ่นใหม่ deploy ผ่านแล้ว

ยังไม่ต้องติดตั้ง Railway CLI ในเครื่องเพื่อทำขั้นตอนนี้; ใช้ Railway Dashboard ได้. `docker compose up` ใน README ยังคงเป็นวิธีรัน local
