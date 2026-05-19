package com.nudgeup.backend.global.fcm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// TODO: FCM 푸시 알림 실제 발송 서비스
// - sendNudgeMessage(FcmMessageDto): FirebaseMessaging.getInstance().send()로 발송
// - FCM 토큰이 null이거나 빈 문자열이면 발송 스킵 (기기 미등록 케이스)
// - 발송 실패 시 FirebaseMessagingException catch → 로그 출력 후 무시 (알림 실패가 작업 API를 막으면 안 됨)
@Slf4j
@Service
public class FcmService {

    public void sendNudgeMessage(FcmMessageDto dto) {
        // TODO: 구현
        // if (dto.getToken() == null || dto.getToken().isBlank()) return;
        // Message message = Message.builder()
        //     .setToken(dto.getToken())
        //     .setNotification(Notification.builder().setTitle(dto.getTitle()).setBody(dto.getBody()).build())
        //     .putAllData(dto.getData())
        //     .build();
        // FirebaseMessaging.getInstance().send(message);
    }
}
