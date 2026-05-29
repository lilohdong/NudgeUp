package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class MemberResponse {
    private long userId;
    @SerializedName("username")
    private String name;
    private String role;
    private boolean canNudge;
    private String color;

    public MemberResponse() {}

    public MemberResponse(long userId, String name, String role, boolean canNudge) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.canNudge = canNudge;
    }

    public long getUserId() { return userId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean isCanNudge() { return canNudge; }
    public String getColor() { return color; }
}
