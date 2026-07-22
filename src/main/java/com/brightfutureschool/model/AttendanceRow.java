package com.brightfutureschool.model;

public class AttendanceRow {
    private final Student student;
    private String status; // "PRESENT", "ABSENT", or null (not marked yet)

    public AttendanceRow(Student student, String status) {
        this.student = student;
        this.status = status;
    }

    public Student getStudent() { return student; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}