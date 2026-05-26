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
