package com.nudgeup.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO: 서비스 전체 에러 코드를 Enum으로 중앙 관리
// - HTTP 상태코드 + 클라이언트 메시지를 함께 보유
// - API 명세서의 Error Cases 항목을 모두 등록
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_USER_ID(409, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(401, "유효하지 않거나 만료된 토큰입니다."),

    // User
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),

    // Team
    TEAM_NOT_FOUND(404, "팀을 찾을 수 없습니다."),
    INVALID_INVITE_CODE(400, "유효하지 않은 초대 코드입니다."),
    ALREADY_TEAM_MEMBER(409, "이미 소속된 팀입니다."),
    NOT_TEAM_MEMBER(403, "해당 팀의 멤버가 아닙니다."),
    NOT_TEAM_LEADER(403, "팀장만 수행할 수 있습니다."),
    LEADER_CANNOT_LEAVE(400, "팀장은 팀장을 위임한 후 탈퇴할 수 있습니다."),

    // Task
    TASK_NOT_FOUND(404, "작업을 찾을 수 없습니다."),
    TASK_ALREADY_COMPLETED(400, "이미 완료된 작업입니다."),
    NO_TASK_PERMISSION(403, "담당자 또는 팀장만 상태를 변경할 수 있습니다."),

    // Nudge
    NO_NUDGE_PERMISSION(403, "독촉 권한이 없습니다."),

    // Server
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
