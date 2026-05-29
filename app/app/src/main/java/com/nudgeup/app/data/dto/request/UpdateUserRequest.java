package com.nudgeup.app.data.dto.request;

public class UpdateUserRequest {
    private String username;
    private String color;

    public UpdateUserRequest(String username, String color) {
        this.username = username;
        this.color = color;
    }
}
