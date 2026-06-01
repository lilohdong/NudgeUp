package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class NudgeLogItemResponse {

    @SerializedName("nudgeLogId")
    private long id;
    private long taskId;
    private String taskTitle;
    private long teamId;
    private String teamName;
    private UserResponse sentTo;
    private UserResponse sentBy;
    private String sentAt;

    public long getId()              { return id; }
    public long getTaskId()          { return taskId; }
    public String getTaskTitle()     { return taskTitle; }
    public long getTeamId()          { return teamId; }
    public String getTeamName()      { return teamName; }
    public UserResponse getSentTo()  { return sentTo; }
    public UserResponse getSentBy()  { return sentBy; }
    public String getSentAt()        { return sentAt; }
}
