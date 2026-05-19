package com.nudgeup.backend.domain.nudge.dto.response;

import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

// TODO: POST /teams/{teamId}/tasks/{taskId}/nudge 응답 DTO
// - 발송 직후 응답: 로그 ID, 작업 정보, 발신자/수신자 정보 포함
@Getter
@Builder
public class NudgeResponse {

    private Long nudgeLogId;
    private Long taskId;
    private String taskTitle;
    private UserResponse sentTo;
    private UserResponse sentBy;
    private String sentAt;
}
