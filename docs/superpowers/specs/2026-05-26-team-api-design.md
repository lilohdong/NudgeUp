# Team 도메인 API 설계

**날짜**: 2026-05-26  
**범위**: `domain/team` 파일 7개 + `TeamMemberRepository` 메서드 1개 추가  
**방식**: DEV_TASKS.md 기반, `createTeam` 응답을 `TeamDetailResponse`로 통일 (방식 A)

---

## 구현 대상 파일

| 파일 | 작업 |
|------|------|
| `entity/Team.java` | 생성자, createdAt, updateLeader(), updateTeamName() 구현 |
| `entity/TeamMember.java` | 생성자, joinedAt, updateNudgePermission(), updateRole() 구현 |
| `dto/response/TeamMemberResponse.java` | from() 구현 |
| `dto/response/TeamDetailResponse.java` | inviteLink 필드 추가 + from() 구현 |
| `service/TeamService.java` | 6개 메서드 전체 구현 |
| `controller/TeamController.java` | 7개 엔드포인트 전체 구현 |
| `repository/TeamMemberRepository.java` | countByTeamId() 메서드 추가 |

---

## 섹션 1: 엔티티

### Team.java
- `@CreationTimestamp LocalDateTime createdAt` 필드 추가
- `@Builder` 생성자: `this.teamName`, `this.leaderId`, `this.inviteCode` 할당
- `updateLeader(Long newLeaderId)`: `this.leaderId = newLeaderId`
- `updateTeamName(String teamName)`: `this.teamName = teamName`

### TeamMember.java
- `@CreationTimestamp LocalDateTime joinedAt` 필드 추가
- `@Builder` 생성자: `this.teamId`, `this.user`, `this.role`, `this.canNudge` 할당
- `updateNudgePermission(boolean canNudge)`: `this.canNudge = canNudge`
- `updateRole(TeamRole role)`: `this.role = role`

---

## 섹션 2: Response DTO

### TeamMemberResponse.from(TeamMember)
```
userId   ← member.getUser().getId()
username ← member.getUser().getUsername()
color    ← member.getUser().getColor()
role     ← member.getRole()
canNudge ← member.isCanNudge()
joinedAt ← member.getJoinedAt().toString()
```

### TeamDetailResponse
- `inviteLink` 필드 추가 (String)
- `from(Team, List<TeamMemberResponse>)`:
```
teamId     ← team.getId()
teamName   ← team.getTeamName()
leaderId   ← team.getLeaderId()
inviteCode ← team.getInviteCode()
inviteLink ← "nudgeup://join?code=" + team.getInviteCode()
createdAt  ← team.getCreatedAt().toString()
members    ← members
```

### TeamResponse (서비스에서 빌더 직접 사용)
```
teamId      ← team.getId()
teamName    ← team.getTeamName()
leaderId    ← team.getLeaderId()
memberCount ← teamMemberRepository.countByTeamId(teamId)
myRole      ← membership.getRole()
createdAt   ← team.getCreatedAt().toString()
```

### TeamMemberRepository 추가
```java
long countByTeamId(Long teamId);
```

---

## 섹션 3: TeamService 비즈니스 로직

### createTeam `@Transactional`
1. `userRepository.findById(userId)` → `USER_NOT_FOUND`
2. `inviteCode = UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase()`
3. `Team` 저장 → `TeamMember(LEADER, canNudge=true)` 저장
4. `TeamDetailResponse.from(team, List.of(TeamMemberResponse.from(member)))` 반환

### getMyTeams `readOnly`
1. `findAllByUserId(userId)` → 멤버십 목록
2. 각 멤버십 → `teamRepository.findById()` → `TeamResponse` 빌더 생성
3. `List<TeamResponse>` 반환

### getTeamDetail `readOnly`
1. `findByTeamIdAndUserId()` → `NOT_TEAM_MEMBER`
2. `findByTeamId()` → 멤버 목록 → `TeamMemberResponse.from()` 리스트
3. `TeamDetailResponse.from(team, members)` 반환

### getInviteCode `readOnly`
1. `findByTeamIdAndUserId()` → `NOT_TEAM_MEMBER`
2. `InviteResponse` 빌더로 반환

### joinTeam `@Transactional`
1. `findByInviteCode()` → `INVALID_INVITE_CODE`
2. `existsByTeamIdAndUserId()` → `ALREADY_TEAM_MEMBER`
3. `userRepository.findById(userId)` → `TeamMember(MEMBER, canNudge=false)` 저장
4. `TeamResponse` 빌더로 반환
> 참고: API_SPEC 3-5는 `joinedAt` 필드를 포함하지만 기존 컨트롤러 시그니처가 `TeamResponse`로 고정되어 있어 해당 필드는 포함하지 않음 (허용된 사소한 차이)

### updateTeamSettings `@Transactional`
1. `findByTeamIdAndUserId()` → `NOT_TEAM_MEMBER`, role != LEADER → `NOT_TEAM_LEADER`
2. `teamName != null` → `team.updateTeamName()`
3. `newLeaderId != null` → 기존 팀장 `updateRole(MEMBER)`, 새 팀장 `updateRole(LEADER)` + `team.updateLeader()`
4. `memberPermissions != null` → 각 항목 멤버 조회 후 `updateNudgePermission()`

### leaveTeam `@Transactional`
1. `findByTeamIdAndUserId()` → `NOT_TEAM_MEMBER`
2. role == LEADER → `LEADER_CANNOT_LEAVE`
3. `teamMemberRepository.delete(member)`

---

## 섹션 4: TeamController

모든 메서드: `userDetails.getUser().getId()`로 userId 추출 → service 호출.

| 메서드 | HTTP 상태 | ApiResponse 메시지 |
|--------|-----------|-------------------|
| createTeam | 201 Created | "팀이 생성되었습니다." |
| getMyTeams | 200 OK | "팀 목록 조회 성공" |
| getTeamDetail | 200 OK | "팀 조회 성공" |
| getInviteCode | 200 OK | "초대 코드 조회 성공" |
| joinTeam | 200 OK | "팀에 참가했습니다." |
| updateTeamSettings | 200 OK | "팀 설정이 저장되었습니다." |
| leaveTeam | 200 OK | "팀에서 탈퇴했습니다." |

`createTeam`만 `ResponseEntity.status(HttpStatus.CREATED)`, 나머지 `ResponseEntity.ok()`.

---

## 에러 처리 요약

| 상황 | ErrorCode |
|------|-----------|
| 팀 미소속 접근 | `NOT_TEAM_MEMBER` |
| 팀장 아닌 설정 변경 | `NOT_TEAM_LEADER` |
| 잘못된 초대 코드 | `INVALID_INVITE_CODE` |
| 중복 팀 참가 | `ALREADY_TEAM_MEMBER` |
| 팀장 탈퇴 시도 | `LEADER_CANNOT_LEAVE` |
| 유저 미존재 | `USER_NOT_FOUND` |
