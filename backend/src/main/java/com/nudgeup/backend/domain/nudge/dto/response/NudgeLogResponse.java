package com.nudgeup.backend.domain.nudge.dto.response;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

// TODO: GET /teams/{teamId}/nudge-logs 응답의 개별 로그 항목 DTO
// - from(NudgeLog, taskTitle, sentTo, sentBy): 변환 메서드
@Getter
@Builder
public class NudgeLogResponse {

    private Long nudgeLogId;
    private Long taskId;
    private String taskTitle;
    private UserResponse sentTo;
    private UserResponse sentBy;
    private String sentAt;

    public static NudgeLogResponse from(NudgeLog log, String taskTitle,
                                        UserResponse sentTo, UserResponse sentBy) {
        // TODO: 구현
        return null;
    }
}
