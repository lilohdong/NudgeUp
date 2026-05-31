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
    private Long teamId;
    private String teamName;
    private UserResponse sentTo;
    private UserResponse sentBy;
    private String sentAt;

    public static NudgeLogResponse from(NudgeLog log, String taskTitle,
                                        Long teamId, String teamName,
                                        UserResponse sentTo, UserResponse sentBy) {
        return NudgeLogResponse.builder()
                .nudgeLogId(log.getId())
                .taskId(log.getTaskId())
                .taskTitle(taskTitle)
                .teamId(teamId)
                .teamName(teamName)
                .sentTo(sentTo)
                .sentBy(sentBy)
                .sentAt(log.getSentAt() != null ? log.getSentAt().toString() : null)
                .build();
    }

    // 하위 호환: 기존 getNudgeLogs()에서 사용
    public static NudgeLogResponse from(NudgeLog log, String taskTitle,
                                        UserResponse sentTo, UserResponse sentBy) {
        return from(log, taskTitle, null, null, sentTo, sentBy);
    }
}
