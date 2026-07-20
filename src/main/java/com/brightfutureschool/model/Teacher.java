package com.brightfutureschool.model;

public class Teacher {
    private long id;
    private int srNo;
    private String fullName;
    private String fatherName;
    private String contact;
    private String dateOfBirth;
    private String cnic;
    private String qualification;
    private String previousExperience;
    private String nationality;
    private String religion;
    private String photoBase64;
    private Long assignedClassId; // nullable -- teacher may not be assigned yet
    private String subject;
    private double salary;

    public Teacher() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getSrNo() { return srNo; }
    public void setSrNo(int srNo) { this.srNo = srNo; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getCnic() { return cnic; }
    public void setCnic(String cnic) { this.cnic = cnic; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getPreviousExperience() { return previousExperience; }
    public void setPreviousExperience(String previousExperience) { this.previousExperience = previousExperience; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public Long getAssignedClassId() { return assignedClassId; }
    public void setAssignedClassId(Long assignedClassId) { this.assignedClassId = assignedClassId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
}