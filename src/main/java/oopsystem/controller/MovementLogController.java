package oopsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import oopsystem.model.MovementLog;
import oopsystem.repository.MovementLogRepository;
import oopsystem.util.SceneNavigator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class MovementLogController {

    /* NAVIGATION */
    @FXML public void goToDashboard() { SceneNavigator.switchTo("DashboardView"); }
    @FXML public void goToPassSlipIssuance() { SceneNavigator.switchTo("PassSlipIssuanceView"); }
    @FXML public void goToMovementLogs() { SceneNavigator.switchTo("MovementlogView"); }
    @FXML public void goToEmployeeDirectory() { SceneNavigator.switchTo("EmployeeDirectoryView"); }
    @FXML public void gotoReports() { SceneNavigator.switchTo("ReportsView"); }

    /* FXML */
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private TextField employeeNameFilter;

    @FXML private Label totalMovementsLabel;
    @FXML private Label currentlyOutLabel;
    @FXML private Label complianceRateLabel;

    @FXML private TableView<MovementLog> movementLogsTable;

    @FXML private TableColumn<MovementLog, String> dateColumn;
    @FXML private TableColumn<MovementLog, String> employeeColumn;
    @FXML private TableColumn<MovementLog, String> reasonColumn;
    @FXML private TableColumn<MovementLog, String> timeOutColumn;
    @FXML private TableColumn<MovementLog, String> timeInColumn;
    @FXML private TableColumn<MovementLog, String> durationColumn;
    @FXML private TableColumn<MovementLog, String> statusColumn;

    /* DATA */
    private final ObservableList<MovementLog> masterData = FXCollections.observableArrayList();
    private final ObservableList<MovementLog> filteredData = FXCollections.observableArrayList();
    private final MovementLogRepository repository = new MovementLogRepository();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private int currentPage = 0;
    private final int pageSize = 10;


    @FXML
    public void initialize() {

        setupTable();
        movementLogsTable.setItems(filteredData);

        loadMovementLogs();

        setupAutoFiltering(); // 🔥 IMPORTANT FIX
    }


    private void setupAutoFiltering() {

        departmentFilter.setOnAction(e -> handleApplyFilters());
        startDatePicker.setOnAction(e -> handleApplyFilters());
        endDatePicker.setOnAction(e -> handleApplyFilters());

        employeeNameFilter.textProperty().addListener((obs, oldVal, newVal) -> {
            handleApplyFilters();
        });
    }


    private void setupTable() {

        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getTimeOut() == null
                                ? "-"
                                : data.getValue().getTimeOut().format(dateFormatter)
                )
        );

        employeeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmployeeName())
        );

        reasonColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getReason() + " - " + data.getValue().getDestination()
                )
        );

        timeOutColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getTimeOut() == null
                                ? "-"
                                : data.getValue().getTimeOut().format(timeFormatter)
                )
        );

        timeInColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getTimeIn() == null
                                ? "-"
                                : data.getValue().getTimeIn().format(timeFormatter)
                )
        );

        durationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDuration() + " min")
        );

        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isStatus() ? "Returned" : "Out")
        );
    }


    private void loadMovementLogs() {

        masterData.setAll(repository.getAllMovementLogs());

        filteredData.setAll(masterData);

        loadDepartments();
        updateStatistics();
    }


    private void loadDepartments() {

        departmentFilter.getItems().clear();
        departmentFilter.getItems().add("All Departments");

        masterData.stream()
                .map(MovementLog::getDepartment)
                .distinct()
                .sorted()
                .forEach(departmentFilter.getItems()::add);

        departmentFilter.setValue("All Departments");
    }


    @FXML
    private void handleApplyFilters() {

        filteredData.setAll(
                masterData.stream()
                        .filter(this::matchesDateFilter)
                        .filter(this::matchesDepartmentFilter)
                        .filter(this::matchesEmployeeFilter)
                        .collect(Collectors.toList())
        );

        updateStatistics();
    }


    private boolean matchesDateFilter(MovementLog log) {

        if (log.getCreatedAt() == null) return false;

        LocalDate logDate = log.getCreatedAt().toLocalDate();

        if (startDatePicker.getValue() != null &&
                logDate.isBefore(startDatePicker.getValue())) {
            return false;
        }

        if (endDatePicker.getValue() != null &&
                logDate.isAfter(endDatePicker.getValue())) {
            return false;
        }

        return true;
    }


    private boolean matchesDepartmentFilter(MovementLog log) {

        String selected = departmentFilter.getValue();

        return selected == null
                || selected.equals("All Departments")
                || log.getDepartment().equalsIgnoreCase(selected);
    }

    private boolean matchesEmployeeFilter(MovementLog log) {

        String keyword = employeeNameFilter.getText();

        return keyword == null
                || keyword.isBlank()
                || log.getEmployeeName().toLowerCase().contains(keyword.toLowerCase());
    }

    /* STATS */
    private void updateStatistics() {

        totalMovementsLabel.setText(String.valueOf(filteredData.size()));

        long out = filteredData.stream().filter(l -> !l.isStatus()).count();
        long returned = filteredData.stream().filter(MovementLog::isStatus).count();

        currentlyOutLabel.setText(String.valueOf(out));

        double compliance = filteredData.isEmpty()
                ? 0
                : (returned * 100.0 / filteredData.size());

        complianceRateLabel.setText(String.format("%.1f%%", compliance));
    }

    /* EXPORT ikaw na here luki
    */
    @FXML
    private void handleExportPdf() {
        new Alert(Alert.AlertType.INFORMATION, "PDF export not implemented yet.").show();
    }

    @FXML
    private void handleExportExcel() {
        new Alert(Alert.AlertType.INFORMATION, "Excel export not implemented yet.").show();
    }
}