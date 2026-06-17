package oopsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import oopsystem.model.MovementLog;
import oopsystem.repository.MovementLogRepository;
import oopsystem.util.SceneNavigator;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.stage.FileChooser;

public class MovementLogController {

    /* NAVIGATION */
    @FXML public void goToDashboard()         { SceneNavigator.switchTo("DashboardView"); }
    @FXML public void goToPassSlipIssuance()  { SceneNavigator.switchTo("PassSlipIssuanceView"); }
    @FXML public void goToMovementLogs()      { SceneNavigator.switchTo("MovementlogView"); }
    @FXML public void goToEmployeeDirectory() { SceneNavigator.switchTo("EmployeeDirectoryView"); }
    @FXML public void gotoReports()           { SceneNavigator.switchTo("ReportsView"); }

    /* FILTERS */
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private TextField employeeNameFilter;
    @FXML private Button exportExcelBtn;
    @FXML private Button exportPdfBtn;

    /* STAT CARDS */
    @FXML private Label totalMovementsLabel;
    @FXML private Label currentlyOutLabel;
    @FXML private Label complianceRateLabel;

    /* TABLE */
    @FXML private TableView<MovementLog>           movementLogsTable;
    @FXML private TableColumn<MovementLog, String> dateColumn;
    @FXML private TableColumn<MovementLog, String> employeeColumn;
    @FXML private TableColumn<MovementLog, String> reasonColumn;
    @FXML private TableColumn<MovementLog, String> timeOutColumn;
    @FXML private TableColumn<MovementLog, String> timeInColumn;
    @FXML private TableColumn<MovementLog, String> durationColumn;
    @FXML private TableColumn<MovementLog, String> statusColumn;

    /* PAGINATION */
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Label  pageInfoLabel;

    /* DETAIL CARD */
    @FXML private VBox  detailCard;
    @FXML private Label detailTransactionId;
    @FXML private Label detailEmployee;
    @FXML private Label detailDepartment;
    @FXML private Label detailTimestamp;
    @FXML private Label detailStatus;
    @FXML private Label detailNotes;

    /* STATE */
    private final ObservableList<MovementLog> masterData   = FXCollections.observableArrayList();
    private final ObservableList<MovementLog> filteredData = FXCollections.observableArrayList();
    private final ObservableList<MovementLog> pageData     = FXCollections.observableArrayList();
    private final MovementLogRepository repository = new MovementLogRepository();

    private static final int ROWS_PER_PAGE = 10;
    private int currentPage = 1;
    private int totalPages  = 1;

    private final DateTimeFormatter dateFmt      = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFmt      = DateTimeFormatter.ofPattern("hh:mm a");
    private final DateTimeFormatter cardStampFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy | hh:mm a");

    /* ------------------------------------------------------------------ */
    @FXML
    public void initialize() {
        setupTable();
        movementLogsTable.setItems(pageData);
        loadMovementLogs();
        setupAutoFiltering();
        setupSelectionListener();
        setupClickOutsideListener();
    }

    /* ------------------------------------------------------------------ */
    /*  TABLE COLUMNS                                                       */
    /* ------------------------------------------------------------------ */
    private void setupTable() {
        dateColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTimeOut() == null ? "-"
                        : d.getValue().getTimeOut().format(dateFmt)));

        employeeColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEmployeeName()));

        reasonColumn.setCellValueFactory(d -> {
            String dest   = d.getValue().getDestination();
            String reason = d.getValue().getReason();
            return new SimpleStringProperty(
                    (dest != null && !dest.isBlank()) ? reason + " - " + dest : reason);
        });

        timeOutColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTimeOut() == null ? "-"
                        : d.getValue().getTimeOut().format(timeFmt)));

        timeInColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTimeIn() == null ? "-"
                        : d.getValue().getTimeIn().format(timeFmt)));

        durationColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstimatedDuration() + " min"));

        // STATUS column — shows RETURNED LATE, RETURNED, OVERDUE, OUT
        statusColumn.setCellValueFactory(d -> {
            MovementLog log = d.getValue();
            // returned late — use is_late flag from DB
            if ("RETURNED".equals(log.getPassStatus()) && log.isLate()) {
                return new SimpleStringProperty("RETURNED LATE");
            }
            return new SimpleStringProperty(log.getPassStatus());
        });

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    String color = switch (status) {
                        case "RETURNED"      -> "#16a34a";
                        case "RETURNED LATE" -> "#FFBB00";
                        case "OVERDUE"       -> "#ea580c";
                        default              -> "#cc0000"; // OUT
                    };
                    setStyle("-fx-text-fill: " + color + "; "
                            + "-fx-font-weight: bold; "
                            + "-fx-background-color: transparent;");
                }
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /*  LOAD & FILTER                                                       */
    /* ------------------------------------------------------------------ */
    private void loadMovementLogs() {
        masterData.setAll(repository.getAllMovementLogs());
        filteredData.setAll(masterData);
        loadDepartments();
        currentPage = 1;
        refreshPage();
    }

    private void loadDepartments() {
        departmentFilter.getItems().clear();
        departmentFilter.getItems().add("All Departments");
        masterData.stream().map(MovementLog::getDepartment)
                .distinct().sorted()
                .forEach(departmentFilter.getItems()::add);
        departmentFilter.setValue("All Departments");
    }

    private void setupAutoFiltering() {
        departmentFilter.setOnAction(e -> {
            if (departmentFilter.getValue() != null) applyFilters();
        });
        startDatePicker.setOnAction(e -> applyFilters());
        endDatePicker.setOnAction(e -> applyFilters());
        employeeNameFilter.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void setupClickOutsideListener() {
        movementLogsTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                    javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
                    while (target != null) {
                        if (target instanceof Button
                                || target instanceof TextField
                                || target instanceof ComboBox
                                || target instanceof TableView) {
                            return;
                        }
                        target = target.getParent();
                    }
                    movementLogsTable.getSelectionModel().clearSelection();
                });
            }
        });
    }

    @FXML private void handleApplyFilters() { applyFilters(); }

    private void applyFilters() {
        List<MovementLog> result = masterData.stream()
                .filter(this::matchesDate)
                .filter(this::matchesDepartment)
                .filter(this::matchesEmployee)
                .collect(Collectors.toList());
        filteredData.setAll(result);
        currentPage = 1;
        refreshPage();
    }

    private boolean matchesDate(MovementLog log) {
        if (log.getCreatedAt() == null) return true;
        LocalDate d = log.getCreatedAt().toLocalDate();
        if (startDatePicker.getValue() != null && d.isBefore(startDatePicker.getValue())) return false;
        if (endDatePicker.getValue()   != null && d.isAfter(endDatePicker.getValue()))    return false;
        return true;
    }

    private boolean matchesDepartment(MovementLog log) {
        String sel = departmentFilter.getValue();
        return sel == null || sel.equals("All Departments")
                || (log.getDepartment() != null && log.getDepartment().equalsIgnoreCase(sel));
    }

    private boolean matchesEmployee(MovementLog log) {
        String kw = employeeNameFilter.getText();
        return kw == null || kw.isBlank()
                || log.getEmployeeName().toLowerCase().contains(kw.toLowerCase());
    }

    /* ------------------------------------------------------------------ */
    /*  PAGINATION                                                          */
    /* ------------------------------------------------------------------ */
    private void refreshPage() {
        int total = filteredData.size();
        totalPages = Math.max(1, (int) Math.ceil((double) total / ROWS_PER_PAGE));
        if (currentPage > totalPages) currentPage = totalPages;

        int from = (currentPage - 1) * ROWS_PER_PAGE;
        int to   = Math.min(from + ROWS_PER_PAGE, total);

        pageData.setAll(filteredData.subList(from, to));

        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);

        clearDetailCard();
        updateStatistics();
    }

    @FXML private void handlePrevPage() { if (currentPage > 1)         { currentPage--; refreshPage(); } }
    @FXML private void handleNextPage() { if (currentPage < totalPages) { currentPage++; refreshPage(); } }

    /* ------------------------------------------------------------------ */
    /*  DETAIL CARD                                                         */
    /* ------------------------------------------------------------------ */
    private void setupSelectionListener() {
        movementLogsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) populateDetailCard(selected);
                    else                  clearDetailCard();
                });
    }

    private void populateDetailCard(MovementLog log) {
        detailCard.setVisible(true);
        detailCard.setManaged(true);

        detailTransactionId.setText(String.format("#PS-2026-%05d", log.getPassSlipId()));
        detailEmployee.setText(log.getEmployeeName().toUpperCase());
        detailDepartment.setText(log.getDepartment() != null ? log.getDepartment() : "N/A");
        detailTimestamp.setText(log.getCreatedAt() != null
                ? log.getCreatedAt().format(cardStampFmt) : "-");

        // show RETURNED LATE in detail card status too
        String displayStatus = ("RETURNED".equals(log.getPassStatus()) && log.isLate())
                ? "RETURNED LATE" : log.getPassStatus();

        detailStatus.setText(displayStatus);
        detailStatus.setStyle(switch (displayStatus) {
            case "RETURNED"      -> "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
            case "RETURNED LATE" -> "-fx-text-fill: #d97706; -fx-font-weight: bold;";
            case "OVERDUE"       -> "-fx-text-fill: #ea580c; -fx-font-weight: bold;";
            default              -> "-fx-text-fill: #800000; -fx-font-weight: bold;";
        });

        String dest   = (log.getDestination() != null && !log.getDestination().isBlank())
                ? " heading to " + log.getDestination() : "";
        String reason = log.getReason() != null ? log.getReason().toLowerCase() : "unknown reason";

        StringBuilder notes = new StringBuilder();
        notes.append("Employee requested pass slip for ").append(reason).append(dest).append(".");

        // still out and overdue
        if ("OVERDUE".equals(log.getPassStatus()) && log.getTimeOut() != null) {
            long allowedMinutes = log.getEstimatedDuration() + 3;
            long minutesOverdue = java.time.Duration.between(
                    log.getTimeOut().plusMinutes(allowedMinutes),
                    java.time.LocalDateTime.now()
            ).toMinutes();
            notes.append("\n⚠️ Employee is still outside and is overdue by ")
                    .append(minutesOverdue).append(" minute(s).");
        }

        // returned late — use is_late from DB
        if ("RETURNED".equals(log.getPassStatus()) && log.isLate()
                && log.getTimeOut() != null && log.getTimeIn() != null) {
            long allowedMinutes = log.getEstimatedDuration() + 3;
            long actualMinutes  = java.time.Duration.between(
                    log.getTimeOut(), log.getTimeIn()).toMinutes();
            long lateBy = actualMinutes - allowedMinutes;
            notes.append("\n⚠️ Employee returned late by ").append(lateBy).append(" minute(s).");
        }

        detailNotes.setText(notes.toString());
    }

    private void clearDetailCard() {
        detailCard.setVisible(false);
        detailCard.setManaged(false);
    }

    /* ------------------------------------------------------------------ */
    /*  STATS                                                               */
    /* ------------------------------------------------------------------ */
    private void updateStatistics() {
        long out      = filteredData.stream().filter(l -> "OUT".equals(l.getPassStatus())).count();
        long returned = filteredData.stream().filter(l -> "RETURNED".equals(l.getPassStatus())).count();
        long overdue  = filteredData.stream().filter(l -> "OVERDUE".equals(l.getPassStatus())).count();

        totalMovementsLabel.setText(String.valueOf(filteredData.size()));
        currentlyOutLabel.setText(String.valueOf(out + overdue));
        double rate = filteredData.isEmpty() ? 0 : (returned * 100.0 / filteredData.size());
        complianceRateLabel.setText(String.format("%.1f%%", rate));
    }

    /* ------------------------------------------------------------------ */
    /*  TIME IN                                                             */
    /* ------------------------------------------------------------------ */
    @FXML
    private void handleTimeIn() {
        MovementLog selected = movementLogsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a record first.").show();
            return;
        }

        if ("RETURNED".equals(selected.getPassStatus())) {
            new Alert(Alert.AlertType.WARNING,
                    selected.getEmployeeName() + " has already returned.").show();
            return;
        }

        String msg = "OVERDUE".equals(selected.getPassStatus())
                ? "This employee is OVERDUE.\nAre you sure you want to record TIME IN?"
                : "Record TIME IN for " + selected.getEmployeeName() + "?";

        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noBtn  = new ButtonType("No",  ButtonBar.ButtonData.NO);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Time In");
        confirm.setHeaderText("Time In Confirmation");
        confirm.setContentText(msg);
        confirm.getButtonTypes().setAll(yesBtn, noBtn);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == yesBtn) {
                boolean success = repository.recordTimeIn(selected.getPassSlipId());
                if (success) {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Time In recorded for " + selected.getEmployeeName() + ".").show();
                    loadMovementLogs();
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "Failed to record Time In. Please try again.").show();
                }
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /*  EXPORT                                                              */
    /* ------------------------------------------------------------------ */

    /**
     * Exports movement logs into a real Excel workbook (.xlsx).
     * The user can choose whether to export only the currently filtered rows
     * or every movement log record loaded from the database.
     */
    @FXML
    private void handleExportExcel() {
        showScopeDialog("Excel", (data, label) -> {
            if (data.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Data to Export",
                        "There are no movement log records to export.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Movement Logs Excel File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx")
            );
            fileChooser.setInitialFileName(defaultExportName("movement-logs", label, ".xlsx"));

            File file = fileChooser.showSaveDialog(movementLogsTable.getScene().getWindow());
            if (file == null) {
                showAlert(Alert.AlertType.INFORMATION, "Export Cancelled",
                        "Excel export was cancelled.");
                return;
            }

            try {
                writeMovementLogsWorkbook(data, label, file);
                showAlert(Alert.AlertType.INFORMATION, "Excel Export Successful",
                        data.size() + " movement log record(s) were exported successfully.\n\nSaved as: " + file.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Excel Export Failed",
                        "Failed to export movement logs to Excel.\n\n" + ex.getMessage());
            }
        });
    }

    /**
     * Exports movement logs into a clean PDF table.
     * The PDF uses drawn table borders instead of text separators, so it is
     * easier to read than a plain text export.
     */
    @FXML
    private void handleExportPdf() {
        showScopeDialog("PDF", (data, label) -> {
            if (data.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Data to Export",
                        "There are no movement log records to export.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Movement Logs PDF File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf")
            );
            fileChooser.setInitialFileName(defaultExportName("movement-logs", label, ".pdf"));

            File file = fileChooser.showSaveDialog(movementLogsTable.getScene().getWindow());
            if (file == null) {
                showAlert(Alert.AlertType.INFORMATION, "Export Cancelled",
                        "PDF export was cancelled.");
                return;
            }

            try {
                writeMovementLogsPdf(data, label, file);
                showAlert(Alert.AlertType.INFORMATION, "PDF Export Successful",
                        data.size() + " movement log record(s) were exported successfully.\n\nSaved as: " + file.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "PDF Export Failed",
                        "Failed to export movement logs to PDF.\n\n" + ex.getMessage());
            }
        });
    }

    /**
     * Gives the user control over whether the export should use the current
     * filtered result set or all records loaded from the database.
     */
    private void showScopeDialog(String format, java.util.function.BiConsumer<List<MovementLog>, String> onConfirm) {
        Alert scopeDialog = new Alert(Alert.AlertType.NONE);
        scopeDialog.setTitle("Export as " + format);
        scopeDialog.setHeaderText("Choose export scope");
        scopeDialog.setContentText("Select which movement log records should be included in the " + format + " file.");

        ButtonType filteredBtn = new ButtonType("Filtered Data (" + filteredData.size() + " records)");
        ButtonType allBtn      = new ButtonType("All Records ("   + masterData.size()   + " records)");
        ButtonType cancelBtn   = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        scopeDialog.getButtonTypes().setAll(filteredBtn, allBtn, cancelBtn);

        scopeDialog.showAndWait().ifPresent(choice -> {
            if (choice == filteredBtn) {
                onConfirm.accept(new ArrayList<>(filteredData), "Filtered Data");
            } else if (choice == allBtn) {
                onConfirm.accept(new ArrayList<>(masterData), "All Records");
            }
        });
    }

    private String defaultExportName(String base, String scope, String extension) {
        String scopeName = scope.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String date = LocalDate.now().toString();
        return base + "-" + scopeName + "-" + date + extension;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ------------------------------------------------------------------ */
    /*  EXCEL WRITER (.XLSX)                                                */
    /* ------------------------------------------------------------------ */

    private void writeMovementLogsWorkbook(List<MovementLog> data, String scopeLabel, File file) throws IOException {
        String workbookXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="Movement Logs" sheetId="1" r:id="rId1"/>
                    </sheets>
                </workbook>
                """;

        String workbookRels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;

        String rootRels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;

        String contentTypes = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
                """;

        String stylesXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <fonts count="3">
                        <font><sz val="11"/><name val="Calibri"/></font>
                        <font><b/><sz val="14"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                        <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                    </fonts>
                    <fills count="3">
                        <fill><patternFill patternType="none"/></fill>
                        <fill><patternFill patternType="gray125"/></fill>
                        <fill><patternFill patternType="solid"><fgColor rgb="FF800000"/><bgColor indexed="64"/></patternFill></fill>
                    </fills>
                    <borders count="2">
                        <border><left/><right/><top/><bottom/><diagonal/></border>
                        <border><left style="thin"/><right style="thin"/><top style="thin"/><bottom style="thin"/><diagonal/></border>
                    </borders>
                    <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                    <cellXfs count="4">
                        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                        <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
                        <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
                        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
                    </cellXfs>
                    <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                </styleSheet>
                """;

        String sheetXml = buildMovementLogsSheet(data, scopeLabel);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            addZipEntry(zip, "[Content_Types].xml", contentTypes);
            addZipEntry(zip, "_rels/.rels", rootRels);
            addZipEntry(zip, "xl/workbook.xml", workbookXml);
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels);
            addZipEntry(zip, "xl/styles.xml", stylesXml);
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml);
        }
    }

    private String buildMovementLogsSheet(List<MovementLog> data, String scopeLabel) {
        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sheet.append("<cols>");
        double[] widths = {16, 26, 22, 34, 16, 16, 16, 18};
        for (int i = 0; i < widths.length; i++) {
            sheet.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(widths[i]).append("\" customWidth=\"1\"/>");
        }
        sheet.append("</cols><sheetData>");

        int row = 1;
        appendRow(sheet, row++, 1, "Employee Movement Logs Report", "", "", "", "", "", "", "");
        appendRow(sheet, row++, 0, "Scope", scopeLabel, "Generated", LocalDate.now().toString(), "Records", String.valueOf(data.size()), "", "");
        row++;
        appendRow(sheet, row++, 2, "Date", "Employee", "Department", "Reason / Destination", "Time-Out", "Time-In", "Duration", "Status");

        for (MovementLog log : data) {
            appendRow(sheet, row++, 3,
                    formatDate(log),
                    safe(log.getEmployeeName()),
                    safe(log.getDepartment()),
                    reasonDestination(log),
                    formatTimeOut(log),
                    formatTimeIn(log),
                    formatDuration(log),
                    displayStatus(log));
        }

        sheet.append("</sheetData></worksheet>");
        return sheet.toString();
    }

    private void appendRow(StringBuilder sheet, int rowNumber, int style, String... values) {
        sheet.append("<row r=\"").append(rowNumber).append("\">");
        for (int i = 0; i < values.length; i++) {
            String cellRef = columnName(i + 1) + rowNumber;
            sheet.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\" s=\"").append(style).append("\">")
                    .append("<is><t>").append(xmlEscape(values[i])).append("</t></is></c>");
        }
        sheet.append("</row>");
    }

    private void addZipEntry(ZipOutputStream zip, String path, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.strip().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String columnName(int columnNumber) {
        StringBuilder name = new StringBuilder();
        while (columnNumber > 0) {
            int rem = (columnNumber - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            columnNumber = (columnNumber - 1) / 26;
        }
        return name.toString();
    }

    /* ------------------------------------------------------------------ */
    /*  PDF WRITER                                                         */
    /* ------------------------------------------------------------------ */

    private void writeMovementLogsPdf(List<MovementLog> data, String scopeLabel, File file) throws IOException {
        List<String> pages = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        final double pageWidth = 842;   // A4 landscape width in points
        final double pageHeight = 595;  // A4 landscape height in points
        final double margin = 36;
        final double rowHeight = 23;
        final double[] colW = {62, 130, 100, 228, 65, 65, 75};
        final String[] headers = {"Date", "Employee", "Department", "Reason / Destination", "Out", "In", "Status"};

        double y = pageHeight - margin;
        y = startPdfPage(content, scopeLabel, data.size(), y, pageWidth, margin);
        y = drawTableHeader(content, headers, colW, margin, y, rowHeight);

        for (MovementLog log : data) {
            if (y - rowHeight < margin + 10) {
                pages.add(content.toString());
                content = new StringBuilder();
                y = pageHeight - margin;
                y = startPdfPage(content, scopeLabel, data.size(), y, pageWidth, margin);
                y = drawTableHeader(content, headers, colW, margin, y, rowHeight);
            }

            String[] row = {
                    formatDate(log),
                    truncate(safe(log.getEmployeeName()), 24),
                    truncate(safe(log.getDepartment()), 18),
                    truncate(reasonDestination(log), 42),
                    formatTimeOut(log),
                    formatTimeIn(log),
                    displayStatus(log)
            };
            y = drawTableRow(content, row, colW, margin, y, rowHeight);
        }
        pages.add(content.toString());

        writePdfPages(file, pages, pageWidth, pageHeight);
    }

    private double startPdfPage(StringBuilder content, String scopeLabel, int recordCount,
                                double y, double pageWidth, double margin) {
        addText(content, "Employee Movement Logs Report", margin, y, 16, true);
        y -= 22;
        addText(content, "Scope: " + scopeLabel + "    Records: " + recordCount
                + "    Generated: " + LocalDate.now(), margin, y, 10, false);
        y -= 22;
        line(content, margin, y, pageWidth - margin, y);
        return y - 18;
    }

    private double drawTableHeader(StringBuilder content, String[] headers, double[] colW,
                                   double x, double y, double rowHeight) {
        rect(content, x, y - rowHeight, sum(colW), rowHeight, true);
        double curX = x;
        for (int i = 0; i < headers.length; i++) {
            line(content, curX, y, curX, y - rowHeight);
            addText(content, headers[i], curX + 4, y - 15, 8, true);
            curX += colW[i];
        }
        line(content, curX, y, curX, y - rowHeight);
        return y - rowHeight;
    }

    private double drawTableRow(StringBuilder content, String[] values, double[] colW,
                                double x, double y, double rowHeight) {
        rect(content, x, y - rowHeight, sum(colW), rowHeight, false);
        double curX = x;
        for (int i = 0; i < values.length; i++) {
            line(content, curX, y, curX, y - rowHeight);
            addText(content, values[i], curX + 4, y - 15, 7.5, false);
            curX += colW[i];
        }
        line(content, curX, y, curX, y - rowHeight);
        return y - rowHeight;
    }

    private void writePdfPages(File file, List<String> pages, double pageWidth, double pageHeight) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        int catalogObj = 1;
        int pagesObj = 2;
        int fontObj = 3;
        int firstPageObj = 4;
        int objectCount = 3 + (pages.size() * 2);

        offsets.add(pdf.size());
        writeObj(pdf, catalogObj, "<< /Type /Catalog /Pages " + pagesObj + " 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(firstPageObj + (i * 2)).append(" 0 R ");
        offsets.add(pdf.size());
        writeObj(pdf, pagesObj, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");

        offsets.add(pdf.size());
        writeObj(pdf, fontObj, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        for (int i = 0; i < pages.size(); i++) {
            int pageObj = firstPageObj + (i * 2);
            int contentObj = pageObj + 1;
            offsets.add(pdf.size());
            writeObj(pdf, pageObj, "<< /Type /Page /Parent " + pagesObj + " 0 R /MediaBox [0 0 "
                    + pageWidth + " " + pageHeight + "] /Resources << /Font << /F1 " + fontObj
                    + " 0 R >> >> /Contents " + contentObj + " 0 R >>");

            byte[] contentBytes = pages.get(i).getBytes(StandardCharsets.US_ASCII);
            offsets.add(pdf.size());
            pdf.write((contentObj + " 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n")
                    .getBytes(StandardCharsets.US_ASCII));
            pdf.write(contentBytes);
            pdf.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }

        int xrefStart = pdf.size();
        pdf.write(("xref\n0 " + (objectCount + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (int offset : offsets) {
            pdf.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.US_ASCII));
        }
        pdf.write(("trailer\n<< /Size " + (objectCount + 1) + " /Root " + catalogObj + " 0 R >>\n"
                + "startxref\n" + xrefStart + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));

        Files.write(file.toPath(), pdf.toByteArray());
    }

    private void writeObj(ByteArrayOutputStream pdf, int objectNumber, String body) throws IOException {
        pdf.write((objectNumber + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
    }

    private void addText(StringBuilder content, String text, double x, double y, double size, boolean bold) {
        content.append("BT /F1 ").append(size).append(" Tf ")
                .append(x).append(" ").append(y).append(" Td (")
                .append(pdfEscape(text)).append(") Tj ET\n");
    }

    private void line(StringBuilder content, double x1, double y1, double x2, double y2) {
        content.append("0.75 w ").append(x1).append(" ").append(y1).append(" m ")
                .append(x2).append(" ").append(y2).append(" l S\n");
    }

    private void rect(StringBuilder content, double x, double y, double w, double h, boolean filled) {
        if (filled) {
            content.append("0.50 0.00 0.00 rg ").append(x).append(" ").append(y).append(" ")
                    .append(w).append(" ").append(h).append(" re f 0 0 0 rg\n");
        }
        content.append(x).append(" ").append(y).append(" ").append(w).append(" ").append(h).append(" re S\n");
    }

    private double sum(double[] values) {
        double total = 0;
        for (double v : values) total += v;
        return total;
    }

    /* ------------------------------------------------------------------ */
    /*  FORMAT HELPERS                                                     */
    /* ------------------------------------------------------------------ */

    private String formatDate(MovementLog log) {
        return log.getTimeOut() == null ? "-" : log.getTimeOut().format(dateFmt);
    }

    private String formatTimeOut(MovementLog log) {
        return log.getTimeOut() == null ? "-" : log.getTimeOut().format(timeFmt);
    }

    private String formatTimeIn(MovementLog log) {
        return log.getTimeIn() == null ? "-" : log.getTimeIn().format(timeFmt);
    }

    private String formatDuration(MovementLog log) {
        return log.getEstimatedDuration() + " min";
    }

    private String displayStatus(MovementLog log) {
        if ("RETURNED".equals(log.getPassStatus()) && log.isLate()) return "RETURNED LATE";
        return log.getPassStatus() == null ? "-" : log.getPassStatus();
    }

    private String reasonDestination(MovementLog log) {
        String reason = safe(log.getReason());
        String dest = safe(log.getDestination());
        return dest.isBlank() || "-".equals(dest) ? reason : reason + " - " + dest;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) return "-";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String xmlEscape(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String pdfEscape(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}