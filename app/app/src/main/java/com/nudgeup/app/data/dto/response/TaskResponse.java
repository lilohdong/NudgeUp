package com.nudgeup.app.data.dto.response;

import com.google.gson.annotations.SerializedName;

public class TaskResponse {
    @SerializedName("taskId")
    private long id;
    private String title;
    private String description;
    private String status;
    private String dueDate;
    private UserResponse assignee;
    private UserResponse createdBy;

    public TaskResponse() {}

    public TaskResponse(long id, String title, String status, String dueDate, UserResponse assignee) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.dueDate = dueDate;
        this.assignee = assignee;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getDueDate() { return dueDate; }
    public UserResponse getAssignee() { return assignee; }
    public UserResponse getCreatedBy() { return createdBy; }
}
