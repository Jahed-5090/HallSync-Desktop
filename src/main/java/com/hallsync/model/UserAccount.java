package com.hallsync.model;

/** Represents a user account row for admin management (includes password for display/edit). */
public record UserAccount(int id, String username, String password, String fullName, String role, String email) {}
