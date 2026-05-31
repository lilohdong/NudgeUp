package com.nudgeup.backend.domain.task.entity;

import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Task(Long teamId, String title, String description,
                Long assigneeId, Long createdById, LocalDate dueDate) {
        this.teamId = teamId;
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
        this.createdById = createdById;
        this.dueDate = dueDate;
        this.status = TaskStatus.PENDING;
    }

    public void changeStatus(TaskStatus newStatus) {
        if (this.status == TaskStatus.CANCELLED) {
            throw new CustomException(ErrorCode.TASK_ALREADY_CANCELLED);
        }
        if (this.status == TaskStatus.COMPLETED
                && newStatus != TaskStatus.IN_PROGRESS
                && newStatus != TaskStatus.CANCELLED) {
            throw new CustomException(ErrorCode.TASK_ALREADY_COMPLETED);
        }
        if (newStatus == TaskStatus.IN_PROGRESS) {
            if (this.startedAt == null) {
                this.startedAt = LocalDateTime.now();
            }
            this.completedAt = null;
        } else if (newStatus == TaskStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        } else if (newStatus == TaskStatus.CANCELLED) {
            this.cancelledAt = LocalDateTime.now();
        }
        this.status = newStatus;
    }
}
