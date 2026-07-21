package com.brightfutureschool.model;

public class AttendanceRecord {
    private long id;
    private long studentId;
    private long classId;
    private String attendanceDate; // ISO format yyyy-MM-dd
    private String status;         // "PRESENT" or "ABSENT"

    public AttendanceRecord() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }

    public String getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(String attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}