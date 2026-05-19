package com.nudgeup.backend.domain.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

// TODO: POST /teams/{teamId}/tasks 요청 바디
// - assigneeId: Optional (null이면 요청자 본인으로 지정)
// - dueDate: Optional
// - description: 최대 1000자
@Getter
public class CreateTaskRequest {

    @NotBlank(message = "작업 제목은 필수입니다.")
    @Size(max = 200, message = "작업 제목은 200자 이하여야 합니다.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    private String description;

    private Long assigneeId;

    private LocalDate dueDate;
}
