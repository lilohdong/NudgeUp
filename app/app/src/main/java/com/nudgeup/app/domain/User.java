package com.nudgeup.app.domain;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("userId")
    private long id;
    private String email;
    @SerializedName("username")
    private String name;
    private String color;

    public User() {}

    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getColor() { return color; }
}
