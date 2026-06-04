package com.nudgeup.app.data.dto.response;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserResponse getUser() { return user; }
}
