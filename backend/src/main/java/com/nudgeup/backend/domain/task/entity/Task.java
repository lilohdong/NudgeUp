package com.nudgeup.backend.domain.task.entity;

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
        if (newStatus == TaskStatus.IN_PROGRESS) {
            this.startedAt = LocalDateTime.now();
        } else if (newStatus == TaskStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
        this.status = newStatus;
    }
}
