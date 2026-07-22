package com.brightfutureschool.controller.result;

import com.brightfutureschool.config.SchoolInfo;
import com.brightfutureschool.model.Exam;
import com.brightfutureschool.model.ExamSubject;
import com.brightfutureschool.model.ResultRow;
import com.brightfutureschool.model.SchoolClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class ResultCardController {

    @FXML private ImageView logoView;
    @FXML private ImageView watermarkView;
    @FXML private Label schoolNameLabel;
    @FXML private Label addressLabel;
    @FXML private Label phone1Label;
    @FXML private Label phone2Label;

    @FXML private Label examTitleLabel;
    @FXML private Label classHeadingLabel;
    @FXML private Label rollNoLabelValue;
    @FXML private Label studentNameLabelValue;
    @FXML private Label fatherNameLabelValue;
    @FXML private ImageView studentPhotoView;

    @FXML private GridPane subjectsGrid;

    @FXML private Label totalValue;
    @FXML private Label percentageValue;
    @FXML private Label gradeValue;
    @FXML private Label remarksValue;

    @FXML private javafx.scene.layout.StackPane cardRoot;

    private ResultRow resultRow;
    private List<ExamSubject> subjects;

    public void setData(SchoolClass schoolClass, Exam exam, List<ExamSubject> subjects, ResultRow resultRow) {
        this.resultRow = resultRow;
        this.subjects = subjects;

        schoolNameLabel.setText(SchoolInfo.SCHOOL_NAME);
        addressLabel.setText(SchoolInfo.ADDRESS);
        phone1Label.setText(SchoolInfo.PHONE_1);
        phone2Label.setText(SchoolInfo.PHONE_2);

        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            Image logo = new Image(logoUrl.toExternalForm());
            logoView.setImage(logo);
            watermarkView.setImage(logo);
        }

        examTitleLabel.setText(exam.getExamName() + (exam.getExamYear() != null ? " - " + exam.getExamYear() : ""));
        classHeadingLabel.setText(schoolClass.getClassName() + " - " + schoolClass.getSection());
        rollNoLabelValue.setText(resultRow.getStudent().getRollNo());
        studentNameLabelValue.setText(resultRow.getStudent().getFullName());
        fatherNameLabelValue.setText(resultRow.getStudent().getFatherName());

        String photo = resultRow.getStudent().getPhotoBase64();
        if (photo != null && !photo.isEmpty()) {
            try {
                studentPhotoView.setImage(new Image(photo));
            } catch (Exception ignored) { }
        }

        buildSubjectsTable();

        totalValue.setText((int) resultRow.getTotalObtained(subjects) + " / " + resultRow.getTotalMax(subjects));
        percentageValue.setText(String.format("%.1f%%", resultRow.getPercentage(subjects)));
        gradeValue.setText(resultRow.getGrade(subjects));
        remarksValue.setText(resultRow.getRemarks(subjects));
    }

    private void buildSubjectsTable() {
        subjectsGrid.getChildren().clear();
        subjectsGrid.getRowConstraints().clear();

        String[] headers = {"SUBJECT", "TOTAL MARKS", "MARKS OBTAINED", "PERCENTAGE", "GRADE"};
        for (int col = 0; col < headers.length; col++) {
            addCell(headers[col], col, 0, true, true);
        }
        subjectsGrid.getRowConstraints().add(new RowConstraints(32));

        int rowIndex = 1;
        for (ExamSubject subject : subjects) {
            Double mark = resultRow.getMark(subject.getId());
            String markText = mark == null ? "-" : (mark == mark.intValue() ? String.valueOf(mark.intValue()) : String.valueOf(mark));

            addCell(subject.getSubjectName(), 0, rowIndex, false, false);
            addCell(String.valueOf(subject.getTotalMarks()), 1, rowIndex, true, false);
            addCell(markText, 2, rowIndex, true, false);
            addCell(mark == null ? "-" : String.format("%.1f%%", resultRow.getSubjectPercentage(subject)), 3, rowIndex, true, false);
            addCell(resultRow.getSubjectGrade(subject), 4, rowIndex, true, false);

            subjectsGrid.getRowConstraints().add(new RowConstraints(28));
            rowIndex++;
        }
    }

    private void addCell(String text, int col, int row, boolean center, boolean isHeader) {
        Label label = new Label(text);
        String base = "-fx-border-color: #cccccc; -fx-border-width: 0.5; -fx-padding: 5;";
        if (isHeader) {
            label.setStyle(base + " -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #7B5B3E;");
        } else {
            label.setStyle(base + " -fx-font-size: 11px; -fx-text-fill: #2C3E50;-fx-background-color: transparent;");
        }
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.setAlignment(center ? javafx.geometry.Pos.CENTER : javafx.geometry.Pos.CENTER_LEFT);
        GridPane.setColumnIndex(label, col);
        GridPane.setRowIndex(label, row);
        GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setVgrow(label, Priority.ALWAYS);
        subjectsGrid.getChildren().add(label);
    }
    @FXML
    private void onPrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean proceed = job.showPrintDialog(cardRoot.getScene().getWindow());
            if (proceed) {
                boolean success = job.printPage(cardRoot);
                if (success) {
                    job.endJob();
                }
            }
        }
    }

    @FXML
    private void onSaveAsPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(resultRow.getStudent().getRollNo() + "_result_card.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
        File file = chooser.showSaveDialog(cardRoot.getScene().getWindow());
        if (file == null) return;

        try {
            // Render at 3x scale for a crisp, high-resolution PDF instead of a blurry screen-res capture
            double scale = 3.0;
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setTransform(javafx.scene.transform.Transform.scale(scale, scale));

            double scaledWidth = cardRoot.getBoundsInLocal().getWidth() * scale;
            double scaledHeight = cardRoot.getBoundsInLocal().getHeight() * scale;

            WritableImage fxImage = new WritableImage((int) Math.ceil(scaledWidth), (int) Math.ceil(scaledHeight));
            cardRoot.snapshot(params, fxImage);

            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);

            try (PDDocument document = new PDDocument()) {
                // Page size stays at the card's real (unscaled) dimensions in points,
                // while the embedded image keeps its full high-res pixel data — sharp when zoomed/printed.
                float pageWidth = (float) cardRoot.getBoundsInLocal().getWidth();
                float pageHeight = (float) cardRoot.getBoundsInLocal().getHeight();

                PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                document.addPage(page);

                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, pageWidth, pageHeight);
                }

                document.save(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void onClose() {
        ((Stage) cardRoot.getScene().getWindow()).close();
    }
}