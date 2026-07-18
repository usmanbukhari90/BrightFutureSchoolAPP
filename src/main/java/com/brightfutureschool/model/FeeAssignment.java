package com.brightfutureschool.model;

public class FeeAssignment {
    private String feeType;
    private String month; // yyyy-MM

    public FeeAssignment(String feeType, String month) {
        this.feeType = feeType;
        this.month = month;
    }

    public String getFeeType() { return feeType; }
    public String getMonth() { return month; }

    @Override
    public String toString() {
        return feeType + " - " + com.brightfutureschool.util.MonthUtil.format(month);
    }
}