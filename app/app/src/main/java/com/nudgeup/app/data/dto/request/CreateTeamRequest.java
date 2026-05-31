package com.nudgeup.app.data.dto.request;

public class CreateTeamRequest {
    private String teamName;
    private String color;

    public CreateTeamRequest(String teamName, String color) {
        this.teamName = teamName;
        this.color = color;
    }
}
