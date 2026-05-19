package com.nudgeup.backend.domain.team.entity;

import com.nudgeup.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

// TODO: team_members 테이블과 매핑되는 JPA 엔티티 (Team ↔ User 연관 관계 테이블)
// DB 컬럼: id, team_id, user_id, role, can_nudge, joined_at
// - role: TeamRole Enum (@Enumerated(EnumType.STRING))
// - canNudge: 독촉 알림 발송 권한 (기본값 false, 팀장은 항상 true)
// - updateNudgePermission(): 팀장이 독촉 권한 on/off 시 호출
// - updateRole(): 팀장 위임 시 호출
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

    // TODO: @CreationTimestamp LocalDateTime joinedAt 추가

    @Builder
    public TeamMember(Long teamId, User user, TeamRole role, boolean canNudge) {
        // TODO: 구현
    }

    public void updateNudgePermission(boolean canNudge) {
        // TODO: 구현
    }

    public void updateRole(TeamRole role) {
        // TODO: 구현
    }
}
