package com.brightfutureschool.model;

public class ExamSubject {
    private long id;
    private long examId;
    private String subjectName;
    private int totalMarks;

    public ExamSubject() {}

    public ExamSubject(String subjectName, int totalMarks) {
        this.subjectName = subjectName;
        this.totalMarks = totalMarks;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }
}