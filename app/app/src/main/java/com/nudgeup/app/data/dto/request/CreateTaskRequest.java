package com.nudgeup.app.data.dto.request;

public class CreateTaskRequest {
    private String title;
    private String description;
    private Long assigneeId;
    private String dueDate;

    public CreateTaskRequest(String title, String description, Long assigneeId, String dueDate) {
        this.title = title;
        this.description = description;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }
}
