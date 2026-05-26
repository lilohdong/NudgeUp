package com.nudgeup.backend.domain.nudge.dto.response;

import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import com.nudgeup.backend.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

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
        return NudgeLogResponse.builder()
                .nudgeLogId(log.getId())
                .taskId(log.getTaskId())
                .taskTitle(taskTitle)
                .sentTo(sentTo)
                .sentBy(sentBy)
                .sentAt(log.getSentAt() != null ? log.getSentAt().toString() : null)
                .build();
    }
}
