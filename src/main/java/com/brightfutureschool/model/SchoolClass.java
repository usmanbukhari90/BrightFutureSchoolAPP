package com.brightfutureschool.model;

public class SchoolClass {
    private long id;
    private String className;
    private String section;
    private int rollBase;

    public SchoolClass() {}

    public SchoolClass(String className, String section) {
        this.className = className;
        this.section = section;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public int getRollBase() { return rollBase; }
    public void setRollBase(int rollBase) { this.rollBase = rollBase; }

    @Override
    public String toString() { return className + " - " + section; }
}