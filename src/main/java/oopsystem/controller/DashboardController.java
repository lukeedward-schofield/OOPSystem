package oopsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import oopsystem.model.MovementLog;
import oopsystem.model.PassSlip;
import oopsystem.repository.MovementLogRepository;
import oopsystem.repository.PassSlipRepository;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label statOut, statPending, statTotal;
    @FXML private Label statOutNote, statPendingNote, statTotalNote;
    @FXML private Label slipSeries, slipDate, slipEmp, slipTime, slipDest;
    @FXML private Button btnNewEntry;
    @FXML private TableView<MovementLog> movementTable;
    @FXML private TableColumn<MovementLog, String> colEmpName, colDept, colReason, colTimeOut, colStatus, colAction;
    @FXML private Label movFooter;

    private final MovementLogRepository movementLogRepository = new MovementLogRepository();
    private final PassSlipRepository passSlipRepository = new PassSlipRepository();
    private final ObservableList<MovementLog> movementLogs = FXCollections.observableArrayList();

    @Override
    public void initialize(URL u, ResourceBundle r) {
        movementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTodayMovements();
        loadLatestPassSlip();
        loadStatCards();
    }

    private void setupTableColumns() {
        colEmpName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmployeeName()));
        colDept.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDepartment()));
        colReason.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getReason()));
        colTimeOut.setCellValueFactory(data -> {
            if (data.getValue().getTimeOut() != null) {
                return new SimpleStringProperty(
                        data.getValue().getTimeOut().format(DateTimeFormatter.ofPattern("hh:mm a"))
                );
            }
            return new SimpleStringProperty("-");
        });
        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPassStatus()));
        colAction.setCellValueFactory(data ->
                new SimpleStringProperty("RECORD TIME-IN"));
    }

    private void loadTodayMovements() {
        List<MovementLog> logs = movementLogRepository.getAllMovementLogs();
        movementLogs.setAll(logs);
        movementTable.setItems(movementLogs);
        movFooter.setText("Showing " + logs.size() + " of " + logs.size() + " active movements");
    }

    private void loadLatestPassSlip() {
        PassSlip slip = passSlipRepository.getLatestTodayPassSlip();
        if (slip != null) {
            slipSeries.setText("SERIES NO: " + slip.getPassSlipId());
            slipEmp.setText(slip.getEmployeeName() != null ? slip.getEmployeeName() : "-");
            slipDate.setText(slip.getTimeOut() != null ?
                    slip.getTimeOut().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "-");
            slipTime.setText(slip.getTimeOut() != null ?
                    slip.getTimeOut().format(DateTimeFormatter.ofPattern("hh:mm a")) : "-");
            slipDest.setText(slip.getDestination() != null ? slip.getDestination() : "-");
        }
    }

    private void loadStatCards() {
        int out = passSlipRepository.getEmployeesOutCount();
        int pending = passSlipRepository.getPendingReturnsCount();
        int total = passSlipRepository.getTotalPassSlipsToday();

        statOut.setText(String.valueOf(out));
        statPending.setText(String.format("%02d", pending));
        statTotal.setText(String.valueOf(total));

        statOutNote.setText("↗ employees currently out");
        statPendingNote.setText(pending > 0 ? "⚠ " + pending + " pending returns" : "✓ All returned");
        statTotalNote.setText("Last updated: just now");
    }

    @FXML private void handleNewEntry() {
        SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView");
    }

    @FXML private void handlePrintCopy() {}

    @FXML private void goToIssuePassSlip() {
        SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView");
    }

    @FXML private void goToMovementLogs() {
        SceneNavigator.switchTo("movementLogs/MovementLogsView");
    }

    @FXML private void goToEmployeeDirectory() {
        SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
    }

    @FXML private void goToReports() {
        SceneNavigator.switchTo("reports/ReportsView");
    }

    @FXML private void goToSettings() {}

    @FXML private void goToLogout() {
        SceneNavigator.switchTo("login/LoginView");
    }
}