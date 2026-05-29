package com.nudgeup.app.data.dto.request;

public class RegisterRequest {
    private String userId;
    private String username;
    private String email;
    private String password;

    public RegisterRequest(String userId, String username, String email, String password) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
