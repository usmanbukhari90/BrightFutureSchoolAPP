package com.brightfutureschool.model;

public class Student {
    private long id;
    private long classId;
    private String rollNo;
    private String fullName;
    private String fatherName;
    private String motherName;
    private String address;
    private String contact;
    private String photoBase64;

    private String dateOfBirth;      // ISO format yyyy-MM-dd
    private String fatherProfession;
    private String motherProfession;
    private String fatherContact;
    private String gender;           // Male / Female / Prefer not to say
    private String religion;
    private String nationality;
    private String studentBform;     // format: xxxxx-xxxxxxx-x
    private String fatherCnic;       // format: xxxxx-xxxxxxx-x
    private String motherCnic;       // format: xxxxx-xxxxxxx-x

    public Student() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getFatherProfession() { return fatherProfession; }
    public void setFatherProfession(String fatherProfession) { this.fatherProfession = fatherProfession; }

    public String getMotherProfession() { return motherProfession; }
    public void setMotherProfession(String motherProfession) { this.motherProfession = motherProfession; }

    public String getFatherContact() { return fatherContact; }
    public void setFatherContact(String fatherContact) { this.fatherContact = fatherContact; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getStudentBform() { return studentBform; }
    public void setStudentBform(String studentBform) { this.studentBform = studentBform; }

    public String getFatherCnic() { return fatherCnic; }
    public void setFatherCnic(String fatherCnic) { this.fatherCnic = fatherCnic; }

    public String getMotherCnic() { return motherCnic; }
    public void setMotherCnic(String motherCnic) { this.motherCnic = motherCnic; }

}