package com.brightfutureschool.controller.fee;

import com.brightfutureschool.dao.local.ClassDao;
import com.brightfutureschool.dao.local.FeeDao;
import com.brightfutureschool.model.FeeRecord;
import com.brightfutureschool.model.SchoolClass;
import com.brightfutureschool.model.Student;
import com.brightfutureschool.util.MonthUtil;
import javafx.fxml.FXML;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import java.awt.image.BufferedImage;
import java.io.File;
import javafx.scene.control.Alert;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReceiptController {

    @FXML private VBox receiptRoot;
    @FXML private ImageView logoImage;
    @FXML private Label studentNameLabel, fatherNameLabel, classLabel, monthLabel, rollNoLabel;
    @FXML private Label receiptNoLabel, datePaidLabel;
    @FXML private GridPane feeTableGrid;
    @FXML private Label totalLabel, paidLabel, remainingLabel;
    @FXML private HBox actionButtonsBox;

    private final FeeDao feeDao = new FeeDao();
    private final ClassDao classDao = new ClassDao();

    // Always shown in this order, even if amount is 0
    private static final String[] DISPLAY_ORDER = {
            "Admission Fee", "School Fee", "Academy Fee", "Paper Money", "Arrears / Balance"
    };

    @FXML
    public void initialize() {
        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) logoImage.setImage(new Image(logoUrl.toExternalForm()));
    }

    public void loadReceipt(Student student, String month) {
        try {
            SchoolClass schoolClass = classDao.getAllClasses().stream()
                    .filter(c -> c.getId() == student.getClassId())
                    .findFirst().orElse(null);

            List<FeeRecord> records = feeDao.getCurrentMonthForStudent(student.getId())
                    .stream().filter(r -> month.equals(r.getMonth())).toList();

            double refund = feeDao.getRefundsForStudentMonth(student.getId(), month);
            String receiptNo = feeDao.getOrCreateReceiptNumber(student.getId(), month);

            studentNameLabel.setText(student.getFullName());
            fatherNameLabel.setText("S/O / D/O: " + student.getFatherName());
            classLabel.setText("Class: " + (schoolClass != null ? schoolClass.toString() : "-"));
            monthLabel.setText("Month: " + MonthUtil.format(month));
            rollNoLabel.setText("Roll No: " + student.getRollNo());
            receiptNoLabel.setText(receiptNo);

            String latestPaidDate = records.stream()
                    .filter(r -> r.getPaidDate() != null)
                    .map(FeeRecord::getPaidDate)
                    .max(String::compareTo)
                    .orElse(java.time.LocalDate.now().toString());
            datePaidLabel.setText(latestPaidDate);

            buildFeeTable(records, refund);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildFeeTable(List<FeeRecord> records, double refund) {
        feeTableGrid.getChildren().clear();

        addCell("Fee Description", 0, 0, true);
        addCell("Amount", 1, 0, true);

        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        for (String type : DISPLAY_ORDER) categoryTotals.put(type, 0.0);
        categoryTotals.put("Other", 0.0);

        double total = 0, paid = 0;
        StringBuilder otherDetail = new StringBuilder();

        for (FeeRecord r : records) {
            boolean known = false;
            for (String type : DISPLAY_ORDER) {
                if (type.equalsIgnoreCase(r.getFeeType()) || "Tuition Fee".equalsIgnoreCase(r.getFeeType()) && type.equals("School Fee")) {
                    categoryTotals.merge(type, r.getAmount(), Double::sum);
                    known = true;
                    break;
                }
            }
            if (!known) {
                categoryTotals.merge("Other", r.getAmount(), Double::sum);
                otherDetail.append(r.getFeeType()).append(" ");
            }
            total += r.getAmount();
            if ("PAID".equals(r.getStatus())) paid += r.getAmount();
        }

        int row = 1;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            String label = entry.getKey().equals("Other") && !otherDetail.isEmpty()
                    ? "Other (" + otherDetail.toString().trim() + ")"
                    : entry.getKey();
            if (entry.getKey().equals("Other") && otherDetail.isEmpty()) continue; // hide empty "Other" row
            addCell(label, 0, row, false);
            addCell("Rs. " + (int) (double) entry.getValue(), 1, row, false);
            row++;
        }

        addCell("Refund", 0, row, false);
        addCell("Rs. " + (int) refund, 1, row, false);
        row++;

        totalLabel.setText("Rs. " + (int) total);
        paidLabel.setText("Rs. " + (int) paid);
        remainingLabel.setText("Rs. " + (int) (total - paid - refund));
    }

    private void addCell(String text, int col, int row, boolean header) {
        Label label = new Label(text);
        label.setStyle((header
                ? "-fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #7B5B3E;"
                : "-fx-text-fill: #2C3E50;") + " -fx-padding: 6 10 6 10; -fx-font-size: 11px;");
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(label, Priority.ALWAYS);
        feeTableGrid.add(label, col, row);
    }

    @FXML
    private void onSaveAsPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Fee Receipt");
        chooser.setInitialFileName(receiptNoLabel.getText() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(receiptRoot.getScene().getWindow());
        if (file == null) return;

        try {
            double logicalWidth = receiptRoot.getBoundsInParent().getWidth();
            double logicalHeight = receiptRoot.getBoundsInParent().getHeight();

            // Render at 3x resolution for crisp, high-quality PDF output
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(javafx.scene.transform.Transform.scale(3, 3));
            actionButtonsBox.setVisible(false);
            actionButtonsBox.setManaged(false);

            WritableImage fxImage = receiptRoot.snapshot(params,
                    new WritableImage((int) (logicalWidth * 3), (int) (logicalHeight * 3)));

            actionButtonsBox.setVisible(true);
            actionButtonsBox.setManaged(true);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bufferedImage);

                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float scale = (float) Math.min(pageWidth / logicalWidth, pageHeight / logicalHeight);
                float drawWidth = (float) (logicalWidth * scale);
                float drawHeight = (float) (logicalHeight * scale);
                float x = (pageWidth - drawWidth) / 2;
                float y = pageHeight - drawHeight;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, x, y, drawWidth, drawHeight);
                }
                doc.save(file);
            }

            Alert done = new Alert(Alert.AlertType.INFORMATION);
            done.setTitle("Saved");
            done.setContentText("Receipt saved to:\n" + file.getAbsolutePath());
            done.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void onPrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;


        Printer printer = job.getPrinter();
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(pageLayout);

        boolean proceed = job.showPrintDialog(receiptRoot.getScene().getWindow());
        if (!proceed) return;

        actionButtonsBox.setVisible(false);
        actionButtonsBox.setManaged(false);

        double scaleX = pageLayout.getPrintableWidth() / receiptRoot.getBoundsInParent().getWidth();
        Scale scale = new Scale(scaleX, scaleX);
        receiptRoot.getTransforms().add(scale);
        boolean success = job.printPage(pageLayout, receiptRoot);
        receiptRoot.getTransforms().remove(scale);

        actionButtonsBox.setVisible(true);
        actionButtonsBox.setManaged(true);

        if (success) job.endJob();
    }


    @FXML
    private void onClose() {
        Stage stage = (Stage) receiptRoot.getScene().getWindow();
        stage.close();
    }
}