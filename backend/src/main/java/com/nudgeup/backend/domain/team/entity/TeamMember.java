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
