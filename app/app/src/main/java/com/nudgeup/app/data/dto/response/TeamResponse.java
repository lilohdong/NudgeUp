package com.nudgeup.app.data.dto.response;

public class TeamResponse {
    private long id;
    private String name;
    private String inviteCode;

    public TeamResponse() {}

    public TeamResponse(long id, String name, String inviteCode) {
        this.id = id;
        this.name = name;
        this.inviteCode = inviteCode;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getInviteCode() { return inviteCode; }
}
