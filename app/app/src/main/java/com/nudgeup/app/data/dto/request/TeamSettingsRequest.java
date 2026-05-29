package com.nudgeup.app.data.dto.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamSettingsRequest {
    private Long newLeaderId;
    private List<MemberPermissionRequest> memberPermissions;

    public TeamSettingsRequest(Long newLeaderId, Map<Long, Boolean> nudgePermissionsMap) {
        this.newLeaderId = newLeaderId;
        this.memberPermissions = new ArrayList<>();
        if (nudgePermissionsMap != null) {
            for (Map.Entry<Long, Boolean> entry : nudgePermissionsMap.entrySet()) {
                this.memberPermissions.add(new MemberPermissionRequest(entry.getKey(), entry.getValue()));
            }
        }
    }
}
