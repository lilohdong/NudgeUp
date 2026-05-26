package com.nudgeup.app.domain;

public class User {
    private long id;
    private String email;
    private String name;
    private String fcmToken;

    public User() {}

    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getFcmToken() { return fcmToken; }
}
