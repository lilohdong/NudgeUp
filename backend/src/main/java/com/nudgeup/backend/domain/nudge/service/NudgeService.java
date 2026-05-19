package com.nudgeup.backend.domain.nudge.service;

import com.nudgeup.backend.domain.nudge.dto.response.NudgeLogResponse;
import com.nudgeup.backend.domain.nudge.dto.response.NudgeResponse;
import com.nudgeup.backend.domain.nudge.entity.NudgeLog;
import com.nudgeup.backend.domain.nudge.repository.NudgeLogRepository;
import com.nudgeup.backend.domain.task.entity.Task;
import com.nudgeup.backend.domain.task.entity.TaskStatus;
import com.nudgeup.backend.domain.task.repository.TaskRepository;
import com.nudgeup.backend.domain.team.entity.TeamRole;
import com.nudgeup.backend.domain.team.repository.TeamMemberRepository;
import com.nudgeup.backend.domain.user.repository.UserRepository;
import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import com.nudgeup.backend.global.fcm.FcmMessageDto;
import com.nudgeup.backend.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO: 독촉 알림 발송 및 로그 조회 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NudgeService {

    private final NudgeLogRepository nudgeLogRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public NudgeResponse sendNudge(Long userId, Long teamId, Long taskId) {
        // TODO: 구현
        // 1. Task 조회 → TASK_NOT_FOUND
        // 2. 완료된 작업이면 TASK_ALREADY_COMPLETED throw
        // 3. 발신자 TeamMember 조회 → NOT_TEAM_MEMBER
        // 4. 권한 검증: LEADER 또는 canNudge=true → 아니면 NO_NUDGE_PERMISSION
        // 5. 수신자(assignee) User 조회 → fcmToken 확인
        // 6. FcmMessageDto 구성 후 fcmService.sendNudgeMessage() 호출
        // 7. NudgeLog 저장
        // 8. NudgeResponse 반환
        return null;
    }

    public List<NudgeLogResponse> getNudgeLogs(Long userId, Long teamId, Long taskId) {
        // TODO: 구현
        // 1. 팀 소속 검증
        // 2. taskId != null ? findByTaskId() : 팀 전체 작업 ID 조회 후 findByTaskIdIn()
        // 3. 각 로그에 대해 task, sentBy User 조회 후 NudgeLogResponse.from() 매핑
        return null;
    }
}
