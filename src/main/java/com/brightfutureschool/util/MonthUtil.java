package com.brightfutureschool.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class MonthUtil {
    public static String format(String yyyyMM) {
        try {
            YearMonth ym = YearMonth.parse(yyyyMM);
            return ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        } catch (Exception e) {
            return yyyyMM;
        }
    }
}