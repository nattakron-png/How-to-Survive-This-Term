# คู่มือโมดูลรูปแบบการแข่ง (คนที่ 4)

โมดูลนี้สร้าง "ตารางการแข่ง" ของรายการ แบบแพ้คัดออกสร้างสายการแข่งพร้อม `next_match_id` ส่วนแบบเก็บคะแนน (Free Fire) สร้างเกมย่อยตามจำนวนเกม ระบบเลือกวิธีสร้างด้วย **Strategy Pattern** ตามรูปแบบของรายการ นี่คือ Pattern หลักที่ต้องอธิบายตอนนำเสนอ

ตอนนี้ทำแบบแพ้คัดออกได้ทันที เพราะใช้ตาราง `matches` (V7) ที่มีอยู่แล้ว ส่วนแบบเก็บคะแนนต้องรอ V10 ของ Free Fire

---

## 1. ขอบเขตงาน

| Layer | ไฟล์ |
| --- | --- |
| Enum | `domain/enums/TournamentFormat.java` (ถ้าคนทำ V9 สร้างแล้ว ให้ใช้ของเขา) |
| Strategy | `service/format/FormatStrategy.java`, `SingleEliminationStrategy.java`, `PointsStrategy.java` (หลัง V10) |
| Service | `service/ScheduleService.java` + `service/impl/ScheduleServiceImpl.java` |
| Repository | เพิ่มเมธอดใน `MatchRepository`, สร้าง `TournamentRepository` และ `TournamentTeamRepository` ถ้ายังไม่มี |
| DTO + Mapper | `MatchResponse`, `ScheduleResponse`, `MatchMapper` |
| Controller | `ScheduleController` (สร้างตาราง + ดูแมตช์ของรายการ), `MatchController` (`GET /api/v1/matches`) |
| Test | `SingleEliminationStrategyTest`, `ScheduleServiceImplTest` |
| Frontend | หน้าสายการแข่ง, ตารางคะแนนรวม Free Fire, ปุ่มสร้างตารางการแข่งในหน้า Admin |

### API ที่ต้องมี

| Method | Endpoint | หน้าที่ | Status |
| --- | --- | --- | --- |
| POST | `/api/v1/tournaments/{id}/schedule` | สร้างตารางการแข่งตามรูปแบบของรายการ | 201 / 404 / 409 |
| GET | `/api/v1/tournaments/{id}/matches` | ดูแมตช์ทั้งหมดของรายการ เรียงตามรอบ | 200 / 404 |
| GET | `/api/v1/matches?page=0&size=20` | รายการแมตช์ทั้งหมด (บรีฟข้อ 30) | 200 |
| GET | `/api/v1/matches/{id}` | ดูแมตช์เดียว | 200 / 404 |

---

## 2. Setup บนเครื่อง (VS Code + Docker)

1. ติดตั้ง Docker Desktop (โหมด WSL 2), JDK 21 และส่วนขยาย **Extension Pack for Java** + **Spring Boot Extension Pack** ใน VS Code
2. ดึงโค้ดล่าสุดเข้า Branch ตัวเอง

   ```powershell
   git config user.email "อีเมลที่ผูกกับ-github"
   git checkout <Branch ของตัวเอง>
   git pull origin develop
   ```

3. เปิดฐานข้อมูล

   ```powershell
   Copy-Item .env.example .env
   docker compose up -d --wait db
   ```

   ถ้าขึ้น error เรื่อง port 5432 (เจอบ่อยบน Windows) ให้แก้ `DB_PORT=5433` ใน `.env` แล้วรันใหม่
4. สร้าง `.vscode/launch.json` เพื่อให้แอปอ่านค่าจาก `.env`

   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Tournament (local)",
         "request": "launch",
         "mainClass": "com.example.tournament.TournamentApplication",
         "envFile": "${workspaceFolder}/.env",
         "env": { "DB_HOST": "localhost" }
       }
     ]
   }
   ```

5. กด F5 แล้วเปิด `http://localhost:8080/actuator/health` ต้องได้ `{"status":"UP"}`

วางโค้ดแล้วกด `Shift+Alt+O` ให้ VS Code เติม import เอง โค้ดในคู่มือนี้ตัดบรรทัด import ออกเพื่อให้สั้น

---

## 3. ต้องตกลงกับเพื่อนก่อน

| เรื่อง | คุยกับใคร | ข้อตกลง |
| --- | --- | --- |
| `match_number` | ณัฐกร (ผลการแข่ง) | นับใหม่ในแต่ละรอบ (1, 2, 3…) ผู้ชนะจากแมตช์เลขคี่ไปช่อง A ของแมตช์ถัดไป เลขคู่ไปช่อง B `BracketProgressionListener` ของณัฐกรใช้กติกานี้ ถ้าสร้างสายคนละแบบ ผู้ชนะจะลงผิดช่อง |
| ค่า `status` ของแมตช์ | ณัฐกร | `PENDING` (รอคู่แข่ง), `SCHEDULED` (รู้ทีมครบแล้ว), `COMPLETED` (มีผลแล้ว) |
| ค่า `status` ของรายการ | คนทำ Tournament + คณิศร | `UPCOMING` → `ONGOING` → `COMPLETED` ให้เช็กใน V5 ว่าค่าเริ่มต้นของคอลัมน์ `status` คืออะไร แล้วใช้ให้ตรงกัน |
| `tournaments.format` | คนทำ V9 | ก่อนมี V9 ให้ใช้ `SINGLE_ELIMINATION` ไปก่อน หลัง V9 เปลี่ยนมาอ่านจาก `tournament.getFormat()` |
| `TournamentRepository`, `TournamentTeamRepository` | คนทำ Tournament | ถ้ายังไม่มีใครสร้าง ให้สร้างแบบเล็กที่สุดแล้วบอกเขา อย่าสร้างซ้ำกัน |
| `MatchRepository` | ณัฐกร | ณัฐกรสร้างไฟล์ไว้แล้ว ให้เพิ่มเมธอดในไฟล์เดิม ไม่ต้องสร้างใหม่ |

---

## 4. วิธีสร้างสายแบบแพ้คัดออก

1. **จัดลำดับทีม:** เรียงตาม `joined_at` ใน `tournament_teams` ทีมที่เข้าร่วมก่อนได้ seed ดีกว่า
2. **ขนาดสาย:** เลขกำลังสองที่ไม่น้อยกว่าจำนวนทีม เช่น 5 ทีม → 8 ช่อง จำนวนบาย = 8 − 5 = 3
3. **จับคู่รอบแรกแบบ seed มาตรฐาน:** ลำดับช่องของสาย 8 ทีมคือ `1, 8, 4, 5, 2, 7, 3, 6` ได้คู่ 1v8, 4v5, 2v7, 3v6 ทีม seed 1 กับ 2 จึงพบกันได้เร็วที่สุดแค่นัดชิง ช่องที่เลข seed เกินจำนวนทีมคือบาย
4. **สร้างแมตช์ทุกรอบไว้ก่อน:** รอบ r แมตช์ k ส่งผู้ชนะไปรอบ r+1 แมตช์ ⌈k/2⌉
5. **จัดการบายทันที:** ไม่สร้างแมตช์ที่มีทีมเดียว ส่งทีมนั้นไปรอบ 2 เลย (เลขคี่ → ช่อง A, เลขคู่ → ช่อง B) ใน `matches` จึงไม่มี NULL ที่แปลว่าบาย NULL แปลว่า "รอผู้ชนะ" อย่างเดียว
6. **บันทึกจากนัดชิงลงมา:** แมตช์ปลายทางต้องถูก save ก่อน แมตช์ที่ชี้มาหามันถึงจะอ้าง `next_match_id` ได้

ตัวอย่าง 5 ทีม: รอบแรกมีแข่งจริงคู่เดียว (seed 4 พบ 5) รอบรองชนะเลิศมี seed 1 รอผู้ชนะคู่นั้น และ seed 2 พบ seed 3 จากนั้นเป็นนัดชิง รวม 4 แมตช์ (จำนวนทีม − 1 เสมอ)

---

## 5. โค้ด เรียงตามลำดับ commit

### Commit 1: Enum รูปแบบการแข่ง

`domain/enums/TournamentFormat.java`

```java
package com.example.tournament.domain.enums;

public enum TournamentFormat {
    SINGLE_ELIMINATION, POINTS
}
```

`feat: add TournamentFormat enum`

### Commit 2: Repository

เพิ่มใน `repository/MatchRepository.java` (ไฟล์ของณัฐกร)

```java
public interface MatchRepository extends JpaRepository<Match, Long> {
    boolean existsByTournamentId(Long tournamentId);
    List<Match> findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(Long tournamentId);
}
```

ถ้ายังไม่มี ให้สร้าง `repository/TournamentRepository.java` และ `repository/TournamentTeamRepository.java`

```java
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}
```

```java
public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, TournamentTeamId> {
    List<TournamentTeam> findByTournamentIdOrderByJoinedAtAsc(Long tournamentId);
}
```

ชื่อเมธอดอ้างฟิลด์ใน Entity (`tournament`, `joinedAt`) ถ้า Entity ของคณิศรตั้งชื่อฟิลด์ต่างจากนี้ ให้แก้ชื่อเมธอดตาม

`feat: add repository queries for schedule generation`

### Commit 3: Strategy interface

`service/format/FormatStrategy.java`

```java
package com.example.tournament.service.format;

public interface FormatStrategy {

    TournamentFormat format();

    /** สร้างตารางการแข่งและบันทึกลงฐานข้อมูล คืนจำนวนแมตช์หรือเกมที่สร้าง */
    int createSchedule(Tournament tournament, List<Team> teamsInSeedOrder);
}
```

`feat: add FormatStrategy interface`

### Commit 4: SingleEliminationStrategy

`service/format/SingleEliminationStrategy.java`

แยก `buildBracket()` เป็นเมธอดที่ไม่แตะฐานข้อมูล จะได้เขียน Unit Test ได้โดยไม่ต้อง mock อะไรเลย

```java
package com.example.tournament.service.format;

@Component
public class SingleEliminationStrategy implements FormatStrategy {

    private final MatchRepository matches;

    public SingleEliminationStrategy(MatchRepository matches) {
        this.matches = matches;
    }

    @Override
    public TournamentFormat format() {
        return TournamentFormat.SINGLE_ELIMINATION;
    }

    @Override
    public int createSchedule(Tournament tournament, List<Team> teamsInSeedOrder) {
        List<Match> bracket = buildBracket(tournament, teamsInSeedOrder);
        matches.saveAll(bracket); // เรียงจากนัดชิงลงมาแล้ว
        return bracket.size();
    }

    /** สร้างสายในหน่วยความจำ ผลลัพธ์เรียงจากนัดชิงลงมา */
    public List<Match> buildBracket(Tournament tournament, List<Team> teams) {
        int size = 2;
        while (size < teams.size()) {
            size *= 2;
        }
        int rounds = Integer.numberOfTrailingZeros(size);
        LocalDateTime now = LocalDateTime.now();

        // 1. สร้างแมตช์ทุกตำแหน่งของทุกรอบ
        List<List<Match>> byRound = new ArrayList<>();
        for (int r = 1; r <= rounds; r++) {
            List<Match> round = new ArrayList<>();
            for (int k = 1; k <= (size >> r); k++) {
                Match match = new Match();
                match.setTournament(tournament);
                match.setRoundNumber(r);
                match.setMatchNumber(k);
                match.setStatus(MatchStatus.PENDING.name());
                match.setCreatedAt(now);
                round.add(match);
            }
            byRound.add(round);
        }

        // 2. ผูก next_match: รอบ r แมตช์ k → รอบ r+1 แมตช์ ceil(k/2)
        for (int r = 0; r < rounds - 1; r++) {
            List<Match> current = byRound.get(r);
            List<Match> next = byRound.get(r + 1);
            for (int k = 0; k < current.size(); k++) {
                current.get(k).setNextMatch(next.get(k / 2));
            }
        }

        // 3. ใส่ทีมรอบแรกตามลำดับ seed มาตรฐาน และจัดการบาย
        int[] order = seedOrder(size);
        Set<Match> byes = new HashSet<>();
        List<Match> firstRound = byRound.get(0);
        for (int k = 0; k < firstRound.size(); k++) {
            Match match = firstRound.get(k);
            Team teamA = teamAtSeed(teams, order[2 * k]);
            Team teamB = teamAtSeed(teams, order[2 * k + 1]);
            if (teamA != null && teamB != null) {
                match.setTeamA(teamA);
                match.setTeamB(teamB);
                match.setStatus(MatchStatus.SCHEDULED.name());
            } else {
                // บาย: ส่งทีมที่มีไปรอบถัดไปทันที และไม่บันทึกแมตช์นี้
                placeInNextMatch(match, teamA != null ? teamA : teamB);
                byes.add(match);
            }
        }

        // 4. เรียงจากนัดชิงลงมา เพื่อให้ save แมตช์ปลายทางก่อน
        List<Match> result = new ArrayList<>();
        for (int r = rounds - 1; r >= 0; r--) {
            for (Match match : byRound.get(r)) {
                if (!byes.contains(match)) {
                    result.add(match);
                }
            }
        }
        return result;
    }

    /** เลขคี่ไปช่อง A เลขคู่ไปช่อง B ตรงกับ BracketProgressionListener */
    private static void placeInNextMatch(Match from, Team team) {
        Match next = from.getNextMatch();
        if (from.getMatchNumber() % 2 == 1) {
            next.setTeamA(team);
        } else {
            next.setTeamB(team);
        }
        if (next.getTeamA() != null && next.getTeamB() != null) {
            next.setStatus(MatchStatus.SCHEDULED.name());
        }
    }

    /** ลำดับ seed ในสาย เช่น 8 ทีม → 1, 8, 4, 5, 2, 7, 3, 6 */
    static int[] seedOrder(int size) {
        int[] order = {1};
        while (order.length < size) {
            int length = order.length * 2;
            int[] next = new int[length];
            for (int i = 0; i < order.length; i++) {
                next[2 * i] = order[i];
                next[2 * i + 1] = length + 1 - order[i];
            }
            order = next;
        }
        return order;
    }

    private static Team teamAtSeed(List<Team> teams, int seed) {
        return seed <= teams.size() ? teams.get(seed - 1) : null;
    }
}
```

`feat: implement single elimination strategy with byes`

### Commit 5: Test ของ Strategy

`test/backend/java/com/example/tournament/service/format/SingleEliminationStrategyTest.java`

```java
package com.example.tournament.service.format;

class SingleEliminationStrategyTest {

    SingleEliminationStrategy strategy = new SingleEliminationStrategy(null); // buildBracket ไม่ใช้ repository
    Tournament tournament = new Tournament();

    @Test
    void twoTeamsMakeOneFinal() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(2));

        assertEquals(1, bracket.size());
        assertNull(bracket.get(0).getNextMatch());
        assertEquals("SCHEDULED", bracket.get(0).getStatus());
    }

    @Test
    void eightTeamsMakeSevenMatchesWithStandardSeeding() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(8));

        assertEquals(7, bracket.size());
        assertEquals(4, countRound(bracket, 1));
        Match first = find(bracket, 1, 1);
        assertEquals(1L, first.getTeamA().getId());
        assertEquals(8L, first.getTeamB().getId());
    }

    @Test
    void fiveTeamsGiveThreeByes() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(5));

        assertEquals(4, bracket.size()); // จำนวนทีม − 1
        assertEquals(1, countRound(bracket, 1));

        Match onlyFirstRound = find(bracket, 1, 2);
        assertEquals(4L, onlyFirstRound.getTeamA().getId());
        assertEquals(5L, onlyFirstRound.getTeamB().getId());

        Match semi1 = find(bracket, 2, 1);
        assertEquals(1L, semi1.getTeamA().getId());
        assertNull(semi1.getTeamB());
        assertEquals("PENDING", semi1.getStatus());

        Match semi2 = find(bracket, 2, 2);
        assertEquals(2L, semi2.getTeamA().getId());
        assertEquals(3L, semi2.getTeamB().getId());
        assertEquals("SCHEDULED", semi2.getStatus());
    }

    @Test
    void finalComesFirstSoItIsSavedFirst() {
        List<Match> bracket = strategy.buildBracket(tournament, teams(8));
        assertNull(bracket.get(0).getNextMatch());
    }

    @Test
    void seedOrderForEight() {
        assertArrayEquals(new int[] {1, 8, 4, 5, 2, 7, 3, 6}, SingleEliminationStrategy.seedOrder(8));
    }

    private static List<Team> teams(int count) {
        List<Team> teams = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            Team team = new Team();
            team.setId(id);
            teams.add(team);
        }
        return teams;
    }

    private static long countRound(List<Match> bracket, int round) {
        return bracket.stream().filter(m -> m.getRoundNumber() == round).count();
    }

    private static Match find(List<Match> bracket, int round, int number) {
        return bracket.stream()
                .filter(m -> m.getRoundNumber() == round && m.getMatchNumber() == number)
                .findFirst()
                .orElseThrow();
    }
}
```

`test: add bracket tests for 2, 5 and 8 teams`

### Commit 6: DTO + Mapper

`dto/response/MatchResponse.java`

```java
package com.example.tournament.dto.response;

public record MatchResponse(
        Long id,
        Long tournamentId,
        Integer roundNumber,
        Integer matchNumber,
        Long teamAId,
        String teamAName,
        Long teamBId,
        String teamBName,
        Long nextMatchId,
        LocalDateTime scheduledAt,
        String status) {
}
```

`dto/response/ScheduleResponse.java`

```java
package com.example.tournament.dto.response;

public record ScheduleResponse(Long tournamentId, String format, int created) {
}
```

`mapper/MatchMapper.java`

```java
package com.example.tournament.mapper;

@Component
public class MatchMapper {

    public MatchResponse toResponse(Match match) {
        Team a = match.getTeamA();
        Team b = match.getTeamB();
        Match next = match.getNextMatch();
        return new MatchResponse(
                match.getId(),
                match.getTournament().getId(),
                match.getRoundNumber(),
                match.getMatchNumber(),
                a == null ? null : a.getId(),
                a == null ? null : a.getName(),
                b == null ? null : b.getId(),
                b == null ? null : b.getName(),
                next == null ? null : next.getId(),
                match.getScheduledAt(),
                match.getStatus());
    }
}
```

`feat: add match response DTO and mapper`

### Commit 7: ScheduleService (จุดที่ใช้ Strategy)

`service/ScheduleService.java`

```java
package com.example.tournament.service;

public interface ScheduleService {
    ScheduleResponse createSchedule(Long tournamentId);
    List<MatchResponse> listMatches(Long tournamentId);
}
```

`service/impl/ScheduleServiceImpl.java`

```java
package com.example.tournament.service.impl;

@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final TournamentRepository tournaments;
    private final TournamentTeamRepository tournamentTeams;
    private final MatchRepository matches;
    private final MatchMapper mapper;
    private final Map<TournamentFormat, FormatStrategy> strategies;

    // Spring ส่ง FormatStrategy ทุกตัวที่เป็น @Component เข้ามาเอง
    public ScheduleServiceImpl(TournamentRepository tournaments, TournamentTeamRepository tournamentTeams,
            MatchRepository matches, MatchMapper mapper, List<FormatStrategy> strategyList) {
        this.tournaments = tournaments;
        this.tournamentTeams = tournamentTeams;
        this.matches = matches;
        this.mapper = mapper;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(FormatStrategy::format, Function.identity()));
    }

    @Override
    public ScheduleResponse createSchedule(Long tournamentId) {
        Tournament tournament = findTournament(tournamentId);

        if (!"UPCOMING".equals(tournament.getStatus())) {
            throw new BusinessException("Schedule can only be created before the tournament starts");
        }
        if (matches.existsByTournamentId(tournamentId)) {
            throw new BusinessException("Schedule already exists for this tournament");
        }

        List<Team> teams = tournamentTeams.findByTournamentIdOrderByJoinedAtAsc(tournamentId).stream()
                .map(TournamentTeam::getTeam)
                .toList();
        if (teams.size() < 2) {
            throw new BusinessException("At least 2 teams are required");
        }

        // หลังมี V9: TournamentFormat format = tournament.getFormat();
        TournamentFormat format = TournamentFormat.SINGLE_ELIMINATION;
        FormatStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new BusinessException("Unsupported tournament format: " + format);
        }

        int created = strategy.createSchedule(tournament, teams);
        tournament.setStatus("ONGOING");
        return new ScheduleResponse(tournamentId, format.name(), created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchResponse> listMatches(Long tournamentId) {
        findTournament(tournamentId);
        return matches.findByTournamentIdOrderByRoundNumberAscMatchNumberAsc(tournamentId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private Tournament findTournament(Long id) {
        return tournaments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
    }
}
```

`feat: add ScheduleService selecting strategy by format`

ตอนนำเสนอ ชี้ที่ constructor ตัวนี้: `ScheduleServiceImpl` รู้จักแค่ interface `FormatStrategy` (DIP) ถ้าเพิ่มรูปแบบใหม่ก็แค่สร้างคลาสใหม่ที่ implement `FormatStrategy` ไม่ต้องแก้ Service นี้เลย (Open/Closed)

### Commit 8: Test ของ ScheduleService

`test/backend/java/com/example/tournament/service/impl/ScheduleServiceImplTest.java`

```java
package com.example.tournament.service.impl;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock TournamentRepository tournaments;
    @Mock TournamentTeamRepository tournamentTeams;
    @Mock MatchRepository matches;
    @Mock FormatStrategy singleElimination;

    ScheduleServiceImpl service;
    Tournament tournament;

    @BeforeEach
    void setUp() {
        when(singleElimination.format()).thenReturn(TournamentFormat.SINGLE_ELIMINATION);
        service = new ScheduleServiceImpl(tournaments, tournamentTeams, matches, new MatchMapper(),
                List.of(singleElimination));
        tournament = new Tournament();
        tournament.setStatus("UPCOMING");
    }

    @Test
    void rejectsWhenTournamentAlreadyStarted() {
        tournament.setStatus("ONGOING");
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        assertThrows(BusinessException.class, () -> service.createSchedule(1L));
    }

    @Test
    void rejectsWhenScheduleAlreadyExists() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(matches.existsByTournamentId(1L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createSchedule(1L));
    }

    @Test
    void rejectsWhenFewerThanTwoTeams() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentTeams.findByTournamentIdOrderByJoinedAtAsc(1L)).thenReturn(List.of(joined(10L)));
        assertThrows(BusinessException.class, () -> service.createSchedule(1L));
    }

    @Test
    void usesStrategyAndStartsTournament() {
        when(tournaments.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentTeams.findByTournamentIdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(joined(10L), joined(20L)));
        when(singleElimination.createSchedule(eq(tournament), anyList())).thenReturn(1);

        ScheduleResponse response = service.createSchedule(1L);

        assertEquals(1, response.created());
        assertEquals("ONGOING", tournament.getStatus());
        verify(singleElimination).createSchedule(eq(tournament), anyList());
    }

    private static TournamentTeam joined(Long teamId) {
        Team team = new Team();
        team.setId(teamId);
        TournamentTeam tt = new TournamentTeam();
        tt.setTeam(team);
        return tt;
    }
}
```

`test: add unit tests for ScheduleService`

### Commit 9: Controller

`controller/api/ScheduleController.java`

```java
package com.example.tournament.controller.api;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @PostMapping("/schedule")
    public ResponseEntity<ScheduleResponse> create(@PathVariable Long tournamentId) {
        ScheduleResponse body = service.createSchedule(tournamentId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tournaments/{id}/matches").buildAndExpand(tournamentId).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/matches")
    public List<MatchResponse> matches(@PathVariable Long tournamentId) {
        return service.listMatches(tournamentId);
    }
}
```

`GET /api/v1/matches` ตามบรีฟข้อ 30 ต้องผ่าน Service เสมอ เพราะใบงานห้าม Controller เรียก Repository ตรง

`service/MatchService.java`

```java
package com.example.tournament.service;

public interface MatchService {
    Page<MatchResponse> list(Pageable pageable);
    MatchResponse get(Long id);
}
```

`service/impl/MatchServiceImpl.java`

```java
package com.example.tournament.service.impl;

@Service
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matches;
    private final MatchMapper mapper;

    public MatchServiceImpl(MatchRepository matches, MatchMapper mapper) {
        this.matches = matches;
        this.mapper = mapper;
    }

    @Override
    public Page<MatchResponse> list(Pageable pageable) {
        return matches.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    public MatchResponse get(Long id) {
        return matches.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }
}
```

`controller/api/MatchController.java` ใช้ `PageResponse` ที่วัชรพลสร้างไว้แล้ว

```java
package com.example.tournament.controller.api;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matches;

    public MatchController(MatchService matches) {
        this.matches = matches;
    }

    @GetMapping
    public PageResponse<MatchResponse> list(
            @PageableDefault(size = 20, sort = {"roundNumber", "matchNumber"}) Pageable pageable) {
        return PageResponse.from(matches.list(pageable));
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable Long id) {
        return matches.get(id);
    }
}
```

`feat: add schedule and match API endpoints`

---

## 6. แบบเก็บคะแนน (Free Fire) หลังมี V10

รอให้ตาราง `free_fire_games` ถูกสร้างก่อน แล้วค่อยทำ `PointsStrategy`

- `format()` คืน `TournamentFormat.POINTS`
- `createSchedule()` สร้างเกมย่อย `game_number` 1 ถึง `tournament.getTotalGames()` ทุกเกมสถานะ `SCHEDULED` แล้วบันทึกด้วย Repository ของ `free_fire_games`
- ไม่ต้องแก้ `ScheduleServiceImpl` เลย Spring จะส่ง `PointsStrategy` เข้ามาใน `List<FormatStrategy>` เอง (นี่คือจุดที่โชว์ Open/Closed ได้ชัดที่สุด)
- ใน `ScheduleServiceImpl` เปลี่ยนบรรทัด format เป็น `tournament.getFormat()`
- เพิ่มเทสต์: รายการ Free Fire 10 เกม → ได้เกมย่อย 10 เกมเลข 1–10
- ตารางคะแนนรวมคำนวณจากผลของเกมย่อย ส่วนการกรอกผลเป็นของณัฐกร หน้าตารางคะแนนรวมเป็นของคุณ ให้ตกลง API อ่านคะแนนรวมกับณัฐกรก่อนทำหน้า

`feat: implement points strategy for Free Fire`

---

## 7. เอกสารและ Diagram ที่เป็นของคุณ

- **Class Diagram:** ระบุตำแหน่ง Strategy (`FormatStrategy` → `SingleEliminationStrategy`, `PointsStrategy`, ใช้งานโดย `ScheduleServiceImpl`)
- **State Diagram:** ของ Match (`PENDING → SCHEDULED → COMPLETED`) และ Tournament (`UPCOMING → ONGOING → COMPLETED`)
- **Sequence Diagram:** การสร้างตารางการแข่งทั้งสองรูปแบบ
- **`doc/design-patterns.md`:** แถว Strategy ระบุปัญหาที่แก้ ไฟล์ที่ใช้ และเหตุผล
- **`doc/solid-analysis.md`:** หลัก O และ D ใช้ `ScheduleServiceImpl` เป็นตัวอย่างได้

---

## 8. Checklist ก่อนเปิด PR

- [ ] `./mvnw test` หรือเทสต์ใน VS Code ผ่านทั้งหมด
- [ ] ไม่มี Controller เรียก Repository ตรง
- [ ] สร้างสาย 5 ทีมผ่าน API จริงแล้วได้ 4 แมตช์ และ `GET /tournaments/{id}/matches` แสดงถูก
- [ ] สร้างซ้ำครั้งที่สองได้ 409
- [ ] คุยกับณัฐกรแล้วว่ากติกาเลขคี่/เลขคู่ตรงกัน
- [ ] PR เข้า `develop` มีเพื่อน Approve อย่างน้อย 1 คน
