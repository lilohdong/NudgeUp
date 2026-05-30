package com.nudgeup.app.data.dto.request;

public class UpdateColorRequest {
    private final String color;

    public UpdateColorRequest(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
