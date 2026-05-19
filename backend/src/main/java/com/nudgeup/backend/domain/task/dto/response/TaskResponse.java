package com.nudgeup.backend.domain.task.dto.response;

import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// TODO: 작업 응답 DTO
// - dDay: 마감일 기준 남은 일수 (양수=여유, 음수=초과, 0=당일, null=마감일 없음)
// - assignee, createdBy: UserResponse로 유저 정보 포함
// - from(Task, UserResponse assignee, UserResponse createdBy): 엔티티 → DTO 변환
@Getter
@Builder
public class TaskResponse {

    private Long taskId;
    private String title;
    private String description;
    private TaskStatus status;
    private UserResponse assignee;
    private UserResponse createdBy;
    private LocalDate dueDate;
    private Long dDay;
    private Long nudgeCount;
    private String startedAt;
    private String completedAt;
    private String createdAt;

    public static TaskResponse from(Task task, UserResponse assignee, UserResponse createdBy, long nudgeCount) {
        // TODO: 구현
        // dDay 계산: dueDate != null ? ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate()) : null
        return null;
    }
}
