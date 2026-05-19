package com.nudgeup.backend.global.fcm;

import lombok.Builder;
import lombok.Getter;

// TODO: FCM 전송 시 사용하는 내부 DTO
// - token: 수신자의 FCM 디바이스 토큰
// - title: 알림 제목
// - body: 알림 본문
// - data: 앱에서 딥링크 처리에 사용할 추가 데이터 (key-value)
@Getter
@Builder
public class FcmMessageDto {

    private String token;
    private String title;
    private String body;

    // TODO: Map<String, String> data 필드 추가
    // - type: "NUDGE"
    // - teamId, taskId, taskTitle, sentBy
}
