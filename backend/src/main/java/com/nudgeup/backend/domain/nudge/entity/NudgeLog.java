package com.nudgeup.backend.domain.nudge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "nudge_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NudgeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "sent_by_id", nullable = false)
    private Long sentById;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @Builder
    public NudgeLog(Long taskId, Long sentById) {
        this.taskId = taskId;
        this.sentById = sentById;
    }
}
