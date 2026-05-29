package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    public UserResponse() {}

    @SerializedName("userId")
    private long id;
    @SerializedName("username")
    private String username;
    private String email;
    private String color;

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getColor() { return color; }
}
