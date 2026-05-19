package com.nudgeup.backend.domain.task.dto.request;

import com.nudgeup.backend.domain.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// TODO: PATCH /teams/{teamId}/tasks/{taskId}/status 요청 바디
@Getter
public class UpdateTaskStatusRequest {

    @NotNull(message = "상태는 필수입니다.")
    private TaskStatus status;
}
