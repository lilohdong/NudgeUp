package com.nudgeup.backend.domain.task.dto.response;

import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
        Long dDay = task.getDueDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate())
                : null;
        return TaskResponse.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignee(assignee)
                .createdBy(createdBy)
                .dueDate(task.getDueDate())
                .dDay(dDay)
                .nudgeCount(nudgeCount)
                .startedAt(task.getStartedAt() != null ? task.getStartedAt().toString() : null)
                .completedAt(task.getCompletedAt() != null ? task.getCompletedAt().toString() : null)
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null)
                .build();
    }
}
