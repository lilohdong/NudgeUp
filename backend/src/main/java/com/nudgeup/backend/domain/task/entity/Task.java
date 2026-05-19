package com.nudgeup.backend.domain.task.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// TODO: tasks 테이블과 매핑되는 JPA 엔티티
// DB 컬럼: id, team_id, title, description, assignee_id, created_by_id, status, due_date, started_at, completed_at, created_at
// - assigneeId: 작업 담당자 (nullable - 미지정 시 생성자 본인)
// - createdById: 작업 생성자
// - changeStatus(): 상태 변경 메서드 (PENDING→IN_PROGRESS 시 startedAt, IN_PROGRESS→COMPLETED 시 completedAt 자동 기록)
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

    // TODO: @CreationTimestamp LocalDateTime createdAt 추가

    @Builder
    public Task(Long teamId, String title, String description,
                Long assigneeId, Long createdById, LocalDate dueDate) {
        // TODO: 구현, status 기본값 = PENDING
    }

    public void changeStatus(TaskStatus newStatus) {
        // TODO: 구현
        // PENDING → IN_PROGRESS: this.startedAt = LocalDateTime.now()
        // IN_PROGRESS → COMPLETED: this.completedAt = LocalDateTime.now()
        this.status = newStatus;
    }
}
