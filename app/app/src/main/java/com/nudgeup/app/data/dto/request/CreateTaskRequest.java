package com.nudgeup.app.data.dto.request;

public class CreateTaskRequest {
    private String title;
    private long assigneeId;
    private long teamId;
    private String dueDate;

    public CreateTaskRequest(String title, long assigneeId, long teamId, String dueDate) {
        this.title = title;
        this.assigneeId = assigneeId;
        this.teamId = teamId;
        this.dueDate = dueDate;
    }
}
