package com.nudgeup.app.data.dto.response;

import com.nudgeup.app.domain.User;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private User user;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public User getUser() { return user; }
}
