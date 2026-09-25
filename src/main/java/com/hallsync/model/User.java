package com.hallsync.model;

public class User {
    public final int id;
    public final String username;
    public final String fullName;
    public final String role;

    public User(int id, String username, String fullName, String role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }
}
