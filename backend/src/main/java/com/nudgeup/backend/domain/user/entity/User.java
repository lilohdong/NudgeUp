package com.nudgeup.backend.domain.user.entity;

import com.nudgeup.backend.global.exception.CustomException;
import com.nudgeup.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

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

    @Column(name = "username_changed_at")
    private LocalDateTime usernameChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String userId, String username, String email, String password, String color) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.color = color != null ? color : "#A78BFA";
    }

    public void updateUsername(String username) {
        if (this.username.equals(username)) return;
        if (usernameChangedAt != null &&
                usernameChangedAt.plusDays(7).isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.USERNAME_CHANGE_TOO_SOON);
        }
        this.username = username;
        this.usernameChangedAt = LocalDateTime.now();
    }

    public void updateColor(String color) {
        if (color != null) this.color = color;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
