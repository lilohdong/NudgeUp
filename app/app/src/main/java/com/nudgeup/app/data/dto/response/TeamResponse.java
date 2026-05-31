package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class TeamResponse {
    @SerializedName("teamId")
    private long id;
    @SerializedName("teamName")
    private String name;
    private long leaderId;
    private int memberCount;
    private String myRole;
    private String color;

    public TeamResponse() {}

    public TeamResponse(long id, String name, String inviteCode) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public long getLeaderId() { return leaderId; }
    public int getMemberCount() { return memberCount; }
    public String getMyRole() { return myRole; }
    public String getColor() { return color; }
}
