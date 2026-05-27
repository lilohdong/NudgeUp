package com.nudgeup.app.data.dto.response;

import com.nudgeup.app.domain.User;

public class TaskResponse {
    private long id;
    private String title;
    private String status;
    private String dueDate;
    private User assignee;
    private User createdBy;

    public TaskResponse() {}

    public TaskResponse(long id, String title, String status, String dueDate, User assignee) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.dueDate = dueDate;
        this.assignee = assignee;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getDueDate() { return dueDate; }
    public User getAssignee() { return assignee; }
    public User getCreatedBy() { return createdBy; }
}
