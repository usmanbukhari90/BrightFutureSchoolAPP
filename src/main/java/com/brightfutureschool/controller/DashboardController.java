package com.brightfutureschool.controller;
import com.brightfutureschool.dao.local.AttendanceDao;
import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.dao.local.StudentDao;
import com.brightfutureschool.model.FeeRecord;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

    @FXML private Label greetingLabel;
    @FXML private Label dateLabel;
    @FXML private Label hTens, hUnits, mTens, mUnits, sTens, sUnits, ampmLabel;

    @FXML private Label totalStudentsLabel;
    @FXML private Label totalTeachersLabel;
    @FXML private Label feeCollectedLabel;
    @FXML private Label attendanceLabel;
    @FXML private Label upcomingExamLabel;

    @FXML private ComboBox<SchoolClass> classDropdown;
    @FXML private PieChart studentsPieChart;
    @FXML private BarChart<String, Number> attendanceBarChart;
    @FXML private AreaChart<String, Number> revenueChart;
    @FXML private GridPane calendarGrid;

    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final ClassDao classDao = new ClassDao();
    private final StudentDao studentDao = new StudentDao();
    private final FeeDao feeDao = new FeeDao();

    @FXML
    public void initialize() {
        setupGreeting();
        setupClock();
        setupCalendar();
        loadStatCards();
        setupClassDropdown();
        setupAttendanceChart();
        setupRevenueChart();
    }

    private void setupGreeting() {
        int hour = LocalDateTime.now().getHour();
        greetingLabel.setText(hour < 12 ? "Good Morning, Admin" : hour < 17 ? "Good Afternoon, Admin" : "Good Evening, Admin");
        dateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));

        greetingLabel.setTranslateX(-80);
        greetingLabel.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(600), greetingLabel);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));
        FadeTransition fade = new FadeTransition(Duration.millis(600), greetingLabel);
        fade.setFromValue(0);
        fade.setToValue(1);
        slide.play();
        fade.play();
    }

    private void setupClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        updateClock();
    }

    private void updateClock() {
        LocalTime now = LocalTime.now();
        int hour12 = now.getHour() % 12 == 0 ? 12 : now.getHour() % 12;
        String hh = String.format("%02d", hour12);
        String mm = String.format("%02d", now.getMinute());
        String ss = String.format("%02d", now.getSecond());
        hTens.setText(String.valueOf(hh.charAt(0)));
        hUnits.setText(String.valueOf(hh.charAt(1)));
        mTens.setText(String.valueOf(mm.charAt(0)));
        mUnits.setText(String.valueOf(mm.charAt(1)));
        sTens.setText(String.valueOf(ss.charAt(0)));
        sUnits.setText(String.valueOf(ss.charAt(1)));
        ampmLabel.setText(now.getHour() < 12 ? "AM" : "PM");
    }

    private void setupCalendar() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        int daysInMonth = today.lengthOfMonth();
        int startCol = firstOfMonth.getDayOfWeek().getValue() % 7;

        String[] dayNames = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLbl = new Label(dayNames[i]);
            dayLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-opacity: 0.6;");
            calendarGrid.add(dayLbl, i, 0);
        }

        int row = 1, col = startCol;
        for (int day = 1; day <= daysInMonth; day++) {
            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setPrefWidth(28);
            dayLabel.setAlignment(Pos.CENTER);
            if (day == today.getDayOfMonth()) {
                dayLabel.setStyle("-fx-background-color: #7B5B3E; -fx-text-fill: white; -fx-background-radius: 50%; -fx-font-weight: bold;");
            }
            calendarGrid.add(dayLabel, col, row);
            col++;
            if (col > 6) { col = 0; row++; }
        }
    }


}