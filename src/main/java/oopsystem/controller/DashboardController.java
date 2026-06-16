package oopsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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
    @FXML private HBox paginationBox;
    @FXML private ScrollPane mainScrollPane;

    private final MovementLogRepository movementLogRepository = new MovementLogRepository();
    private final PassSlipRepository passSlipRepository = new PassSlipRepository();
    private final ObservableList<MovementLog> movementLogs = FXCollections.observableArrayList();
    private final ObservableList<MovementLog> allLogs = FXCollections.observableArrayList();

    private int currentPage = 0;
    private static final int ROWS_PER_PAGE = 10;
    private boolean scrollPageChangeLock = false;

    @Override
    public void initialize(URL u, ResourceBundle r) {
        movementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        loadTodayMovements();
        loadLatestPassSlip();
        loadStatCards();
        setupScrollPagination();
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
        allLogs.setAll(logs);
        currentPage = 0;
        updateTablePage();
    }

    private void updateTablePage() {
        int from = currentPage * ROWS_PER_PAGE;
        int to = Math.min(from + ROWS_PER_PAGE, allLogs.size());

        movementLogs.setAll(allLogs.subList(from, to));
        movementTable.setItems(movementLogs);

        int total = allLogs.size();
        if (total == 0) {
            movFooter.setText("-");
        } else {
            movFooter.setText("Showing " + (from + 1) + " - " + to + " of " + total + " movements");
        }

        int totalPages = (int) Math.ceil((double) total / ROWS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        paginationBox.getChildren().clear();
        for (int i = 0; i < totalPages; i++) {
            final int pageIndex = i;
            Button btn = new Button(String.valueOf(i + 1));
            if (i == currentPage) {
                btn.setStyle("-fx-background-color:#8B0000; -fx-text-fill:white; -fx-background-radius:4px; -fx-min-width:28px; -fx-min-height:28px; -fx-padding:0;");
            } else {
                btn.setStyle("-fx-min-width:28px; -fx-min-height:28px; -fx-padding:0;");
            }
            btn.setOnAction(e -> {
                currentPage = pageIndex;
                updateTablePage();
            });
            paginationBox.getChildren().add(btn);
        }
    }

    /**
     * Allows switching between movement log pages by scrolling the table.
     * Scrolling down at the bottom of the table moves to the next page;
     * scrolling up at the top moves to the previous page.
     */
    private void setupScrollPagination() {
        movementTable.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (scrollPageChangeLock) return;

            int totalPages = (int) Math.ceil((double) allLogs.size() / ROWS_PER_PAGE);
            if (totalPages <= 1) return;

            ScrollBar vBar = getVerticalScrollBar(movementTable);
            if (vBar == null) return;

            boolean atBottom = vBar.getValue() >= vBar.getMax() - 0.0001;
            boolean atTop = vBar.getValue() <= vBar.getMin() + 0.0001;

            if (event.getDeltaY() < 0 && atBottom && currentPage < totalPages - 1) {
                currentPage++;
                updateTablePage();
                lockScrollMomentarily();
                event.consume();
            } else if (event.getDeltaY() > 0 && atTop && currentPage > 0) {
                currentPage--;
                updateTablePage();
                lockScrollMomentarily();
                event.consume();
            }
        });
    }

    private void lockScrollMomentarily() {
        scrollPageChangeLock = true;
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
        pause.setOnFinished(e -> scrollPageChangeLock = false);
        pause.play();
    }

    private ScrollBar getVerticalScrollBar(TableView<?> table) {
        for (javafx.scene.Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) node;
                if (bar.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                    return bar;
                }
            }
        }
        return null;
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

            String dest = slip.getDestination();
            String reason = slip.getReason();

            if (dest != null && !dest.isBlank()) {
                slipDest.setText(dest);
            } else if (reason != null && !reason.isBlank()) {
                slipDest.setText(reason);
            } else {
                slipDest.setText("-");
            }

        } else {
            slipSeries.setText("-");
            slipEmp.setText("-");
            slipDate.setText("-");
            slipTime.setText("-");
            slipDest.setText("-");
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