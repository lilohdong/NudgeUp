package com.nudgeup.backend.domain.user.dto.response;

import com.nudgeup.backend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

// TODO: 사용자 정보 응답 DTO
// - from(User): 엔티티 → DTO 변환 정적 팩토리 메서드
// - password, fcmToken은 절대 포함하지 않음
@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private String color;

    public static UserResponse from(User user) {
        // TODO: 구현
        return null;
    }
}
