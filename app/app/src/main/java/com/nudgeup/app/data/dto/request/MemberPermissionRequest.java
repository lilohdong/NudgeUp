package com.nudgeup.app.data.dto.request;

public class MemberPermissionRequest {
    private long userId;
    private boolean canNudge;

    public MemberPermissionRequest(long userId, boolean canNudge) {
        this.userId = userId;
        this.canNudge = canNudge;
    }
}
