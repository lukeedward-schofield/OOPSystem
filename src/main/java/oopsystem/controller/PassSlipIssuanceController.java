package oopsystem.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.*;
import oopsystem.model.Employee;
import oopsystem.model.PassSlip;
import oopsystem.repository.PassSlipRepository;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    // =========================================================================
    // STATE
    // =========================================================================

    private final PassSlipRepository passSlipRepo = new PassSlipRepository();
    private Employee selectedEmployee = null;
    private int lastIssuedPassSlipId = -1;
    private boolean isSelectingFromDropdown = false;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private LocalDateTime capturedTimeOut;
    // =========================================================================
    // INITIALIZE
    // =========================================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        capturedTimeOut = LocalDateTime.now();
        timeOutField.setText(LocalDateTime.now().format(DISPLAY_FMT));
        timeOutField.setEditable(false);


        downloadPdfButton.setDisable(true);
        clearFeedback();
        setupLiveSearch();
        setupDurationField();
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
    // DURATION FIELD SETUP
    // =========================================================================

    /**
     * Two behaviours:
     * 1. Strips any non-digit character immediately as the user types.
     * 2. Shows a live gray preview of the estimated return time
     *    (time_out + entered minutes) so staff can confirm before submitting.
     */
    private void setupDurationField() {
        durationField.textProperty().addListener((obs, oldVal, newVal) -> {

            // Strip non-digits (handles paste, keyboard, anything)
            if (newVal != null && !newVal.matches("\\d*")) {
                durationField.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }

            if (newVal == null || newVal.isBlank()) {
                clearFeedback();
                return;
            }

            try {
                int minutes = Integer.parseInt(newVal);
                if (minutes > 0 && minutes <= 480) {
                    LocalDateTime estimated = capturedTimeOut.plusMinutes(minutes);
                    showInfo("Estimated return: " + estimated.format(DISPLAY_FMT));
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

        clearFeedback();

        // --- Guard: employee must be selected from dropdown ---
        if (selectedEmployee == null) {
            showError("Please search and select an employee from the list.");
            searchEmployeeField.requestFocus();
            return;
        }

        // --- Guard: reason is required (varchar 50, NOT NULL in schema) ---
        String reason = reasonField.getText().trim();
        if (reason.isBlank()) {
            showError("Reason for leaving is required.");
            reasonField.requestFocus();
            return;
        }
        if (reason.length() > 50) {
            showError("Reason must be 50 characters or fewer.");
            reasonField.requestFocus();
            return;
        }

        String destination = destinationField.getText().trim();
        if (destination.length() > 255) {
            showError("Destination is too long (max 255 characters).");
            destinationField.requestFocus();
            return;
        }
        // destination is nullable — store null if blank
        String destValue   = destination.isBlank() ? null : destination;

        String durationText = durationField.getText().trim();
        if (durationText.isBlank()) {
            showError("Duration is required. Enter the number of minutes.");
            durationField.requestFocus();
            return;
        }

        // Duration — required, digits only, 1–480 minutes
        int durationMinutes;
        try {
            durationMinutes = Integer.parseInt(durationText);
        } catch (NumberFormatException e) {
            showError("Duration must be a whole number.");
            durationField.requestFocus();
            return;
        }

        if (durationMinutes <= 0) {
            showError("Duration must be at least 1 minute.");
            durationField.requestFocus();
            return;
        }
        if (durationMinutes > 480) {
            showError("Duration cannot exceed 480 minutes (8 hours).");
            durationField.requestFocus();
            return;
        }
        // --- Guard: session must be active (issued_by FK to users) ---
//        int issuedBy = SessionManager.getLoggedInUserId();
//        if (issuedBy == -1) {
//            showError("Session expired. Please log in again.");
//            SceneNavigator.switchTo("login/Login");
//            return;
//        }

        // --- Guard: session must be active (issued_by FK to users) ---
        int issuedBy = SessionManager.getLoggedInUserId();
        if (issuedBy == -1) {
            issuedBy = 1; // TODO: remove this when LoginController is fully wired
        }

        // Calculate estimated return for display (not stored separately)
        LocalDateTime estimatedReturn = capturedTimeOut.plusMinutes(durationMinutes);

        // Build model and insert (activity log written inside repository)
        PassSlip slip = new PassSlip(
                selectedEmployee.getEmployeeId(),
                issuedBy,
                reason,
                destValue,
                capturedTimeOut,
                durationMinutes
        );

        int generatedId = passSlipRepo.issuePassSlip(slip, issuedBy);

        if (generatedId == -1) {
            showError("Failed to issue pass slip. Check your database connection.");
            return;
        }

        lastIssuedPassSlipId = generatedId;
        downloadPdfButton.setDisable(false);

        showSuccess(String.format(
                "Pass slip #%d issued for %s %s. Estimated return by %s.",
                generatedId,
                selectedEmployee.getFirstName(),
                selectedEmployee.getLastName(),
                estimatedReturn.format(DISPLAY_FMT)
        ));
    }

    // =========================================================================
    // DOWNLOAD PDF
    // =========================================================================

    @FXML
    private void handleDownloadPdf() {

        if (lastIssuedPassSlipId == -1) {
            showError("No pass slip generated yet.");
            return;
        }

        PassSlip slip = passSlipRepo.findById(lastIssuedPassSlipId);
        if (slip == null) {
            showError("Could not load pass slip record for PDF generation.");
            return;
        }

        // ----------------------------------------------------------------
        // PDF GENERATION — implement here when PDF library is added
        // ----------------------------------------------------------------
        // String fileName  = "pass_slip_" + lastIssuedPassSlipId + ".pdf";
        // String outputDir = "passslips/";
        // String fullPath  = outputDir + fileName;
        //
        // boolean ok = PassSlipPdfGenerator.generate(
        //         slip,
        //         selectedEmployee,
        //         estimatedReturnField.getText().trim(),
        //         fullPath
        // );
        //
        // if (ok) {
        //     passSlipRepo.updateFilePath(lastIssuedPassSlipId, fullPath);
        //     showSuccess("PDF saved: " + fullPath);
        // } else {
        //     showError("PDF generation failed.");
        // }
        // ----------------------------------------------------------------

        // Temporary stub
        System.out.println("[PassSlip] PDF requested for pass_slip_ID=" + lastIssuedPassSlipId);
        showSuccess("PDF generation ready — plug in your PDF library to activate.");
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