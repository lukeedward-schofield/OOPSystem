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
    @FXML private TextField estimatedReturnField;
    @FXML private Button    generateButton;
    @FXML private Button    downloadPdfButton;
    @FXML private Label     feedbackLabel;

    // =========================================================================
    // STATE
    // =========================================================================

    private final PassSlipRepository passSlipRepo = new PassSlipRepository();

    /** Set when the user picks from the search dropdown. Null = no selection yet. */
    private Employee selectedEmployee = null;

    /**
     * The PK returned after a successful INSERT.
     * Gates the Download PDF button and PDF file-path update.
     * Reset to -1 on form clear.
     */
    private int lastIssuedPassSlipId = -1;
    private boolean isSelectingFromDropdown = false;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // =========================================================================
    // INITIALIZE
    // =========================================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Autofill time-out with current time; field is read-only.
        timeOutField.setText(LocalDateTime.now().format(DISPLAY_FMT));
        timeOutField.setEditable(false);

        // PDF button starts disabled until a slip is generated.
        downloadPdfButton.setDisable(true);

        clearFeedback();
        setupLiveSearch();
    }

    // =========================================================================
    // LIVE EMPLOYEE SEARCH
    // =========================================================================

    /**
     * Attaches a text-change listener to searchEmployeeField.
     *
     * Fires a background DB query when the user types 2+ characters.
     * Results appear in a ContextMenu dropdown directly below the field.
     * Selecting an item sets selectedEmployee and fills the field text.
     *
     * Selecting resets selectedEmployee to null when the user edits
     * the field again, preventing stale selections from being submitted.
     */
    private void setupLiveSearch() {

        ContextMenu dropdown = new ContextMenu();

        searchEmployeeField.textProperty().addListener((obs, oldVal, newVal) -> {

            // If this text change was caused by us selecting from the dropdown,
            // skip the reset — the employee is already locked in.
            if (isSelectingFromDropdown) return;

            // Any manual keystroke invalidates the previous selection.
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

        // destination is nullable — store null if blank
        String destination = destinationField.getText().trim();
        String destValue   = destination.isBlank() ? null : destination;

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

        // --- Build and insert ---
        PassSlip slip = new PassSlip(
                selectedEmployee.getEmployeeId(),
                issuedBy,
                reason,
                destValue,
                LocalDateTime.now()   // captured for the model; DB uses NOW()
        );

        int generatedId = passSlipRepo.issuePassSlip(slip);

        if (generatedId == -1) {
            showError("Failed to issue pass slip. Check your database connection.");
            return;
        }

        lastIssuedPassSlipId = generatedId;
        downloadPdfButton.setDisable(false);

        showSuccess("Pass slip #" + generatedId + " issued for "
                + selectedEmployee.getFirstName() + " "
                + selectedEmployee.getLastName() + ".");

        System.out.println("[PassSlip] Issued ID=" + generatedId
                + " employee_id=" + selectedEmployee.getEmployeeId()
                + " issued_by=" + issuedBy);
    }

    // =========================================================================
    // DOWNLOAD PDF
    // =========================================================================

    /**
     * Triggers PDF generation for the most recently issued pass slip.
     *
     * Only enabled after handleGeneratePassSlip() succeeds.
     *
     * PDF generation is stubbed out — plug in iText, PDFBox, or JasperReports
     * inside the marked block below.  Once implemented, updateFilePath()
     * stores the saved path back into the pass_slip row.
     *
     * Wired to: onAction="#handleDownloadPdf" on the DOWNLOAD PDF button.
     */
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
    // NAVIGATION  (mirrors EmployeeDirectoryController pattern exactly)
    // =========================================================================

    @FXML public void goToPassSlipIssuance()  { SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView"); }
    @FXML public void goToEmployeeDirectory() { SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView"); }
    @FXML public void gotoReports()           { SceneNavigator.switchTo("reports/ReportsView"); }

    // Uncomment when those screens are implemented:
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
}