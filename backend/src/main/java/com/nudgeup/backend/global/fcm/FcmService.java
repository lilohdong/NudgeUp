package com.nudgeup.backend.global.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class FcmService {

    public void sendNudgeMessage(FcmMessageDto dto) {
        if (!StringUtils.hasText(dto.getToken())) {
            log.warn("FCM 토큰이 없어 알림을 보내지 않습니다.");
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(dto.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(dto.getTitle())
                            .setBody(dto.getBody())
                            .build())
                    .putAllData(dto.getData())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 발송 실패: {}", e.getMessage());
        }
    }
}
