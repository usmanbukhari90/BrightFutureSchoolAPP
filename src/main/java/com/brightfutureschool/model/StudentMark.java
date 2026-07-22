package com.brightfutureschool.model;

public class StudentMark {
    private long id;
    private long examSubjectId;
    private long studentId;
    private Double marksObtained; // null = not yet entered

    public StudentMark() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getExamSubjectId() { return examSubjectId; }
    public void setExamSubjectId(long examSubjectId) { this.examSubjectId = examSubjectId; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public Double getMarksObtained() { return marksObtained; }
    public void setMarksObtained(Double marksObtained) { this.marksObtained = marksObtained; }
}