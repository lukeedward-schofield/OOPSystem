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
import java.util.stream.Collectors;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.geometry.Side;


public class MovementLogController {

    /* NAVIGATION */
    @FXML
    public void goToDashboard() {
        SceneNavigator.switchTo("DashboardView");
    }

    @FXML
    public void goToPassSlipIssuance() {
        SceneNavigator.switchTo("PassSlipIssuanceView");
    }

    @FXML
    public void goToMovementLogs() {
        SceneNavigator.switchTo("MovementlogView");
    }

    @FXML
    public void goToEmployeeDirectory() {
        SceneNavigator.switchTo("EmployeeDirectoryView");
    }

    @FXML
    public void gotoReports() {
        SceneNavigator.switchTo("ReportsView");
    }

    /* FILTERS */
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> departmentFilter;
    @FXML
    private TextField employeeNameFilter;
    @FXML
    private Button exportBtn;

    /* STAT CARDS */
    @FXML
    private Label totalMovementsLabel;
    @FXML
    private Label currentlyOutLabel;
    @FXML
    private Label complianceRateLabel;

    /* TABLE */
    @FXML
    private TableView<MovementLog> movementLogsTable;
    @FXML
    private TableColumn<MovementLog, String> dateColumn;
    @FXML
    private TableColumn<MovementLog, String> employeeColumn;
    @FXML
    private TableColumn<MovementLog, String> reasonColumn;
    @FXML
    private TableColumn<MovementLog, String> timeOutColumn;
    @FXML
    private TableColumn<MovementLog, String> timeInColumn;
    @FXML
    private TableColumn<MovementLog, String> durationColumn;
    @FXML
    private TableColumn<MovementLog, String> statusColumn;

    /* PAGINATION */
    @FXML
    private Button prevPageBtn;
    @FXML
    private Button nextPageBtn;
    @FXML
    private Label pageInfoLabel;

    /* DETAIL CARD */
    @FXML
    private VBox detailCard;
    @FXML
    private Label detailTransactionId;
    @FXML
    private Label detailEmployee;
    @FXML
    private Label detailDepartment;
    @FXML
    private Label detailTimestamp;
    @FXML
    private Label detailStatus;
    @FXML
    private Label detailNotes;

    /* STATE */
    private final ObservableList<MovementLog> masterData = FXCollections.observableArrayList();
    private final ObservableList<MovementLog> filteredData = FXCollections.observableArrayList();
    private final ObservableList<MovementLog> pageData = FXCollections.observableArrayList();
    private final MovementLogRepository repository = new MovementLogRepository();

    private static final int ROWS_PER_PAGE = 10;
    private int currentPage = 1;
    private int totalPages = 1;

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
    private final DateTimeFormatter cardStampFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy | hh:mm a");
    private ContextMenu exportMenu;

    /* ------------------------------------------------------------------ */
    @FXML
    public void initialize() {
        setupTable();
        movementLogsTable.setItems(pageData);
        loadMovementLogs();
        setupAutoFiltering();
        setupSelectionListener();
        setupExportMenu();
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
            String dest = d.getValue().getDestination();
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
                d.getValue().getDuration() + " min"));

        statusColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPassStatus()));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle(switch (status) {
                        case "RETURNED" -> "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
                        case "OVERDUE" -> "-fx-text-fill: #d97706; -fx-font-weight: bold;";
                        default -> "-fx-text-fill: #800000; -fx-font-weight: bold;";
                    });
                }
            }
        });
    }


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
        departmentFilter.setOnAction(e -> applyFilters());
        startDatePicker.setOnAction(e -> applyFilters());
        endDatePicker.setOnAction(e -> applyFilters());
        employeeNameFilter.textProperty().addListener((obs, o, n) -> applyFilters());
    }
    private void setupClickOutsideListener() {
        movementLogsTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {

                    javafx.scene.Node target = (javafx.scene.Node) e.getTarget();

                    // ignore clicks on UI controls
                    while (target != null) {
                        if (target instanceof Button
                                || target instanceof TextField
                                || target instanceof ComboBox
                                || target instanceof TableView) {
                            return; // DO NOT clear selection
                        }
                        target = target.getParent();
                    }

                    // only clear if clicking truly empty space
                    movementLogsTable.getSelectionModel().clearSelection();
                });
            }
        });
    }

    @FXML
    private void handleApplyFilters() {
        applyFilters();
    }

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
        if (endDatePicker.getValue() != null && d.isAfter(endDatePicker.getValue())) return false;
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
        int to = Math.min(from + ROWS_PER_PAGE, total);

        pageData.setAll(filteredData.subList(from, to));

        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);

        clearDetailCard();
        updateStatistics();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshPage();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            refreshPage();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  DETAIL CARD                                                         */
    /* ------------------------------------------------------------------ */
    private void setupSelectionListener() {
        movementLogsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) populateDetailCard(selected);
                    else clearDetailCard();
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

        detailStatus.setText(log.getPassStatus());
        detailStatus.setStyle(switch (log.getPassStatus()) {
            case "RETURNED" -> "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
            case "OVERDUE" -> "-fx-text-fill: #d97706; -fx-font-weight: bold;";
            default -> "-fx-text-fill: #80000; -fx-font-weight: bold;"; // OUT
        });

        String dest = (log.getDestination() != null && !log.getDestination().isBlank())
                ? " heading to " + log.getDestination() : "";
        String reason = log.getReason() != null ? log.getReason().toLowerCase() : "unknown reason";
        detailNotes.setText("Employee requested pass slip for " + reason + dest + ".");
    }

    private void clearDetailCard() {
        detailCard.setVisible(false);
        detailCard.setManaged(false);
    }

    /* ------------------------------------------------------------------ */
    /*  STATS                                                               */
    /* ------------------------------------------------------------------ */
    private void updateStatistics() {
        long out = filteredData.stream().filter(l -> "OUT".equals(l.getPassStatus())).count();
        long returned = filteredData.stream().filter(l -> "RETURNED".equals(l.getPassStatus())).count();
        // overdue counts as still outside, so add to out display if you want
        long overdue = filteredData.stream().filter(l -> "OVERDUE".equals(l.getPassStatus())).count();

        totalMovementsLabel.setText(String.valueOf(filteredData.size()));
        currentlyOutLabel.setText(String.valueOf(out + overdue)); // OUT + OVERDUE = still outside
        double rate = filteredData.isEmpty() ? 0 : (returned * 100.0 / filteredData.size());
        complianceRateLabel.setText(String.format("%.1f%%", rate));
    }

    /* Time  In */
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


        String msg;
        if ("OVERDUE".equals(selected.getPassStatus())) {
            msg = "This employee is OVERDUE.\nAre you sure you want to record TIME IN?";
        } else {
            msg = "Record TIME IN for " + selected.getEmployeeName() + "?";
        }

        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.NO);

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
    /* EXPORT */
    private void setupExportMenu() {

        exportMenu = new ContextMenu();

        MenuItem exportPdf = new MenuItem("📄 Export as PDF");
        MenuItem exportExcel = new MenuItem("📊 Export as Excel");

        exportPdf.setOnAction(e -> handleExportPdf());
        exportExcel.setOnAction(e -> handleExportExcel());

        exportMenu.getItems().addAll(exportPdf, exportExcel);

        exportBtn.setOnAction(e -> {
            if (exportMenu.isShowing()) {
                exportMenu.hide();
            } else {
                exportMenu.show(exportBtn, Side.BOTTOM, 0, 4);
            }
        });
    }

    private void handleExportPdf() {
        new Alert(Alert.AlertType.INFORMATION, "PDF export coming soon.").show();
    }

    private void handleExportExcel() {
        new Alert(Alert.AlertType.INFORMATION, "Excel export coming soon.").show();
    }
}
