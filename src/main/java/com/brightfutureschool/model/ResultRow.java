package com.brightfutureschool.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// One row in the marks table = one student + their marks across all subjects of the current exam.
public class ResultRow {
    private final Student student;
    private final Map<Long, Double> marksBySubjectId = new HashMap<>(); // subjectId -> marks (nullable)

    public ResultRow(Student student) {
        this.student = student;
    }

    public Student getStudent() { return student; }

    public Double getMark(long subjectId) {
        return marksBySubjectId.get(subjectId);
    }

    public void setMark(long subjectId, Double value) {
        marksBySubjectId.put(subjectId, value);
    }

    public double getTotalObtained(List<ExamSubject> subjects) {
        double sum = 0;
        for (ExamSubject s : subjects) {
            Double m = marksBySubjectId.get(s.getId());
            if (m != null) sum += m;
        }
        return sum;
    }

    public int getTotalMax(List<ExamSubject> subjects) {
        int sum = 0;
        for (ExamSubject s : subjects) sum += s.getTotalMarks();
        return sum;
    }

    public double getPercentage(List<ExamSubject> subjects) {
        int max = getTotalMax(subjects);
        if (max == 0) return 0;
        return (getTotalObtained(subjects) / max) * 100.0;
    }

    public String getGrade(List<ExamSubject> subjects) {
        double pct = getPercentage(subjects);
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B+";
        if (pct >= 60) return "B";
        if (pct >= 50) return "C+";
        if (pct >= 40) return "C";
        if (pct >= 33) return "D";
        return "F";
    }


    public String getRemarks(List<ExamSubject> subjects) {
        return getPercentage(subjects) >= 33 ? "PASS" : "FAIL";
    }

    // Per-subject breakdown (used on the printed result card)
    public double getSubjectPercentage(ExamSubject subject) {
        Double mark = marksBySubjectId.get(subject.getId());
        if (mark == null || subject.getTotalMarks() == 0) return 0;
        return (mark / subject.getTotalMarks()) * 100.0;
    }

    public String getSubjectGrade(ExamSubject subject) {
        return gradeFromPercentage(getSubjectPercentage(subject));
    }

    public String getSubjectRemarks(ExamSubject subject) {
        Double mark = marksBySubjectId.get(subject.getId());
        if (mark == null) return "-";
        return getSubjectPercentage(subject) >= 33 ? "PASS" : "FAIL";
    }

    private String gradeFromPercentage(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B+";
        if (pct >= 60) return "B";
        if (pct >= 50) return "C+";
        if (pct >= 40) return "C";
        if (pct >= 33) return "D";
        return "F";
    }
}