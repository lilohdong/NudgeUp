package com.nudgeup.app.data.dto.request;

import java.util.Map;

public class TeamSettingsRequest {
    private Long newLeaderId;
    private Map<Long, Boolean> nudgePermissions; // userId -> canNudge

    public TeamSettingsRequest(Long newLeaderId, Map<Long, Boolean> nudgePermissions) {
        this.newLeaderId = newLeaderId;
        this.nudgePermissions = nudgePermissions;
    }
}
