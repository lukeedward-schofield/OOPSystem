package oopsystem.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import oopsystem.model.DailyReport;
import oopsystem.model.DepartmentUsage;
import oopsystem.model.MonthlyTrend;
import oopsystem.model.ReportSummary;
import oopsystem.repository.ReportsAnalyticsRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX controller for the Reports and Analytics screen.
 *
 * Main responsibility:
 * 1. Receive user actions from the FXML screen.
 * 2. Ask the ReportsAnalyticsRepository for report data.
 * 3. Display that data in labels, tables, and charts.
 *
 * The controller should not contain SQL queries. SQL belongs in the repository.
 */
public class ReportsAnalyticsController {
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label totalPassSlipsLabel;
    @FXML private Label complianceRateLabel;
    @FXML private Label averageDurationLabel;
    @FXML private Label overduePassesLabel;
    @FXML private Label currentlyOutLabel;
    @FXML private Label statusLabel;

    // These labels used to contain hardcoded text in the FXML.
    // The controller now updates them using database-based comparison values.
    @FXML private Label totalPassSlipsChangeLabel;
    @FXML private Label complianceRateChangeLabel;
    @FXML private Label averageDurationChangeLabel;
    @FXML private Label overduePassesChangeLabel;

    @FXML private TableView<DailyReport> dailyReportTable;
    @FXML private TableColumn<DailyReport, LocalDate> reportDateColumn;
    @FXML private TableColumn<DailyReport, Integer> totalIssuedColumn;
    @FXML private TableColumn<DailyReport, Integer> returnedOnTimeColumn;
    @FXML private TableColumn<DailyReport, Integer> overdueColumn;
    @FXML private TableColumn<DailyReport, Double> averageDurationColumn;
    @FXML private TableColumn<DailyReport, Double> dailyComplianceColumn;
    @FXML private TableColumn<DailyReport, DailyReport> actionColumn;

    @FXML private TableView<DepartmentUsage> departmentUsageTable;
    @FXML private TableColumn<DepartmentUsage, String> departmentColumn;
    @FXML private TableColumn<DepartmentUsage, Integer> departmentTotalColumn;
    @FXML private TableColumn<DepartmentUsage, Double> departmentPercentageColumn;

    @FXML private BarChart<String, Number> monthlyTrendChart;

    // Buttons/links that were previously visual only. These are now connected to controller actions.
    @FXML private Button dailyTabButton;
    @FXML private Button weeklyTabButton;
    @FXML private Button exportExcelButton;
    @FXML private Button exportPdfButton;
    @FXML private Button generateReportButton;
    @FXML private Label viewAllDepartmentsLabel;

    // Repository object used by the controller to get reports from the database.
    private final ReportsAnalyticsRepository repository = new ReportsAnalyticsRepository();

    // Cached screen data. Exports use these values so the exported file matches what the user sees.
    private ReportSummary currentSummary;
    private List<DailyReport> currentReportRows = new ArrayList<>();
    private List<DailyReport> cachedDailyRows = new ArrayList<>();
    private List<DailyReport> cachedWeeklyRows = new ArrayList<>();
    private List<DepartmentUsage> currentDepartments = new ArrayList<>();

    // false = Daily table mode, true = Weekly table mode.
    private boolean weeklyMode = false;

    /**
     * initialize() automatically runs after the FXML file is loaded.
     * This is where the screen is prepared before the user interacts with it.
     */
    @FXML
    private void initialize() {
        setupDatePickers();
        setupDailyReportTable();
        setupDepartmentUsageTable();
        setupResponsiveLayout();
        updateTabStyles();
        loadReports(false);
    }

    /**
     * Runs when the user clicks the Date Range button.
     */
    @FXML
    private void handleApplyFilters() {
        loadReports(true);
    }

    /**
     * Runs when the user clicks the Refresh button, if the FXML uses it later.
     */
    @FXML
    private void handleRefresh() {
        loadReports(true);
    }

    /**
     * Shows the compliance table by day.
     */
    @FXML
    private void handleShowDaily() {
        weeklyMode = false;
        updateTabStyles();
        displayCachedComplianceTable();
        setStatus(currentReportRows.isEmpty()
                ? "Daily view loaded. No records found for the selected date range."
                : "Daily view loaded successfully.", false);
    }

    /**
     * Shows the compliance table by week.
     */
    @FXML
    private void handleShowWeekly() {
        weeklyMode = true;
        updateTabStyles();
        displayCachedComplianceTable();
        setStatus(currentReportRows.isEmpty()
                ? "Weekly view loaded. No records found for the selected date range."
                : "Weekly view loaded successfully.", false);
    }

    /**
     * Exports the current report screen as a CSV file.
     *
     * CSV is used because it opens directly in Microsoft Excel without adding
     * extra project dependencies like Apache POI.
     */
    @FXML
    private void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Reports and Analytics as Excel CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName(defaultExportName("reports-analytics", "csv"));

        File file = fileChooser.showSaveDialog(exportExcelButton.getScene().getWindow());
        if (file == null) {
            setStatus("Excel export cancelled.", false);
            return;
        }

        try {
            Files.writeString(file.toPath(), buildCsvExport(), StandardCharsets.UTF_8);
            showInfo("Export successful", "Excel-compatible CSV file was exported successfully.");
        } catch (IOException e) {
            showError("Export failed", e.getMessage());
        }
    }

    /**
     * Exports the current report screen as a simple PDF file.
     *
     * The PDF writer below is intentionally dependency-free so the group does not
     * need to edit pom.xml or install extra libraries just for this export.
     */
    @FXML
    private void handleExportPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Reports and Analytics as PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName(defaultExportName("reports-analytics", "pdf"));

        File file = fileChooser.showSaveDialog(exportPdfButton.getScene().getWindow());
        if (file == null) {
            setStatus("PDF export cancelled.", false);
            return;
        }

        try {
            writeSimplePdf(file, buildPdfLines());
            showInfo("Export successful", "PDF file was exported successfully.");
        } catch (IOException e) {
            showError("Export failed", e.getMessage());
        }
    }

    /**
     * Generates a downloadable report for the currently selected compliance view.
     *
     * If Daily is active, it generates a daily compliance report.
     * If Weekly is active, it generates a weekly compliance report.
     */
    @FXML
    private void handleGenerateReport() {
        if (currentReportRows == null || currentReportRows.isEmpty()) {
            showInfo(
                    "No report data",
                    "No " + selectedReportModeText().toLowerCase() + " report data is available for the selected date range."
            );
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Generate " + selectedReportModeText() + " Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName(defaultExportName(selectedReportModeText().toLowerCase() + "-compliance-report", "pdf"));

        File file = fileChooser.showSaveDialog(generateReportButton.getScene().getWindow());
        if (file == null) {
            setStatus(selectedReportModeText() + " report generation cancelled.", false);
            return;
        }

        try {
            writeSimplePdf(file, buildComplianceReportLines());
            showInfo(
                    "Report generated",
                    selectedReportModeText() + " report was generated and downloaded successfully."
            );
        } catch (IOException e) {
            showError("Report generation failed", e.getMessage());
        }
    }

    /**
     * Shows a simple pop-up containing all department usage rows from the current date range.
     */
    @FXML
    private void handleViewAllDepartments() {
        if (currentDepartments == null || currentDepartments.isEmpty()) {
            showInfo("No department data", "No department usage records were found for the selected date range.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Departmental Usage");
        dialog.setHeaderText("All departments in the selected date range");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<DepartmentUsage> table = new TableView<>();
        table.setPrefSize(520, 360);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(currentDepartments));

        TableColumn<DepartmentUsage, String> department = new TableColumn<>("Department");
        department.setPrefWidth(250);
        department.setCellValueFactory(new PropertyValueFactory<>("department"));

        TableColumn<DepartmentUsage, Integer> slips = new TableColumn<>("Slips");
        slips.setPrefWidth(100);
        slips.setCellValueFactory(new PropertyValueFactory<>("totalSlips"));

        TableColumn<DepartmentUsage, Double> usage = new TableColumn<>("Usage");
        usage.setPrefWidth(120);
        usage.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        usage.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double percentage, boolean empty) {
                super.updateItem(percentage, empty);
                setText(empty || percentage == null ? null : String.format("%.1f%%", percentage));
            }
        });

        table.getColumns().addAll(department, slips, usage);
        dialog.getDialogPane().setContent(table);
        setStatus("Department usage details opened successfully.", false);
        dialog.showAndWait();
    }

    /**
     * Sets the initial date range.
     *
     * Instead of always using the current date, this tries to use the actual
     * minimum and maximum pass slip dates in the database. This makes the report
     * screen immediately show existing records after the group database is seeded.
     */
    private void setupDatePickers() {
        try {
            LocalDate[] range = repository.getAvailableDateRange();
            startDatePicker.setValue(range[0]);
            endDatePicker.setValue(range[1]);
        } catch (SQLException e) {
            // Fallback if the database is temporarily unavailable during startup.
            endDatePicker.setValue(LocalDate.now());
            startDatePicker.setValue(LocalDate.now().minusDays(30));
            setStatus("Using default date range because database dates could not be loaded.", true);
        }
    }

    /**
     * Connects DailyReport model properties to the daily/weekly report table columns.
     */
    private void setupDailyReportTable() {
        dailyReportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportDateColumn.setCellValueFactory(new PropertyValueFactory<>("reportDate"));
        totalIssuedColumn.setCellValueFactory(new PropertyValueFactory<>("totalIssued"));
        returnedOnTimeColumn.setCellValueFactory(new PropertyValueFactory<>("returnedOnTime"));
        overdueColumn.setCellValueFactory(new PropertyValueFactory<>("overdue"));
        averageDurationColumn.setCellValueFactory(new PropertyValueFactory<>("averageDurationMinutes"));
        dailyComplianceColumn.setCellValueFactory(new PropertyValueFactory<>("complianceRate"));

        // Display average duration as a readable value such as "1h 30m" instead of raw minutes.
        averageDurationColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double minutes, boolean empty) {
                super.updateItem(minutes, empty);
                setText(empty || minutes == null ? null : formatDuration(minutes));
            }
        });

        // Display compliance as a percentage such as "94.2%".
        dailyComplianceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double rate, boolean empty) {
                super.updateItem(rate, empty);
                setText(empty || rate == null ? null : String.format("%.1f%%", rate));
            }
        });

        // The submitted UI has a Details text in the last column.
        // Each non-empty report row now receives its own Details button.
        // Using the row object as the cell value is more reliable than checking the visible row index,
        // because JavaFX reuses table cells when the table is refreshed, resized, or scrolled.
        if (actionColumn != null) {
            actionColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
            actionColumn.setCellFactory(column -> new TableCell<>() {
                private final Button detailsButton = new Button("Details");

                {
                    detailsButton.getStyleClass().add("details-link-button");
                }

                @Override
                protected void updateItem(DailyReport report, boolean empty) {
                    super.updateItem(report, empty);

                    if (empty || report == null) {
                        setGraphic(null);
                    } else {
                        detailsButton.setOnAction(event -> showComplianceDetails(report));
                        setGraphic(detailsButton);
                    }

                    setText(null);
                }
            });
        }
    }

    /**
     * Connects DepartmentUsage model properties to the department usage table columns.
     */
    private void setupDepartmentUsageTable() {
        departmentUsageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        departmentTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalSlips"));
        departmentPercentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));

        // Display the department usage share as a percentage.
        departmentPercentageColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double percentage, boolean empty) {
                super.updateItem(percentage, empty);
                setText(empty || percentage == null ? null : String.format("%.1f%%", percentage));
            }
        });
    }

    /**
     * Main method for loading all reports on the screen.
     *
     * This validates the selected date range, calls the repository methods,
     * and updates all UI components.
     */
    private void loadReports(boolean showSuccessDialog) {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showError("Date range required", "Please select both start date and end date.");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError("Invalid date range", "Start date cannot be later than end date.");
            return;
        }

        try {
            // Keep overdue statuses synchronized before showing report values.
            repository.syncOverdueStatuses();

            // Get the summary card values from the repository.
            currentSummary = repository.getSummary(startDate, endDate);
            totalPassSlipsLabel.setText(String.valueOf(currentSummary.getTotalPassSlips()));
            complianceRateLabel.setText(String.format("%.1f%%", currentSummary.getComplianceRate()));
            averageDurationLabel.setText(formatDuration(currentSummary.getAverageDurationMinutes()));
            overduePassesLabel.setText(String.valueOf(currentSummary.getOverduePasses()));
            currentlyOutLabel.setText(String.valueOf(currentSummary.getCurrentlyOut()));

            updateSummaryChangeLabels(currentSummary);

            // Load table data from the database and convert it into observable lists for JavaFX.
            currentDepartments = repository.getDepartmentUsage(startDate, endDate);
            departmentUsageTable.setItems(FXCollections.observableArrayList(currentDepartments));

            // Cache both daily and weekly rows now. This makes the Daily/Weekly buttons switch instantly
            // instead of running another database query every time the user clicks a tab.
            cachedDailyRows = repository.getDailyReports(startDate, endDate);
            cachedWeeklyRows = repository.getWeeklyReports(startDate, endDate);
            displayCachedComplianceTable();

            // Load chart data separately to keep the method organized.
            loadMonthlyTrendChart(startDate, endDate);

            setStatus("Reports loaded successfully. " + currentSummary.getTotalPassSlips() + " pass slip record(s) found.", false);
            if (showSuccessDialog) {
                showInfo("Reports loaded", "Reports were refreshed successfully for the selected date range.");
            }
        } catch (SQLException e) {
            setStatus("Failed to load reports.", true);
            showError("Database error", e.getMessage());
        }
    }

    /**
     * Displays cached daily or weekly rows in the compliance table.
     *
     * This avoids slow tab switching because the database is not queried again
     * when the user only switches between Daily and Weekly view.
     */
    private void displayCachedComplianceTable() {
        if (weeklyMode) {
            reportDateColumn.setText("WEEK START");
            currentReportRows = cachedWeeklyRows;
        } else {
            reportDateColumn.setText("DATE");
            currentReportRows = cachedDailyRows;
        }

        dailyReportTable.setItems(FXCollections.observableArrayList(currentReportRows));
    }

    /**
     * Opens a pop-up showing details for the selected compliance row.
     *
     * The row is already an aggregated report, so the pop-up summarizes the
     * selected day or week instead of showing one individual pass slip.
     */
    private void showComplianceDetails(DailyReport report) {
        if (report == null) {
            return;
        }

        String periodLabel = weeklyMode ? "Week Start" : "Date";
        setStatus("Compliance details opened successfully.", false);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Compliance Details");
        alert.setHeaderText((weeklyMode ? "Weekly" : "Daily") + " Compliance Summary");
        alert.setContentText(
                periodLabel + ": " + report.getReportDate() + "\n\n" +
                "Total Issued: " + report.getTotalIssued() + "\n" +
                "Returned On Time: " + report.getReturnedOnTime() + "\n" +
                "Overdue: " + report.getOverdue() + "\n" +
                "Average Duration: " + formatDuration(report.getAverageDurationMinutes()) + "\n" +
                "Compliance Rate: " + String.format("%.1f%%", report.getComplianceRate())
        );
        alert.showAndWait();
    }

    /**
     * Updates the small comparison labels beside each summary card.
     */
    private void updateSummaryChangeLabels(ReportSummary summary) {
        setChangeLabel(
                totalPassSlipsChangeLabel,
                formatSignedPercent(summary.getTotalPassSlipsChangePercent()),
                summary.getTotalPassSlipsChangePercent() >= 0
        );

        setChangeLabel(
                complianceRateChangeLabel,
                formatSignedPercent(summary.getComplianceRateChange()),
                summary.getComplianceRateChange() >= 0
        );

        // For average duration, a decrease is treated as good because employees
        // spent less time outside compared with the previous period.
        setChangeLabel(
                averageDurationChangeLabel,
                formatSignedDuration(summary.getAverageDurationChangeMinutes()),
                summary.getAverageDurationChangeMinutes() <= 0
        );

        // For overdue passes, an increase is bad; decrease or no change is good.
        setChangeLabel(
                overduePassesChangeLabel,
                formatSignedWholeNumber(summary.getOverduePassesChange()),
                summary.getOverduePassesChange() <= 0
        );
    }

    /**
     * Loads the monthly bar chart using Official and Personal data from the repository.
     */
    private void loadMonthlyTrendChart(LocalDate startDate, LocalDate endDate) throws SQLException {
        monthlyTrendChart.getData().clear();

        XYChart.Series<String, Number> officialSeries = new XYChart.Series<>();
        officialSeries.setName("Official");

        XYChart.Series<String, Number> personalSeries = new XYChart.Series<>();
        personalSeries.setName("Personal");

        // Convert each MonthlyTrend object into chart data points.
        for (MonthlyTrend trend : repository.getMonthlyTrends(startDate, endDate)) {
            officialSeries.getData().add(new XYChart.Data<>(trend.getPeriod(), trend.getOfficialCount()));
            personalSeries.getData().add(new XYChart.Data<>(trend.getPeriod(), trend.getPersonalCount()));
        }

        monthlyTrendChart.getData().addAll(officialSeries, personalSeries);
    }

    /**
     * Updates which Daily/Weekly tab appears active.
     */
    private void updateTabStyles() {
        if (dailyTabButton == null || weeklyTabButton == null) {
            return;
        }

        dailyTabButton.getStyleClass().removeAll("tab-button", "tab-button-active");
        weeklyTabButton.getStyleClass().removeAll("tab-button", "tab-button-active");

        dailyTabButton.getStyleClass().add(weeklyMode ? "tab-button" : "tab-button-active");
        weeklyTabButton.getStyleClass().add(weeklyMode ? "tab-button-active" : "tab-button");
    }

    /**
     * Adjusts chart and table heights when the app is opened on different screen sizes.
     * This prevents the Reports screen from looking compressed on smaller displays
     * while still using extra space on larger monitors.
     */
    private void setupResponsiveLayout() {
        monthlyTrendChart.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }

            updateResponsiveSizes(newScene.getWidth(), newScene.getHeight());
            newScene.widthProperty().addListener((obs, oldWidth, newWidth) ->
                    updateResponsiveSizes(newWidth.doubleValue(), newScene.getHeight()));
            newScene.heightProperty().addListener((obs, oldHeight, newHeight) ->
                    updateResponsiveSizes(newScene.getWidth(), newHeight.doubleValue()));
        });
    }

    private void updateResponsiveSizes(double width, double height) {
        double usableHeight = Math.max(520, height - 130);
        double chartHeight = clamp(usableHeight * 0.34, 170, 290);
        double complianceHeight = clamp(usableHeight * 0.23, 125, 220);
        double departmentHeight = clamp(chartHeight - 38, 150, 250);

        monthlyTrendChart.setPrefHeight(chartHeight);
        dailyReportTable.setPrefHeight(complianceHeight);
        departmentUsageTable.setPrefHeight(departmentHeight);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Builds the CSV content used by the Excel export button.
     */
    private String buildCsvExport() {
        StringBuilder csv = new StringBuilder();
        csv.append("Reports and Analytics\n");
        csv.append("Date Range,").append(csvValue(startDatePicker.getValue())).append(",to,").append(csvValue(endDatePicker.getValue())).append("\n\n");

        csv.append("Summary\n");
        csv.append("Metric,Value\n");
        csv.append("Total Pass Slips,").append(currentSummary == null ? 0 : currentSummary.getTotalPassSlips()).append("\n");
        csv.append("Compliance Rate,").append(currentSummary == null ? "0.0%" : String.format("%.1f%%", currentSummary.getComplianceRate())).append("\n");
        csv.append("Average Duration,").append(csvValue(currentSummary == null ? "0m" : formatDuration(currentSummary.getAverageDurationMinutes()))).append("\n");
        csv.append("Overdue Passes,").append(currentSummary == null ? 0 : currentSummary.getOverduePasses()).append("\n\n");

        csv.append("Departmental Usage\n");
        csv.append("Department,Slips,Usage\n");
        for (DepartmentUsage department : currentDepartments) {
            csv.append(csvValue(department.getDepartment())).append(',')
                    .append(department.getTotalSlips()).append(',')
                    .append(String.format("%.1f%%", department.getPercentage())).append('\n');
        }

        csv.append("\n").append(weeklyMode ? "Weekly" : "Daily").append(" Compliance and Overdue Monitoring\n");
        csv.append(weeklyMode ? "Week Start" : "Date").append(",Total Issued,Returned On Time,Overdue,Average Duration,Compliance Rate\n");
        for (DailyReport report : currentReportRows) {
            csv.append(csvValue(report.getReportDate())).append(',')
                    .append(report.getTotalIssued()).append(',')
                    .append(report.getReturnedOnTime()).append(',')
                    .append(report.getOverdue()).append(',')
                    .append(csvValue(formatDuration(report.getAverageDurationMinutes()))).append(',')
                    .append(String.format("%.1f%%", report.getComplianceRate())).append('\n');
        }

        return csv.toString();
    }

    /**
     * Builds text lines used by the dependency-free PDF export.
     */
    private List<String> buildPdfLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Reports and Analytics");
        lines.add("Date Range: " + startDatePicker.getValue() + " to " + endDatePicker.getValue());
        lines.add("");
        lines.add("Summary");
        lines.add("Total Pass Slips: " + (currentSummary == null ? 0 : currentSummary.getTotalPassSlips()));
        lines.add("Compliance Rate: " + (currentSummary == null ? "0.0%" : String.format("%.1f%%", currentSummary.getComplianceRate())));
        lines.add("Average Duration: " + (currentSummary == null ? "0m" : formatDuration(currentSummary.getAverageDurationMinutes())));
        lines.add("Overdue Passes: " + (currentSummary == null ? 0 : currentSummary.getOverduePasses()));
        lines.add("");
        lines.add("Departmental Usage");
        lines.add("Department | Slips | Usage");
        for (DepartmentUsage department : currentDepartments) {
            lines.add(department.getDepartment() + " | " + department.getTotalSlips() + " | " + String.format("%.1f%%", department.getPercentage()));
        }
        lines.add("");
        lines.add((weeklyMode ? "Weekly" : "Daily") + " Compliance and Overdue Monitoring");
        lines.add((weeklyMode ? "Week Start" : "Date") + " | Total Issued | Returned On Time | Overdue | Avg Duration | Compliance");
        for (DailyReport report : currentReportRows) {
            lines.add(report.getReportDate()
                    + " | " + report.getTotalIssued()
                    + " | " + report.getReturnedOnTime()
                    + " | " + report.getOverdue()
                    + " | " + formatDuration(report.getAverageDurationMinutes())
                    + " | " + String.format("%.1f%%", report.getComplianceRate()));
        }
        return lines;
    }

    /**
     * Builds the downloadable Daily/Weekly compliance report selected by the user.
     */
    private List<String> buildComplianceReportLines() {
        List<String> lines = new ArrayList<>();
        String reportMode = selectedReportModeText();

        lines.add(reportMode + " Compliance and Overdue Report");
        lines.add("Date Range: " + startDatePicker.getValue() + " to " + endDatePicker.getValue());
        lines.add("Generated From: Reports & Analytics Module");
        lines.add("");
        lines.add("Summary");
        lines.add("Total Pass Slips: " + (currentSummary == null ? 0 : currentSummary.getTotalPassSlips()));
        lines.add("Compliance Rate: " + (currentSummary == null ? "0.0%" : String.format("%.1f%%", currentSummary.getComplianceRate())));
        lines.add("Average Duration: " + (currentSummary == null ? "0m" : formatDuration(currentSummary.getAverageDurationMinutes())));
        lines.add("Overdue Passes: " + (currentSummary == null ? 0 : currentSummary.getOverduePasses()));
        lines.add("");
        lines.add(reportMode + " Records");
        lines.add((weeklyMode ? "Week Start" : "Date") + " | Total Issued | Returned On Time | Overdue | Avg Duration | Compliance");

        for (DailyReport report : currentReportRows) {
            lines.add(report.getReportDate()
                    + " | " + report.getTotalIssued()
                    + " | " + report.getReturnedOnTime()
                    + " | " + report.getOverdue()
                    + " | " + formatDuration(report.getAverageDurationMinutes())
                    + " | " + String.format("%.1f%%", report.getComplianceRate()));
        }

        return lines;
    }

    /**
     * Creates a small valid PDF file using only standard Java classes.
     */
    private void writeSimplePdf(File file, List<String> rawLines) throws IOException {
        List<List<String>> pages = paginatePdfLines(rawLines);
        List<byte[]> objects = new ArrayList<>();

        objects.add(pdfObject("<< /Type /Catalog /Pages 2 0 R >>"));

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            kids.append(4 + (i * 2)).append(" 0 R ");
        }
        objects.add(pdfObject("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>"));
        objects.add(pdfObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

        for (int i = 0; i < pages.size(); i++) {
            int pageObjectNumber = 4 + (i * 2);
            int contentObjectNumber = pageObjectNumber + 1;

            objects.add(pdfObject("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Resources << /Font << /F1 3 0 R >> >> "
                    + "/Contents " + contentObjectNumber + " 0 R >>"));

            byte[] content = buildPdfPageContent(pages.get(i)).getBytes(StandardCharsets.US_ASCII);
            String streamHeader = "<< /Length " + content.length + " >>\nstream\n";
            String streamFooter = "\nendstream";

            ByteArrayOutputStream streamObject = new ByteArrayOutputStream();
            streamObject.write(streamHeader.getBytes(StandardCharsets.US_ASCII));
            streamObject.write(content);
            streamObject.write(streamFooter.getBytes(StandardCharsets.US_ASCII));
            objects.add(pdfObject(streamObject.toString(StandardCharsets.US_ASCII)));
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.size());
            pdf.write((i + 1 + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            pdf.write(objects.get(i));
            pdf.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }

        int xrefStart = pdf.size();
        pdf.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i < offsets.size(); i++) {
            pdf.write(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        }
        pdf.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF")
                .getBytes(StandardCharsets.US_ASCII));

        Files.write(file.toPath(), pdf.toByteArray());
    }

    private List<List<String>> paginatePdfLines(List<String> rawLines) {
        List<String> wrapped = new ArrayList<>();
        for (String line : rawLines) {
            wrapped.addAll(wrapLine(line, 92));
        }

        List<List<String>> pages = new ArrayList<>();
        int linesPerPage = 42;
        for (int i = 0; i < wrapped.size(); i += linesPerPage) {
            pages.add(new ArrayList<>(wrapped.subList(i, Math.min(i + linesPerPage, wrapped.size()))));
        }
        if (pages.isEmpty()) {
            pages.add(List.of("Reports and Analytics"));
        }
        return pages;
    }

    private List<String> wrapLine(String line, int maxLength) {
        List<String> result = new ArrayList<>();
        String remaining = line == null ? "" : line;
        while (remaining.length() > maxLength) {
            result.add(remaining.substring(0, maxLength));
            remaining = remaining.substring(maxLength);
        }
        result.add(remaining);
        return result;
    }

    private String buildPdfPageContent(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 11 Tf\n50 760 Td\n");
        for (String line : lines) {
            content.append('(').append(escapePdfText(line)).append(") Tj\n0 -16 Td\n");
        }
        content.append("ET");
        return content.toString();
    }

    private byte[] pdfObject(String content) {
        return content.getBytes(StandardCharsets.US_ASCII);
    }

    private String escapePdfText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("↗", "up")
                .replace("↘", "down")
                .replace("△", "up")
                .replace("▽", "down");
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private String selectedReportModeText() {
        return weeklyMode ? "Weekly" : "Daily";
    }

    private String defaultExportName(String baseName, String extension) {
        return baseName + "-" + startDatePicker.getValue() + "-to-" + endDatePicker.getValue() + "." + extension;
    }

    /**
     * Converts raw minutes into a readable format.
     *
     * Example:
     * 90 minutes becomes "1h 30m".
     */
    private String formatDuration(double minutes) {
        int totalMinutes = (int) Math.round(minutes);
        int hours = totalMinutes / 60;
        int remainingMinutes = totalMinutes % 60;

        if (hours <= 0) {
            return remainingMinutes + "m";
        }

        return hours + "h " + remainingMinutes + "m";
    }

    /**
     * Formats percent changes with a sign and arrow.
     */
    private String formatSignedPercent(double value) {
        String arrow = value >= 0 ? "↗" : "↘";
        return String.format("%+.1f%%%s", value, arrow);
    }

    /**
     * Formats duration changes using hours when possible.
     */
    private String formatSignedDuration(double minutes) {
        double hours = minutes / 60.0;

        if (Math.abs(hours) >= 1) {
            String arrow = hours >= 0 ? "↗" : "↘";
            return String.format("%+.1fh%s", hours, arrow);
        }

        String arrow = minutes >= 0 ? "↗" : "↘";
        return String.format("%+.0fm%s", minutes, arrow);
    }

    /**
     * Formats whole-number count changes.
     */
    private String formatSignedWholeNumber(int value) {
        String arrow = value >= 0 ? "△" : "▽";
        return String.format("%+d%s", value, arrow);
    }

    /**
     * Applies positive/negative CSS to a comparison label.
     */
    private void setChangeLabel(Label label, String text, boolean positive) {
        if (label == null) {
            return;
        }

        label.setText(text);
        label.getStyleClass().removeAll("positive-change", "negative-change");
        label.getStyleClass().add(positive ? "positive-change" : "negative-change");
    }

    /**
     * Updates the visible status/assurance label on the Reports page.
     */
    private void setStatus(String message, boolean error) {
        if (statusLabel == null) {
            return;
        }

        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-label", "status-label-error");
        statusLabel.getStyleClass().add(error ? "status-label-error" : "status-label");
    }

    /**
     * Shows a success/information dialog.
     */
    private void showInfo(String title, String message) {
        setStatus(message, false);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error dialog when validation, database loading, or exporting fails.
     */
    private void showError(String title, String message) {
        setStatus(message == null || message.isBlank() ? title : message, true);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
