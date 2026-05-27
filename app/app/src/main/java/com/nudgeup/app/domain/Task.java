package com.nudgeup.app.domain;

public class Task {
    private long id;
    private String title;
    private String status; // PENDING, IN_PROGRESS, COMPLETED
    private String dueDate;
    private User assignee;
    private User createdBy;

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getDueDate() { return dueDate; }
    public User getAssignee() { return assignee; }
    public User getCreatedBy() { return createdBy; }
}
