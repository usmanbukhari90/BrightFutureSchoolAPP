package com.brightfutureschool.model;

public class Exam {
    private long id;
    private long classId;
    private String examName;
    private String examYear;

    public Exam() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public String getExamYear() { return examYear; }
    public void setExamYear(String examYear) { this.examYear = examYear; }

    @Override
    public String toString() { return examName + (examYear != null && !examYear.isEmpty() ? " (" + examYear + ")" : ""); }
}