package com.nudgeup.app.data.dto.request;

import java.util.List;

public class TeamSettingsRequest {
    private Long newLeaderId;
    private List<MemberPermissionRequest> memberPermissions;

    public TeamSettingsRequest(Long newLeaderId, List<MemberPermissionRequest> memberPermissions) {
        this.newLeaderId = newLeaderId;
        this.memberPermissions = memberPermissions;
    }
}
