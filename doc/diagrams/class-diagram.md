# Class Diagram: ภาพรวมระบบ

แบ่งเป็น 2 ส่วน ส่วนแรกคือ Entity (`domain/entity`) ส่วนที่สองคือชั้น Controller → Service → Repository ของทุกโมดูล รายละเอียดโมดูลสร้างตารางการแข่ง (Strategy Pattern) อยู่ใน [class-diagram-bracket.md](class-diagram-bracket.md)

## 1. Entity

```mermaid
classDiagram
    direction LR

    class Game {
        -Long id
        -String code
        -String name
        -Integer minPlayers
        -String logoUrl
        -Boolean isActive
        -LocalDateTime createdAt
    }
    class Team {
        -Long id
        -String name
        -String description
        -Game game
        -String logoUrl
        -LocalDateTime createdAt
        -List~Player~ players
        -List~TournamentTeam~ tournamentTeams
    }
    class Player {
        -Long id
        -String name
        -String role
        -String description
        -Team team
        -LocalDateTime createdAt
    }
    class Tournament {
        -Long id
        -String name
        -String description
        -LocalDate startDate
        -LocalDate endDate
        -TournamentStatus status
        -Game game
        -TournamentFormat format
        -String logoUrl
        -Short totalGames
        -Short pointsPerKill
        -LocalDateTime createdAt
        -List~TournamentTeam~ tournamentTeams
    }
    class TournamentTeam {
        -TournamentTeamId id
        -Tournament tournament
        -Team team
        -LocalDateTime joinedAt
        -String teamName
        -String teamDescription
        -String teamLogoUrl
    }
    class TournamentTeamId {
        <<Embeddable>>
        -Long tournamentId
        -Long teamId
    }
    class Match {
        -Long id
        -Tournament tournament
        -Team teamA
        -Team teamB
        -Integer roundNumber
        -Integer matchNumber
        -Match nextMatch
        -LocalDateTime scheduledAt
        -String status
        -LocalDateTime createdAt
    }
    class MatchResult {
        -Long id
        -Match match
        -Integer teamAScore
        -Integer teamBScore
        -Team winnerTeam
        -LocalDateTime createdAt
    }
    class TournamentPlacementPoint {
        -Tournament tournament
        -Short placement
        -Short points
    }
    class FreeFireGame {
        -Long id
        -Tournament tournament
        -Short gameNumber
        -LocalDateTime scheduledAt
        -String status
        -LocalDateTime createdAt
        -List~FreeFireGameResult~ results
    }
    class FreeFireGameResult {
        -Long id
        -FreeFireGame game
        -Tournament tournament
        -Team team
        -Short placement
        -Short kills
        -LocalDateTime createdAt
    }
    class User {
        -Long id
        -String username
        -String password
        -String role
        -LocalDateTime createdAt
    }

    class TournamentStatus {
        <<enumeration>>
        UPCOMING
        ONGOING
        COMPLETED
    }
    class TournamentFormat {
        <<enumeration>>
        SINGLE_ELIMINATION
        POINTS
    }
    class MatchStatus {
        <<enumeration>>
        PENDING
        SCHEDULED
        COMPLETED
    }

    Team "0..*" --> "0..1" Game
    Tournament "0..*" --> "0..1" Game
    Player "0..*" <--> "0..1" Team
    TournamentTeam "0..*" <--> "1" Tournament
    TournamentTeam "0..*" <--> "1" Team
    TournamentTeam *-- TournamentTeamId
    Match "0..*" --> "1" Tournament
    Match "0..*" --> "0..2" Team : teamA / teamB
    Match "0..*" --> "0..1" Match : nextMatch
    MatchResult "0..1" <--> "1" Match
    MatchResult "0..*" --> "1" Team : winnerTeam
    TournamentPlacementPoint "0..*" --> "1" Tournament
    FreeFireGame "0..*" --> "1" Tournament
    FreeFireGameResult "0..*" <--> "1" FreeFireGame
    FreeFireGameResult "0..*" --> "1" Team
    Tournament ..> TournamentStatus
    Tournament ..> TournamentFormat
    Match ..> MatchStatus
```

`Match.status` และ `FreeFireGame.status` ยังเก็บเป็น `String` (ค่าจาก `MatchStatus.X.name()`) ส่วน `Tournament` ใช้ Enum ด้วย `@Enumerated(EnumType.STRING)` แล้ว

## 2. ชั้น Controller → Service → Repository

แสดงเฉพาะ interface ของ Service และ dependency หลัก คลาส `*ServiceImpl` ทุกตัว implement interface ชื่อเดียวกัน (ยกเว้น `TournamentRosterSnapshotService` ที่เป็นคลาสเลย)

```mermaid
classDiagram
    direction LR

    class TournamentController
    class TeamController
    class TeamLogoController
    class LogoController
    class TeamMembershipController
    class PlayerController
    class TournamentRosterController
    class ScheduleController
    class MatchController
    class MatchResultController
    class FreeFireResultController

    class TournamentService {
        <<interface>>
        +create(request) TournamentResponse
        +update(id, request) TournamentResponse
        +delete(id)
        +getAll() List
        +searchByNameAndStatus(name, status) List
        +getPlacementPoints(id) List
    }
    class TeamService {
        <<interface>>
        +list(name, pageable) Page
        +create(request) TeamResponse
        +update(id, request) TeamResponse
        +delete(id)
    }
    class TeamLogoService {
        <<interface>>
        +upload(teamId, file) TeamResponse
    }
    class TeamMembershipService {
        <<interface>>
        +listPlayers(teamId, name, role, pageable) Page
        +addPlayer(teamId, playerId) TeamPlayerResponse
        +removePlayer(teamId, playerId)
    }
    class PlayerService {
        <<interface>>
        +list(name, pageable) Page
        +create(request) PlayerResponse
        +update(id, request) PlayerResponse
        +delete(id)
    }
    class TournamentTeamService {
        <<interface>>
        +addTeam(tournamentId, teamId)
        +removeTeam(tournamentId, teamId)
    }
    class TournamentRosterSnapshotService {
        +capturePlayers(tournamentId, teamId)
        +list(tournamentId) List
        +get(tournamentId, teamId) TournamentRosterResponse
    }
    class ScheduleService {
        <<interface>>
        +createSchedule(tournamentId) ScheduleResponse
        +listMatches(tournamentId) List
    }
    class MatchService {
        <<interface>>
        +list(pageable) Page
        +get(id) MatchResponse
    }
    class MatchResultService {
        <<interface>>
        +record(matchId, request) MatchResultResponse
        +get(matchId) MatchResultResponse
    }
    class FreeFireResultService {
        <<interface>>
        +record(gameId, request) FreeFireGameResultsResponse
        +getResults(gameId) FreeFireGameResultsResponse
        +standings(tournamentId) FreeFireStandingsResponse
        +listGames(tournamentId) List
    }

    class FileStorageService {
        <<interface>>
        +storeLogo(file) String
        +loadLogo(filename) StoredFile
        +deleteLogo(filename)
    }
    class LocalFileStorageService
    class TeamJoinRuleChain {
        +validate(team, tournament)
    }
    class TeamJoinRule {
        <<interface>>
        +validate(team, tournament)
        +setNext(rule)
    }
    class FormatStrategy {
        <<interface>>
        +format() TournamentFormat
        +createSchedule(tournament, teams) int
    }
    class PointsCalculator {
        +gamePoints(placement, kills, table, perKill) int
        +standings(participants, results, ...) List
    }
    class ApplicationEventPublisher {
        <<Spring>>
    }
    class BracketProgressionListener {
        +onResultRecorded(MatchResultRecordedEvent)
    }
    class FreeFireCompletionListener {
        +onGameRecorded(FreeFireGameRecordedEvent)
    }
    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
    }

    TournamentController --> TournamentService
    TeamController --> TeamService
    TeamLogoController --> TeamLogoService
    LogoController --> FileStorageService
    TeamMembershipController --> TeamMembershipService
    PlayerController --> PlayerService
    TournamentRosterController --> TournamentRosterSnapshotService
    ScheduleController --> ScheduleService
    MatchController --> MatchService
    MatchResultController --> MatchResultService
    FreeFireResultController --> FreeFireResultService

    TeamLogoService ..> FileStorageService
    FileStorageService <|.. LocalFileStorageService
    TournamentTeamService ..> TeamJoinRuleChain
    TournamentTeamService ..> TournamentRosterSnapshotService
    TeamJoinRuleChain o-- "8" TeamJoinRule
    ScheduleService ..> FormatStrategy
    FreeFireResultService ..> PointsCalculator
    MatchResultService ..> ApplicationEventPublisher
    FreeFireResultService ..> ApplicationEventPublisher
    ApplicationEventPublisher ..> BracketProgressionListener
    ApplicationEventPublisher ..> FreeFireCompletionListener
```

## Design Pattern ที่อยู่ในแผนภาพ

| Pattern | คลาสที่เกี่ยวข้อง | รายละเอียด |
| --- | --- | --- |
| Strategy | `FormatStrategy`, `SingleEliminationStrategy`, `PointsStrategy` | [class-diagram-bracket.md](class-diagram-bracket.md) |
| Chain of Responsibility | `TeamJoinRule` + 8 กฎ, `TeamJoinRuleChain` | [../design-patterns.md](../design-patterns.md) |
| Observer | `MatchResultRecordedEvent` → `BracketProgressionListener`, `FreeFireGameRecordedEvent` → `FreeFireCompletionListener` | [sequence-diagram-result.md](sequence-diagram-result.md) |
| Repository / DTO + Mapper | `*Repository` (Spring Data JPA), `dto/*`, `mapper/*` | |
