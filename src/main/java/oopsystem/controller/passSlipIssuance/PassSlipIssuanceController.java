package oopsystem.controller.passSlipIssuance;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import oopsystem.model.Employee;
import oopsystem.model.PassSlip;
import oopsystem.repository.PassSlipRepository;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;
import oopsystem.util.AppConfig;
import oopsystem.model.User;
import oopsystem.repository.UserRepository;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for PassSlipIssuanceView.fxml.
 *
 * Responsibilities:
 *   1. Autofill TIME-OUT with the current timestamp on load (read-only).
 *   2. Live-search employees as the user types — dropdown via ContextMenu.
 *   3. Lock in the selected employee when the user picks from the dropdown.
 *   4. Validate all required fields before submitting.
 *   5. Call PassSlipRepository.issuePassSlip() and store the returned ID.
 *   6. Enable DOWNLOAD PDF only after a slip is successfully generated.
 *   7. Handle sidebar navigation (mirrors EmployeeDirectoryController pattern).
 *
 * -- fx:id BINDINGS REQUIRED IN FXML --
 * Add these fx:id attributes to the matching nodes in PassSlipIssuanceView.fxml:
 *
 *   TextField  searchEmployeeField    (SEARCH EMPLOYEE)
 *   TextField  reasonField            (REASON FOR LEAVING)
 *   TextField  destinationField       (DESTINATION)
 *   TextField  timeOutField           (TIME-OUT — read-only)
 *   TextField  estimatedReturnField   (ESTIMATED RETURN — for PDF only, not stored)
 *   Button     generateButton         onAction="#handleGeneratePassSlip"
 *   Button     downloadPdfButton      onAction="#handleDownloadPdf"
 *   Label      feedbackLabel          (status/error line below the form)
 *
 * Sidebar buttons already wired in FXML keep their existing onAction methods.
 * Only add the missing ones listed in the navigation section below.
 */
public class PassSlipIssuanceController implements Initializable {

    // =========================================================================
    // FXML COMPONENTS
    // =========================================================================

    @FXML private TextField searchEmployeeField;
    @FXML private TextField reasonField;
    @FXML private TextField destinationField;
    @FXML private TextField timeOutField;
    @FXML private TextField durationField;
    @FXML private Button    generateButton;
    @FXML private Button    downloadPdfButton;
    @FXML private Label     feedbackLabel;

    // Preview panel — the whole VBox is snapshotted for PDF
    @FXML private VBox  previewPanel;
    @FXML private Label previewSlipNo;
    @FXML private Label previewEmployee;
    @FXML private Label previewDepartment;
    @FXML private Label previewRole;
    @FXML private Label previewReason;
    @FXML private Label previewDestination;
    @FXML private Label previewTimeOut;
    @FXML private Label previewDuration;
    @FXML private Label previewEstReturn;
    @FXML private Label previewIssuedBy;
    @FXML private Label previewGeneratedAt;


    // =========================================================================
    // STATE
    // =========================================================================
    private final PassSlipRepository passSlipRepo = new PassSlipRepository();

    private Employee      selectedEmployee        = null;
    private int           lastIssuedPassSlipId    = -1;
    private boolean       isSelectingFromDropdown = false;
    private LocalDateTime capturedTimeOut;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String OUTPUT_DIR =
            System.getProperty("user.home") + File.separator
                    + "OOPSystem" + File.separator + "passslips";


    // =========================================================================
    // INITIALIZE
    // =========================================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        capturedTimeOut = LocalDateTime.now();
        timeOutField.setText(capturedTimeOut.format(DISPLAY_FMT));
        timeOutField.setEditable(false);

        downloadPdfButton.setDisable(true);
        clearFeedback();
        getEffectiveUser();
        setupLiveSearch();
        setupDurationField();

        reasonField.textProperty().addListener((obs, o, n) -> updatePreview());
        destinationField.textProperty().addListener((obs, o, n) -> updatePreview());

        updatePreview();
    }


    // =========================================================================
    // LIVE EMPLOYEE SEARCH Function
    // =========================================================================

    private void setupLiveSearch() {

        ContextMenu dropdown = new ContextMenu();

        searchEmployeeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isSelectingFromDropdown) return;

            selectedEmployee = null;
            dropdown.hide();
            updatePreview();

            String query = newVal == null ? "" : newVal.trim();
            if (query.length() < 2) return;

            Thread searchThread = new Thread(() -> {

                List<Employee> results = passSlipRepo.searchEmployees(query);

                Platform.runLater(() -> {

                    dropdown.getItems().clear();

                    if (results.isEmpty()) {
                        MenuItem none = new MenuItem("No employees found");
                        none.setDisable(true);
                        dropdown.getItems().add(none);
                    } else {
                        for (Employee emp : results) {
                            String label = emp.getFirstName() + " " + emp.getLastName()
                                    + "  |  " + emp.getDepartment()
                                    + "  ·  " + emp.getRole();

                            MenuItem item = new MenuItem(label);

                            item.setOnAction(e -> {
                                // Raise the flag BEFORE setText so the listener
                                // knows to skip its reset logic this one time.
                                isSelectingFromDropdown = true;
                                selectedEmployee = emp;
                                searchEmployeeField.setText(
                                        emp.getFirstName() + " " + emp.getLastName()
                                );
                                searchEmployeeField.positionCaret(
                                        searchEmployeeField.getText().length()
                                );
                                isSelectingFromDropdown = false; // Lower the flag
                                dropdown.hide();
                                clearFeedback();

                                updatePreview();
                            });

                            dropdown.getItems().add(item);
                        }
                    }

                    if (!dropdown.isShowing()
                            && searchEmployeeField.getScene() != null) {
                        dropdown.show(searchEmployeeField, Side.BOTTOM, 0, 0);
                    }
                });
            });

            searchThread.setDaemon(true);
            searchThread.start();
        });
    }

    // =========================================================================
    // PREVIEW UPDATE
    // =========================================================================

    private void updatePreview() {

        // Slip number — only shown after generation
        if (lastIssuedPassSlipId != -1) {
            previewSlipNo.setText("No. " + lastIssuedPassSlipId);
        } else {
            previewSlipNo.setText("No. —");
        }

        if (selectedEmployee != null) {
            previewEmployee.setText(selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName());
            previewDepartment.setText(selectedEmployee.getDepartment());
            previewRole.setText(selectedEmployee.getRole());
        } else {
            previewEmployee.setText("—");
            previewDepartment.setText("—");
            previewRole.setText("—");
        }

        String reason = reasonField.getText().trim();
        previewReason.setText(reason.isBlank() ? "—" : reason);

        String dest = destinationField.getText().trim();
        previewDestination.setText(dest.isBlank() ? "—" : dest);

        previewTimeOut.setText(capturedTimeOut.format(DISPLAY_FMT));

        String durationText = durationField.getText().trim();
        if (!durationText.isBlank()) {
            try {
                int minutes = Integer.parseInt(durationText);
                if (minutes > 0 && minutes <= 480) {
                    previewDuration.setText(minutes + " min");
                    previewEstReturn.setText(
                            capturedTimeOut.plusMinutes(minutes).format(DISPLAY_FMT)
                    );
                } else {
                    previewDuration.setText("—");
                    previewEstReturn.setText("—");
                }
            } catch (NumberFormatException e) {
                previewDuration.setText("—");
                previewEstReturn.setText("—");
            }
        } else {
            previewDuration.setText("—");
            previewEstReturn.setText("—");
        }

        String issuedBy = SessionManager.getLoggedInFullName();
        previewIssuedBy.setText(issuedBy != null ? issuedBy : "—");

        previewGeneratedAt.setText(LocalDateTime.now().format(DISPLAY_FMT));
    }

    // =========================================================================
    // DURATION FIELD SETUP
    // =========================================================================

    private void setupDurationField() {
        durationField.textProperty().addListener((obs, oldVal, newVal) -> {

            if (newVal != null && !newVal.matches("\\d*")) {
                durationField.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }

            updatePreview();

            if (newVal == null || newVal.isBlank()) {
                clearFeedback();
                return;
            }

            try {
                int minutes = Integer.parseInt(newVal);
                if (minutes > 0 && minutes <= 480) {
                    showInfo("Estimated return: "
                            + capturedTimeOut.plusMinutes(minutes).format(DISPLAY_FMT));
                } else if (minutes > 480) {
                    showError("Duration cannot exceed 480 minutes (8 hours).");
                } else {
                    clearFeedback();
                }
            } catch (NumberFormatException ignored) {
                clearFeedback();
            }
        });
    }

    private User getEffectiveUser() {
        User currentUser = SessionManager.getCurrentUser();

        if (currentUser != null) return currentUser;

        if (AppConfig.DEV_MODE) {
            try {
                UserRepository repo = new UserRepository();
                currentUser = repo.findFirstUser();

                if (currentUser != null) {
                    SessionManager.setCurrentUser(currentUser);
                    System.out.println("[DEV] Session set to user_id="
                            + currentUser.getUserId()
                            + " (" + currentUser.getUsername() + ")");
                } else {
                    System.err.println("[DEV] findFirstUser() returned null — check users table.");
                }

            } catch (Exception e) {
                System.err.println("[DEV] getEffectiveUser() failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return currentUser;
    }

    // =========================================================================
    // GENERATE PASS SLIP
    // =========================================================================

    /**
     * Validates the form, builds a PassSlip model, calls the repository,
     * and enables the Download PDF button on success.
     *
     * Wired to: onAction="#handleGeneratePassSlip" on the Generate button.
     */
    @FXML
    private void handleGeneratePassSlip() {

        // Disable immediately to prevent double-clicks reaching the DB
        generateButton.setDisable(true);
        clearFeedback();

        // 1. Employee must be selected from dropdown
        if (selectedEmployee == null) {
            showError("Please search and select an employee from the list.");
            searchEmployeeField.requestFocus();
            generateButton.setDisable(false);
            return;
        }

        // 2. Reason — required, max 50 chars (varchar(50) NOT NULL in schema)
        String reason = reasonField.getText().trim();
        if (reason.isBlank()) {
            showError("Reason for leaving is required.");
            reasonField.requestFocus();
            generateButton.setDisable(false);
            return;
        }
        if (reason.length() > 50) {
            showError("Reason must be 50 characters or fewer.");
            reasonField.requestFocus();
            generateButton.setDisable(false);
            return;
        }

        // 3. Destination — optional, max 255 chars
        String destination = destinationField.getText().trim();
        if (destination.length() > 255) {
            showError("Destination is too long (max 255 characters).");
            destinationField.requestFocus();
            generateButton.setDisable(false);
            return;
        }
        String destValue = destination.isBlank() ? null : destination;

        // 4. Duration — required, digits only, 1–480 minutes
        String durationText = durationField.getText().trim();
        if (durationText.isBlank()) {
            showError("Duration is required. Enter the number of minutes.");
            durationField.requestFocus();
            generateButton.setDisable(false);
            return;
        }

        int durationMinutes;
        try {
            durationMinutes = Integer.parseInt(durationText);
        } catch (NumberFormatException e) {
            showError("Duration must be a whole number.");
            durationField.requestFocus();
            generateButton.setDisable(false);
            return;
        }

        if (durationMinutes <= 0) {
            showError("Duration must be at least 1 minute.");
            durationField.requestFocus();
            generateButton.setDisable(false);
            return;
        }
        if (durationMinutes > 480) {
            showError("Duration cannot exceed 480 minutes (8 hours).");
            durationField.requestFocus();
            generateButton.setDisable(false);
            return;
        }

        // 5. Session — Option A bypass while LoginController is unfinished
        User currentUser = getEffectiveUser();

        if (currentUser == null) {
            showError("No logged-in user found. Please log in again.");
            generateButton.setDisable(false);
            return;
        }

        int issuedBy = currentUser.getUserId();

        LocalDateTime estimatedReturn = capturedTimeOut.plusMinutes(durationMinutes);

        // 6. Build model and insert
        //    hasOpenPassSlip() check + insert happen atomically inside the repository
        //    using FOR UPDATE lock — no separate pre-check needed here
        PassSlip slip = new PassSlip(
                selectedEmployee.getEmployeeId(),
                issuedBy,
                reason,
                destValue,
                capturedTimeOut,
                durationMinutes
        );

        int generatedId = passSlipRepo.issuePassSlip(slip, issuedBy);

        // -2 means the DB check found an existing open slip (returned from inside transaction)
        if (generatedId == -2) {
            showError(selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName()
                    + " has an unresolved pass slip (OUT or OVERDUE). Record their return before issuing a new one.");
            generateButton.setDisable(false); // allow selecting a different employee
            return;
        }

        if (generatedId == -1) {
            showError("Failed to issue pass slip. Check your database connection.");
            generateButton.setDisable(false); // allow retry on genuine DB error
            return;
        }

        // Success — button stays disabled until page is reloaded or nav occurs
        lastIssuedPassSlipId = generatedId;
        downloadPdfButton.setDisable(false);
        updatePreview();

        // Show popup alert with Download PDF option
        showSuccessAlert(
                generatedId,
                selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName(),
                estimatedReturn.format(DISPLAY_FMT)
        );

        showSuccess(String.format(
                "Pass slip #%d issued for %s %s. Estimated return by %s.",
                generatedId,
                selectedEmployee.getFirstName(),
                selectedEmployee.getLastName(),
                estimatedReturn.format(DISPLAY_FMT)
        ));
    }

    // =========================================================================
    // SUCCESS POPUP ALERT
    // =========================================================================

    /**
     * Shows a styled confirmation dialog after a pass slip is successfully issued.
     * Uses JavaFX Alert so it's native to the application window.
     *
     * Offers two actions:
     *   - Download PDF immediately
     *   - Close (do nothing)
     */

    private void showSuccessAlert(int slipId, String employeeName, String estimatedReturn) {

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Pass Slip Issued");
        alert.setHeaderText("Pass slip #" + slipId + " issued successfully.");
        alert.setContentText(
                "Employee: " + employeeName + "\n"
                        + "Estimated return: " + estimatedReturn + "\n\n"
                        + "Would you like to download the PDF now?"
        );

        ButtonType downloadBtn = new ButtonType("Download PDF", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeBtn    = new ButtonType("Close",        ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(downloadBtn, closeBtn);

        // Style the dialog header to match the maroon theme
        alert.getDialogPane().setStyle(
                "-fx-font-size: 13;"
        );

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == downloadBtn) {
            handleDownloadPdf();
        }
    }


    // =========================================================================
    // DOWNLOAD PDF (snapshot approach)
    // =========================================================================

    @FXML
    private void handleDownloadPdf() {

        if (lastIssuedPassSlipId == -1) {
            showError("No pass slip generated yet.");
            return;
        }

        if (previewPanel == null) {
            showError("Preview panel not available.");
            return;
        }

        // Build the default filename: EmployeeName-D(YYYY-MM-DD)-PassSlip#
        String dateStr    = capturedTimeOut.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String empName    = selectedEmployee.getFirstName() + selectedEmployee.getLastName();
        String safeName   = empName.replaceAll("[^a-zA-Z0-9]", ""); // strip spaces/special chars
        String defaultFileName = safeName + "-" + dateStr + "-PassSlip" + lastIssuedPassSlipId;

        // File save dialog — same pattern as ReportsAnalyticsController
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Pass Slip");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf")
        );
        fileChooser.setInitialFileName(defaultFileName + ".pdf");

        File file = fileChooser.showSaveDialog(downloadPdfButton.getScene().getWindow());
        if (file == null) return; // user cancelled

        try {
            // --- Render previewPanel off-screen at fixed A4 dimensions ---
            // A4 at 96 DPI = 794 x 1123 px
            final double A4_WIDTH  = 794;
            final double A4_HEIGHT = 1123;

            // Force layout pass at A4 width before snapshotting
            previewPanel.setMinWidth(A4_WIDTH);
            previewPanel.setMaxWidth(A4_WIDTH);
            previewPanel.setPrefWidth(A4_WIDTH);
            previewPanel.applyCss();
            previewPanel.layout();

            // Snapshot at the fixed size
            WritableImage fxImage = previewPanel.snapshot(null, null);

            // Restore responsive sizing
            previewPanel.setMinWidth(-1);
            previewPanel.setMaxWidth(-1);
            previewPanel.setPrefWidth(-1);

            // Convert to AWT BufferedImage
            BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);

// Place snapshot on a white A4 canvas — natural height, not stretched
            int snapW = awtImage.getWidth();
            int snapH = awtImage.getHeight();
            int canvasH = Math.max(snapH, (int) A4_HEIGHT);

            BufferedImage a4Image = new BufferedImage((int) A4_WIDTH, canvasH,
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = a4Image.createGraphics();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, (int) A4_WIDTH, canvasH);
            g2d.drawImage(awtImage, 0, 0, (int) A4_WIDTH, snapH, null);
            g2d.dispose();
            // Write as PDF with the image embedded on an A4 page
            writeImageAsPdf(a4Image, file);

            // Store file path in DB
            passSlipRepo.updateFilePath(lastIssuedPassSlipId, file.getAbsolutePath());

            // Auto-open
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }

            showSuccess("Saved: " + file.getName());

        } catch (Exception e) {
            showError("Failed to save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Embeds a BufferedImage into a minimal valid PDF file at A4 page size.
     * Uses raw PDF syntax — no external library needed.
     * The image is encoded as raw RGB bytes inside a PDF XObject stream.
     */
    private void writeImageAsPdf(BufferedImage image, File outputFile) throws IOException {

        int imgW = image.getWidth();
        int imgH = image.getHeight();

        // Extract raw RGB bytes from the image
        ByteArrayOutputStream rgbStream = new ByteArrayOutputStream();
        for (int y = 0; y < imgH; y++) {
            for (int x = 0; x < imgW; x++) {
                int rgb = image.getRGB(x, y);
                rgbStream.write((rgb >> 16) & 0xFF); // R
                rgbStream.write((rgb >> 8)  & 0xFF); // G
                rgbStream.write(rgb         & 0xFF); // B
            }
        }
        byte[] imgBytes = rgbStream.toByteArray();

        // A4 in PDF points: 595 x 842 pt (72 pt/inch)
        String mediaBox = "0 0 595 842";

        // Image XObject stream header
        String imgDict = "<< /Type /XObject /Subtype /Image"
                + " /Width " + imgW
                + " /Height " + imgH
                + " /ColorSpace /DeviceRGB"
                + " /BitsPerComponent 8"
                + " /Length " + imgBytes.length
                + " >>";

        // Page content: draw the image scaled to fill the A4 page
        // PDF coordinate origin is bottom-left; cm operator: x y w h
        String pageContent = "q 595 0 0 842 0 0 cm /Im1 Do Q";
        byte[] contentBytes = pageContent.getBytes(StandardCharsets.US_ASCII);

        // Build PDF object list
        // 1: Catalog, 2: Pages, 3: Page, 4: Content stream, 5: Font, 6: Image XObject
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        List<Integer> offsets = new ArrayList<>();

        // Object 1 — Catalog
        offsets.add(pdf.size());
        pdf.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
                .getBytes(StandardCharsets.US_ASCII));

        // Object 2 — Pages
        offsets.add(pdf.size());
        pdf.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
                .getBytes(StandardCharsets.US_ASCII));

        // Object 3 — Page
        offsets.add(pdf.size());
        pdf.write(("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [" + mediaBox + "]"
                + " /Contents 4 0 R /Resources << /XObject << /Im1 5 0 R >> >> >>\nendobj\n")
                .getBytes(StandardCharsets.US_ASCII));

        // Object 4 — Page content stream
        offsets.add(pdf.size());
        String contentHeader = "4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n";
        pdf.write(contentHeader.getBytes(StandardCharsets.US_ASCII));
        pdf.write(contentBytes);
        pdf.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));

        // Object 5 — Image XObject (raw RGB)
        offsets.add(pdf.size());
        pdf.write(("5 0 obj\n" + imgDict + "\nstream\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write(imgBytes);
        pdf.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));

        // Cross-reference table
        int xrefStart = pdf.size();
        pdf.write(("xref\n0 " + (offsets.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (int offset : offsets) {
            pdf.write(String.format("%010d 00000 n \n", offset)
                    .getBytes(StandardCharsets.US_ASCII));
        }
        pdf.write(("trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n"
                + "startxref\n" + xrefStart + "\n%%EOF")
                .getBytes(StandardCharsets.US_ASCII));

        Files.write(outputFile.toPath(), pdf.toByteArray());
    }


    // =========================================================================
    // NAVIGATION
    // =========================================================================

    @FXML public void goToPassSlipIssuance()  { SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView"); }
    @FXML public void goToEmployeeDirectory() { SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView"); }
    @FXML public void gotoReports()           { SceneNavigator.switchTo("reports/ReportsView"); }
    // @FXML public void goToDashboard()      { SceneNavigator.switchTo("dashboard/DashboardView"); }
    // @FXML public void goToMovementLogs()   { SceneNavigator.switchTo("movementLogs/MovementLogsView"); }

    @FXML
    public void handleLogout() {
        SessionManager.clearSession();
        SceneNavigator.switchTo("login/Login");
    }

    // =========================================================================
    // FEEDBACK HELPERS
    // =========================================================================

    private void showSuccess(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle("-fx-text-fill: green;");
    }

    private void showError(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle("-fx-text-fill: red;");
    }

    private void clearFeedback() {
        feedbackLabel.setText("");
    }

    private void showInfo(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle("-fx-text-fill: gray;");
    }
}