# Team 도메인 API 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Team 도메인 스켈레톤 7개 파일을 완전히 구현하여 팀 생성/조회/참가/설정/탈퇴 API를 동작 가능한 상태로 만든다.

**Architecture:** DEV_TASKS.md 방식 A — `createTeam` 응답을 `TeamDetailResponse`로 통일, `inviteLink` 필드를 `TeamDetailResponse`에 추가. 비즈니스 로직은 `TeamService`에 집중, Controller는 userId 추출 후 Service 위임만 담당.

**Tech Stack:** Java 17, Spring Boot 3, Spring Data JPA, Lombok, jakarta.validation

---

## 파일 목록

| 작업 | 파일 |
|------|------|
| Modify | `backend/src/main/java/com/nudgeup/backend/domain/team/entity/Team.java` |
| Modify | `backend/src/main/java/com/nudgeup/backend/domain/team/entity/TeamMember.java` |
| Modify | `backend/src/main/java/com/nudgeup/backend/domain/team/repository/TeamMemberRepository.java` |
| Modify | `backend/src/main/java/com/nudgeup/backend/domain/team/dto/response/TeamMemberResponse.java` |
| Modify | `backend/src/main/java/com/nudgeup/backend/domain/team/dto/response/TeamDetailResponse.java` |
| Modify | `backend/src/main/java/com/nudgeup/backend/domain/team/service/TeamService.java` |
| Modify | `backend/src/main/java/com/nudgeup/backend/domain/team/controller/TeamController.java` |

---

## Task 1: 엔티티 구현 (Team, TeamMember)

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/entity/Team.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/entity/TeamMember.java`

- [ ] **Step 1: Team.java 전체 교체**

```java
package com.nudgeup.backend.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(name = "invite_code", nullable = false, unique = true, length = 20)
    private String inviteCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Team(String teamName, Long leaderId, String inviteCode) {
        this.teamName = teamName;
        this.leaderId = leaderId;
        this.inviteCode = inviteCode;
    }

    public void updateLeader(Long newLeaderId) {
        this.leaderId = newLeaderId;
    }

    public void updateTeamName(String teamName) {
        this.teamName = teamName;
    }
}
```

- [ ] **Step 2: TeamMember.java 전체 교체**

```java
package com.nudgeup.backend.domain.team.entity;

import com.nudgeup.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TeamRole role;

    @Column(name = "can_nudge", nullable = false)
    private boolean canNudge;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    public TeamMember(Long teamId, User user, TeamRole role, boolean canNudge) {
        this.teamId = teamId;
        this.user = user;
        this.role = role;
        this.canNudge = canNudge;
    }

    public void updateNudgePermission(boolean canNudge) {
        this.canNudge = canNudge;
    }

    public void updateRole(TeamRole role) {
        this.role = role;
    }
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/team/entity/
git commit -m "feat: Team/TeamMember 엔티티 구현 (createdAt, joinedAt, 상태변경 메서드)"
```

---

## Task 2: Repository 메서드 추가

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/repository/TeamMemberRepository.java`

- [ ] **Step 1: countByTeamId 메서드 추가**

기존 파일에 아래 메서드 추가:
```java
long countByTeamId(Long teamId);
```

최종 파일:
```java
package com.nudgeup.backend.domain.team.repository;

import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamId(Long teamId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    List<TeamMember> findAllByUserId(Long userId);

    long countByTeamId(Long teamId);
}
```

- [ ] **Step 2: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/team/repository/TeamMemberRepository.java
git commit -m "feat: TeamMemberRepository에 countByTeamId 추가"
```

---

## Task 3: Response DTO 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/dto/response/TeamMemberResponse.java`
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/dto/response/TeamDetailResponse.java`

- [ ] **Step 1: TeamMemberResponse.java — from() 구현**

```java
package com.nudgeup.backend.domain.team.dto.response;

import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamMemberResponse {

    private Long userId;
    private String username;
    private String color;
    private TeamRole role;
    private boolean canNudge;
    private String joinedAt;

    public static TeamMemberResponse from(TeamMember member) {
        return TeamMemberResponse.builder()
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .color(member.getUser().getColor())
                .role(member.getRole())
                .canNudge(member.isCanNudge())
                .joinedAt(member.getJoinedAt().toString())
                .build();
    }
}
```

- [ ] **Step 2: TeamDetailResponse.java — inviteLink 필드 추가 + from() 구현**

```java
package com.nudgeup.backend.domain.team.dto.response;

import com.nudgeup.backend.domain.team.entity.Team;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamDetailResponse {

    private Long teamId;
    private String teamName;
    private Long leaderId;
    private String inviteCode;
    private String inviteLink;
    private String createdAt;
    private List<TeamMemberResponse> members;

    public static TeamDetailResponse from(Team team, List<TeamMemberResponse> members) {
        return TeamDetailResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .leaderId(team.getLeaderId())
                .inviteCode(team.getInviteCode())
                .inviteLink("nudgeup://join?code=" + team.getInviteCode())
                .createdAt(team.getCreatedAt().toString())
                .members(members)
                .build();
    }
}
```

- [ ] **Step 3: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/team/dto/response/
git commit -m "feat: TeamMemberResponse/TeamDetailResponse DTO from() 구현"
```

---

## Task 4: TeamService 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/service/TeamService.java`

- [ ] **Step 1: TeamService.java 전체 교체**

```java
package com.nudgeup.backend.domain.team.service;

import com.nudgeup.backend.domain.team.dto.request.CreateTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.JoinTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.UpdateTeamSettingsRequest;
import com.nudgeup.backend.domain.team.dto.response.*;
import com.nudgeup.backend.domain.team.entity.Team;
import com.nudgeup.backend.domain.team.entity.TeamMember;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
import com.nudgeup.backend.domain.team.repository.TeamRepository;
import com.nudgeup.backend.domain.user.entity.User;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamDetailResponse createTeam(Long userId, CreateTeamRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Team team = teamRepository.save(Team.builder()
                .teamName(request.getTeamName())
                .leaderId(userId)
                .inviteCode(inviteCode)
                .build());
        TeamMember member = teamMemberRepository.save(TeamMember.builder()
                .teamId(team.getId())
                .user(user)
                .role(TeamRole.LEADER)
                .canNudge(true)
                .build());
        return TeamDetailResponse.from(team, List.of(TeamMemberResponse.from(member)));
    }

    public List<TeamResponse> getMyTeams(Long userId) {
        return teamMemberRepository.findAllByUserId(userId).stream()
                .map(membership -> {
                    Team team = teamRepository.findById(membership.getTeamId())
                            .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));
                    int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());
                    return TeamResponse.builder()
                            .teamId(team.getId())
                            .teamName(team.getTeamName())
                            .leaderId(team.getLeaderId())
                            .memberCount(memberCount)
                            .myRole(membership.getRole())
                            .createdAt(team.getCreatedAt().toString())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public TeamDetailResponse getTeamDetail(Long userId, Long teamId) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));
        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(teamId).stream()
                .map(TeamMemberResponse::from)
                .collect(Collectors.toList());
        return TeamDetailResponse.from(team, members);
    }

    public InviteResponse getInviteCode(Long userId, Long teamId) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));
        return InviteResponse.builder()
                .inviteCode(team.getInviteCode())
                .inviteLink("nudgeup://join?code=" + team.getInviteCode())
                .build();
    }

    @Transactional
    public TeamResponse joinTeam(Long userId, JoinTeamRequest request) {
        Team team = teamRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new CustomException(ErrorCode.ALREADY_TEAM_MEMBER);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        teamMemberRepository.save(TeamMember.builder()
                .teamId(team.getId())
                .user(user)
                .role(TeamRole.MEMBER)
                .canNudge(false)
                .build());
        int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());
        return TeamResponse.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .leaderId(team.getLeaderId())
                .memberCount(memberCount)
                .myRole(TeamRole.MEMBER)
                .createdAt(team.getCreatedAt().toString())
                .build();
    }

    @Transactional
    public void updateTeamSettings(Long userId, Long teamId, UpdateTeamSettingsRequest request) {
        TeamMember myMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
        if (myMembership.getRole() != TeamRole.LEADER) {
            throw new CustomException(ErrorCode.NOT_TEAM_LEADER);
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));
        if (request.getTeamName() != null) {
            team.updateTeamName(request.getTeamName());
        }
        if (request.getNewLeaderId() != null) {
            myMembership.updateRole(TeamRole.MEMBER);
            TeamMember newLeader = teamMemberRepository.findByTeamIdAndUserId(teamId, request.getNewLeaderId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
            newLeader.updateRole(TeamRole.LEADER);
            team.updateLeader(request.getNewLeaderId());
        }
        if (request.getMemberPermissions() != null) {
            for (UpdateTeamSettingsRequest.MemberPermission permission : request.getMemberPermissions()) {
                teamMemberRepository.findByTeamIdAndUserId(teamId, permission.getUserId())
                        .ifPresent(m -> m.updateNudgePermission(permission.isCanNudge()));
            }
        }
    }

    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_TEAM_MEMBER));
        if (member.getRole() == TeamRole.LEADER) {
            throw new CustomException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        teamMemberRepository.delete(member);
    }
}
```

- [ ] **Step 2: 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/team/service/TeamService.java
git commit -m "feat: TeamService 비즈니스 로직 전체 구현"
```

---

## Task 5: TeamController 구현

**Files:**
- Modify: `backend/src/main/java/com/nudgeup/backend/domain/team/controller/TeamController.java`

- [ ] **Step 1: TeamController.java 전체 교체**

```java
package com.nudgeup.backend.domain.team.controller;

import com.nudgeup.backend.domain.team.dto.request.CreateTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.JoinTeamRequest;
import com.nudgeup.backend.domain.team.dto.request.UpdateTeamSettingsRequest;
import com.nudgeup.backend.domain.team.dto.response.*;
import com.nudgeup.backend.domain.team.service.TeamService;
import com.nudgeup.backend.global.response.ApiResponse;
import com.nudgeup.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeamDetailResponse>> createTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTeamRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("팀이 생성되었습니다.", teamService.createTeam(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getMyTeams(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("팀 목록 조회 성공", teamService.getMyTeams(userId)));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("팀 조회 성공", teamService.getTeamDetail(userId, teamId)));
    }

    @GetMapping("/{teamId}/invite")
    public ResponseEntity<ApiResponse<InviteResponse>> getInviteCode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("초대 코드 조회 성공", teamService.getInviteCode(userId, teamId)));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody JoinTeamRequest request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("팀에 참가했습니다.", teamService.joinTeam(userId, request)));
    }

    @PatchMapping("/{teamId}/settings")
    public ResponseEntity<ApiResponse<Void>> updateTeamSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId,
            @RequestBody UpdateTeamSettingsRequest request) {
        Long userId = userDetails.getUser().getId();
        teamService.updateTeamSettings(userId, teamId, request);
        return ResponseEntity.ok(ApiResponse.success("팀 설정이 저장되었습니다.", null));
    }

    @DeleteMapping("/{teamId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long teamId) {
        Long userId = userDetails.getUser().getId();
        teamService.leaveTeam(userId, teamId);
        return ResponseEntity.ok(ApiResponse.success("팀에서 탈퇴했습니다.", null));
    }
}
```

- [ ] **Step 2: 최종 컴파일 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/nudgeup/backend/domain/team/controller/TeamController.java
git commit -m "feat: TeamController 7개 엔드포인트 구현"
```
