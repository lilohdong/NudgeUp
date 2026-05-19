package com.nudgeup.backend.domain.team.entity;

import jakarta.persistence.*;
import lombok.*;

// TODO: teams 테이블과 매핑되는 JPA 엔티티
// DB 컬럼: id, team_name, leader_id, invite_code, created_at
// - inviteCode: 팀 생성 시 UUID 앞 8자리로 자동 생성 (unique)
// - updateLeader(): 팀장 변경 메서드
// - updateTeamName(): 팀 이름 변경 메서드
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

    // TODO: @CreationTimestamp LocalDateTime createdAt 추가

    @Builder
    public Team(String teamName, Long leaderId, String inviteCode) {
        // TODO: 구현
    }

    public void updateLeader(Long newLeaderId) {
        // TODO: 구현
    }

    public void updateTeamName(String teamName) {
        // TODO: 구현
    }
}
