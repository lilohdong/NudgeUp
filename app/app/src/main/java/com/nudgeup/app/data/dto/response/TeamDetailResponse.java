package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TeamDetailResponse {
    public TeamDetailResponse() {}

    @SerializedName("teamId")
    private long id;
    @SerializedName("teamName")
    private String name;
    private long leaderId;
    private String inviteCode;
    private List<MemberResponse> members;

    public long getId() { return id; }
    public String getName() { return name; }
    public long getLeaderId() { return leaderId; }
    public String getInviteCode() { return inviteCode; }
    public List<MemberResponse> getMembers() { return members; }
}
