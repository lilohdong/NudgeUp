package com.nudgeup.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

// TODO: users 테이블과 매핑되는 JPA 엔티티
// DB 컬럼: id, username, email, password, color, fcm_token, created_at
// - fcmToken: FCM 푸시 알림 수신용 디바이스 토큰 (앱 실행 시 갱신)
// - color: 프로필 아이콘 색상 (HEX, 기본값 "#A78BFA")
// - updateProfile(): username, color 변경 메서드 (엔티티 내부에서 상태 변경 캡슐화)
// - updateFcmToken(): fcmToken 갱신 메서드
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 7)
    private String color;

    @Column(name = "fcm_token")
    private String fcmToken;

    // TODO: @CreationTimestamp LocalDateTime createdAt 추가

    @Builder
    public User(String username, String email, String password, String color) {
        // TODO: 구현
    }

    public void updateProfile(String username, String color) {
        // TODO: 구현
    }

    public void updateFcmToken(String fcmToken) {
        // TODO: 구현
    }
}
