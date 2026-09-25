package com.hallsync.model;

public record Complaint(int id, String user, String subject, String message, String status, String date) {}
