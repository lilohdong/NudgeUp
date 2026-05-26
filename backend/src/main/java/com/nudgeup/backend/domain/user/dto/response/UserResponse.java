package com.nudgeup.backend.domain.user.dto.response;

import com.nudgeup.backend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private String color;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .color(user.getColor())
                .build();
    }
}
