package oopsystem.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import oopsystem.model.DailyReport;
import oopsystem.model.DepartmentUsage;
import oopsystem.model.MonthlyTrend;
import oopsystem.model.ReportSummary;
import oopsystem.repository.ReportsAnalyticsRepository;

import java.sql.SQLException;
import java.time.LocalDate;

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

    @FXML private TableView<DailyReport> dailyReportTable;
    @FXML private TableColumn<DailyReport, LocalDate> reportDateColumn;
    @FXML private TableColumn<DailyReport, Integer> totalIssuedColumn;
    @FXML private TableColumn<DailyReport, Integer> returnedOnTimeColumn;
    @FXML private TableColumn<DailyReport, Integer> overdueColumn;
    @FXML private TableColumn<DailyReport, Double> averageDurationColumn;
    @FXML private TableColumn<DailyReport, Double> dailyComplianceColumn;

    @FXML private TableView<DepartmentUsage> departmentUsageTable;
    @FXML private TableColumn<DepartmentUsage, String> departmentColumn;
    @FXML private TableColumn<DepartmentUsage, Integer> departmentTotalColumn;
    @FXML private TableColumn<DepartmentUsage, Double> departmentPercentageColumn;

    @FXML private BarChart<String, Number> monthlyTrendChart;

    // Repository object used by the controller to get reports from the database.
    private final ReportsAnalyticsRepository repository = new ReportsAnalyticsRepository();

    /**
     * initialize() automatically runs after the FXML file is loaded.
     * This is where the screen is prepared before the user interacts with it.
     */
    @FXML
    private void initialize() {
        setupDatePickers();
        setupDailyReportTable();
        setupDepartmentUsageTable();
        loadReports();
    }

    /**
     * Runs when the user clicks the Apply Filters button.
     */
    @FXML
    private void handleApplyFilters() {
        loadReports();
    }

    /**
     * Runs when the user clicks the Refresh button.
     */
    @FXML
    private void handleRefresh() {
        loadReports();
    }

    /**
     * Sets a default date range so the screen shows recent data immediately.
     */
    private void setupDatePickers() {
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));
    }

    /**
     * Connects DailyReport model properties to the daily report table columns.
     */
    private void setupDailyReportTable() {
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
    }

    /**
     * Connects DepartmentUsage model properties to the department usage table columns.
     */
    private void setupDepartmentUsageTable() {
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
    private void loadReports() {
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
            // Get the summary card values from the repository.
            ReportSummary summary = repository.getSummary(startDate, endDate);
            totalPassSlipsLabel.setText(String.valueOf(summary.getTotalPassSlips()));
            complianceRateLabel.setText(String.format("%.1f%%", summary.getComplianceRate()));
            averageDurationLabel.setText(formatDuration(summary.getAverageDurationMinutes()));
            overduePassesLabel.setText(String.valueOf(summary.getOverduePasses()));
            currentlyOutLabel.setText(String.valueOf(summary.getCurrentlyOut()));

            // Load table data from the database and convert it into observable lists for JavaFX.
            dailyReportTable.setItems(FXCollections.observableArrayList(repository.getDailyReports(startDate, endDate)));
            departmentUsageTable.setItems(FXCollections.observableArrayList(repository.getDepartmentUsage(startDate, endDate)));

            // Load chart data separately to keep the method organized.
            loadMonthlyTrendChart(startDate, endDate);

            statusLabel.setText("Reports loaded successfully.");
        } catch (SQLException e) {
            statusLabel.setText("Failed to load reports.");
            showError("Database error", e.getMessage());
        }
    }

    /**
     * Loads the monthly bar chart using data from the repository.
     */
    private void loadMonthlyTrendChart(LocalDate startDate, LocalDate endDate) throws SQLException {
        monthlyTrendChart.getData().clear();

        XYChart.Series<String, Number> issuedSeries = new XYChart.Series<>();
        issuedSeries.setName("Issued");

        XYChart.Series<String, Number> returnedSeries = new XYChart.Series<>();
        returnedSeries.setName("Returned");

        XYChart.Series<String, Number> overdueSeries = new XYChart.Series<>();
        overdueSeries.setName("Overdue");

        // Convert each MonthlyTrend object into chart data points.
        for (MonthlyTrend trend : repository.getMonthlyTrends(startDate, endDate)) {
            issuedSeries.getData().add(new XYChart.Data<>(trend.getPeriod(), trend.getTotalIssued()));
            returnedSeries.getData().add(new XYChart.Data<>(trend.getPeriod(), trend.getReturned()));
            overdueSeries.getData().add(new XYChart.Data<>(trend.getPeriod(), trend.getOverdue()));
        }

        monthlyTrendChart.getData().addAll(issuedSeries, returnedSeries, overdueSeries);
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
     * Shows an error dialog when validation or database loading fails.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
