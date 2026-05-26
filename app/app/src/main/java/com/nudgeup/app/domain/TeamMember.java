package com.nudgeup.app.domain;

public class TeamMember {
    private long userId;
    private String name;
    private String role; // LEADER or MEMBER
    private boolean canNudge;

    public long getUserId() { return userId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean isCanNudge() { return canNudge; }
    public boolean isLeader() { return "LEADER".equals(role); }
}
