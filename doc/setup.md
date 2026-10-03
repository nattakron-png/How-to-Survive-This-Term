# Setup และรันโปรเจกต์

## เทคโนโลยี

Java 21, Spring Boot (เวอร์ชันตาม `pom.xml`), Maven, PostgreSQL 17 และ Flyway สำหรับ migration V1–V8 โครงสร้าง Controller, Service และ Repository พร้อมสำหรับพัฒนาต่อ แต่ business API และหน้าจอยังต้องสร้าง

## 1. รันทั้งหมดผ่าน Docker

ติดตั้ง Docker Desktop เปิดให้ Engine ทำงาน และใช้ Linux containers รันคำสั่งที่ root ของ repo

ครั้งแรกบน PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d --build --wait
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

บน macOS/Linux ใช้ `cp .env.example .env` แทน `Copy-Item` และ `curl http://localhost:8080/actuator/health` ตรวจสุขภาพ

ถ้ามี `.env` อยู่แล้วไม่ต้องคัดลอกทับ Docker Compose อ่าน `.env` อัตโนมัติ ไฟล์นี้ถูก ignore จาก Git ค่าเริ่มต้นใช้เฉพาะการพัฒนาในเครื่อง

Dockerfile build JAR ด้วย Maven + Java 21 ภายใน container ไม่ต้องติดตั้ง Java หรือ Maven ในเครื่อง แอปรอจน PostgreSQL พร้อม จากนั้น Flyway สร้าง schema และ Hibernate ตรวจ entity ด้วย `ddl-auto=validate`

| บริการ | การเข้าถึงจากเครื่อง |
|---|---|
| แอป | `http://localhost:8080` |
| Health รวมการเชื่อมต่อฐานข้อมูล | `http://localhost:8080/actuator/health` |
| PostgreSQL | `localhost:5432` |
| Database / Username | `tournament` / `tournament` |
| Password สำหรับ local | `tournament_local` |

ปรับค่าตารางได้ใน `.env` ภายใน Docker แอปติดต่อฐานข้อมูลด้วย host `db` และ port `5432` เสมอ

## 2. ทำงานประจำวันแบบ Docker

หลังแก้ Java, template, properties หรือ migration ให้ build แอปใหม่:

```bash
docker compose up -d --build --wait app
docker compose logs --tail=100 app
```

ดู log ต่อเนื่องด้วย `docker compose logs -f app` กด Ctrl+C เพื่อออกจากการดู log แอปจะยังทำงาน

หยุดบริการและเก็บข้อมูลฐานข้อมูล:

```bash
docker compose down
```

เริ่มอีกครั้ง:

```bash
docker compose up -d --wait
```

ข้อมูลอยู่ใน named volume `postgres_data` อย่าใช้ `docker compose down -v` เว้นแต่ตั้งใจลบข้อมูลฐานข้อมูล local ทั้งหมด

## 3. รันและ debug แอปจาก IDE โดยใช้ฐานข้อมูล Docker

ต้องมี **JDK 21** และตั้ง Project SDK / Java runtime ของ IDE เป็น 21 Maven Wrapper มีใน repo แล้ว ไม่ต้องติดตั้ง Maven แยก

หยุดแอป Docker เพื่อให้ port 8080 ว่าง และเริ่มเฉพาะฐานข้อมูล:

```bash
docker compose stop app
docker compose up -d --wait db
```

ถ้าใช้ค่าฐานข้อมูลเริ่มต้น รันจาก PowerShell ได้เลย:

```powershell
.\mvnw.cmd spring-boot:run
```

หรือเปิด `TournamentApplication.java` แล้ว Run / Debug จาก IDE บน macOS/Linux ใช้ `./mvnw spring-boot:run`

Spring Boot ไม่อ่าน `.env` อัตโนมัติเมื่อรันด้วย Maven หรือ IDE หากเปลี่ยนค่า ให้ตั้ง environment variables ใน Run Configuration หรือ Terminal เช่น:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:DB_NAME = "tournament"
$env:DB_USERNAME = "tournament"
$env:DB_PASSWORD = "ค่าที่ตั้งใน .env"
.\mvnw.cmd spring-boot:run
```

ตัวอย่าง port 5433 ใช้เมื่อเปลี่ยน `DB_PORT=5433` ใน `.env` แอปบนเครื่องใช้ host `localhost` ส่วน host `db` ใช้เฉพาะใน network ของ Docker

## 4. ทดสอบด้วยฐานข้อมูลแยก

```bash
docker compose --profile test build tests
docker compose --profile test run --rm tests
docker compose --profile test stop test-db
```

คำสั่ง `run` คืน exit code ของ Maven ถ้า tests ผ่านจะได้ `BUILD SUCCESS` ใช้ PostgreSQL อีกบริการชื่อ `test-db` ไม่เชื่อมฐานข้อมูลที่ใช้พัฒนา และไม่เปิด port สู่เครื่อง ข้อมูล test อยู่ใน tmpfs และไม่ถูกเก็บเมื่อ container หยุด ตอนนี้มี context-load integration test ตรวจการเริ่ม Spring Boot พร้อม Flyway และ JPA เพิ่ม tests ตาม business logic เมื่อพัฒนาฟีเจอร์

## 5. ดูฐานข้อมูล

ตัวอย่างใช้ค่าเริ่มต้น เปลี่ยน username/database ให้ตรง `.env` หากตั้งค่าเอง:

```bash
docker compose exec db psql -U tournament -d tournament
```

ภายใน psql ใช้ `\dt` ดูตาราง และ `\q` ออก หรือใช้ database extension ของ IDE ด้วยค่าจากตารางด้านบน

ตรวจ migration:

```bash
docker compose exec db psql -U tournament -d tournament -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

ควรเห็น migration V1–V8 ผ่านทั้งหมด เมื่อ schema ถูกใช้งานแล้ว ให้สร้าง migration เลขใหม่สำหรับการแก้ไข เช่น `V9__...sql` แทนการแก้ migration ที่ apply ไปแล้ว

## 6. ปัญหาที่พบบ่อย

- **Cannot connect to Docker daemon:** เปิด Docker Desktop รอ Engine พร้อม แล้วตรวจ `docker version`
- **Port already allocated:** เปลี่ยน `APP_PORT` หรือ `DB_PORT` ใน `.env` เช่น 8081 / 5433 แล้วรัน `docker compose up -d --wait` อีกครั้ง
- **Health ไม่ผ่าน:** ดู `docker compose logs --tail=150 app db` ตรวจ Flyway, schema validation และข้อมูลเชื่อมต่อฐานข้อมูล
- **เปลี่ยน username/password แล้ว login ไม่ได้:** ค่า `POSTGRES_*` ใช้สร้างฐานข้อมูลครั้งแรกเท่านั้น การแก้ `.env` ไม่เปลี่ยนบัญชีใน volume เดิม ให้ใช้ค่าเดิม หรือเปลี่ยนบัญชีใน PostgreSQL ก่อนแก้ config
- **เปิดหน้า `/` ได้ 404:** ยังไม่มีหน้าเว็บหลัก ใช้ `/actuator/health` ตรวจว่า environment พร้อม
- **เครื่องมี Java 24/26:** ใช้ Docker ได้เลย แต่เมื่อต้องการ debug จาก IDE ให้ติดตั้งและเลือก JDK 21 ให้ตรงกับทีม

ไฟล์ `.env` และ build output ไม่ต้อง commit ส่งเฉพาะโค้ดและ config ตัวอย่างที่ทีมใช้ร่วมกัน
