package oopsystem.controller.profile;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import oopsystem.model.ActivityLog;
import oopsystem.model.User;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.UserRepository;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    private static final int USERS_PER_PAGE = 8;
    private static final int ACTIVITY_LOGS_PER_PAGE = 10;
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);

    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField username;
    @FXML private PasswordField currentPassword;
    @FXML private PasswordField newPassword;
    @FXML private PasswordField confirmNewPassword;
    @FXML private Label profileStatusLabel;
    @FXML private TabPane profileTabPane;

    @FXML private TextField userSearchField;
    @FXML private Button clearUserFilterBtn;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> departmentColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private Pagination usersPagination;
    @FXML private Label usersPageInfoLabel;

    @FXML private TextField activitySearchField;
    @FXML private Button clearActivityFilterBtn;
    @FXML private TableView<ActivityLog> activityLogsTable;
    @FXML private TableColumn<ActivityLog, Number> activityLogIdColumn;
    @FXML private TableColumn<ActivityLog, String> activityUsernameColumn;
    @FXML private TableColumn<ActivityLog, String> activityActionColumn;
    @FXML private TableColumn<ActivityLog, String> activityDetailsColumn;
    @FXML private TableColumn<ActivityLog, String> activityCreatedAtColumn;
    @FXML private Pagination activityLogsPagination;
    @FXML private Label activityPageInfoLabel;

    private final ObservableList<User> allUsers = FXCollections.observableArrayList();
    private final ObservableList<User> filteredUsers = FXCollections.observableArrayList();
    private final ObservableList<ActivityLog> allActivityLogs = FXCollections.observableArrayList();
    private final ObservableList<ActivityLog> filteredActivityLogs = FXCollections.observableArrayList();

    private final UserRepository userRepository = new UserRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUsersTable();
        setupActivityLogsTable();
        setupFiltering();
        setupPagination();

        populateProfileFields();
        loadUsersFromDatabase();
        loadActivityLogsFromDatabase();
    }

    private void setupUsersTable() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        usersTable.setItems(FXCollections.observableArrayList());
    }

    private void setupActivityLogsTable() {
        activityLogIdColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getLogId()));
        activityUsernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(nullToDash(cellData.getValue().getUsername())));
        activityActionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(nullToDash(cellData.getValue().getAction())));
        activityDetailsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(nullToDash(cellData.getValue().getLogInDetails())));
        activityCreatedAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCreatedAt() == null
                        ? "-"
                        : cellData.getValue().getCreatedAt().format(LOG_DATE_FORMATTER)
        ));
        activityLogsTable.setItems(FXCollections.observableArrayList());
    }

    private void setupFiltering() {
        userSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyUserFilter());
        activitySearchField.textProperty().addListener((observable, oldValue, newValue) -> applyActivityFilter());
    }

    private void setupPagination() {
        usersPagination.setPageFactory(pageIndex -> {
            updateUsersTablePage(pageIndex);
            return emptyPaginationContent();
        });

        activityLogsPagination.setPageFactory(pageIndex -> {
            updateActivityLogsTablePage(pageIndex);
            return emptyPaginationContent();
        });
    }

    private Pane emptyPaginationContent() {
        Pane pane = new Pane();
        pane.setMinHeight(0);
        pane.setPrefHeight(0);
        pane.setMaxHeight(0);
        return pane;
    }

    private void loadUsersFromDatabase() {
        Task<ObservableList<User>> fetchTask = new Task<>() {
            @Override
            protected ObservableList<User> call() throws Exception {
                return userRepository.findAllUsersWithEmployeeDetails();
            }
        };

        fetchTask.setOnSucceeded(event -> {
            allUsers.setAll(fetchTask.getValue());
            applyUserFilter();
            setStatus("User table loaded successfully. " + allUsers.size() + " user record(s) found.");
        });

        fetchTask.setOnFailed(event -> {
            fetchTask.getException().printStackTrace();
            setStatus("Failed to load user records.");
            showAlert(Alert.AlertType.ERROR, "Failed to load user records: " + fetchTask.getException().getMessage());
        });

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadActivityLogsFromDatabase() {
        Task<ObservableList<ActivityLog>> fetchTask = new Task<>() {
            @Override
            protected ObservableList<ActivityLog> call() throws Exception {
                return activityLogRepository.findAll();
            }
        };

        fetchTask.setOnSucceeded(event -> {
            allActivityLogs.setAll(fetchTask.getValue());
            applyActivityFilter();
        });

        fetchTask.setOnFailed(event -> {
            fetchTask.getException().printStackTrace();
            setStatus("Failed to load activity logs.");
            showAlert(Alert.AlertType.ERROR, "Failed to load activity logs: " + fetchTask.getException().getMessage());
        });

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void applyUserFilter() {
        String query = normalize(userSearchField.getText());
        filteredUsers.setAll(allUsers.filtered(user -> {
            if (query.isEmpty()) return true;
            return contains(user.getUsername(), query)
                    || contains(user.getFirstName(), query)
                    || contains(user.getLastName(), query)
                    || contains(user.getDepartment(), query)
                    || contains(user.getRole(), query);
        }));
        refreshUsersPagination();
    }

    private void applyActivityFilter() {
        String query = normalize(activitySearchField.getText());
        filteredActivityLogs.setAll(allActivityLogs.filtered(log -> {
            if (query.isEmpty()) return true;
            String createdAt = log.getCreatedAt() == null ? "" : log.getCreatedAt().format(LOG_DATE_FORMATTER);
            return contains(log.getUsername(), query)
                    || contains(log.getAction(), query)
                    || contains(log.getLogInDetails(), query)
                    || contains(createdAt, query)
                    || String.valueOf(log.getLogId()).contains(query)
                    || String.valueOf(log.getUserId()).contains(query);
        }));
        refreshActivityPagination();
    }

    private void refreshUsersPagination() {
        int pageCount = calculatePageCount(filteredUsers.size(), USERS_PER_PAGE);
        usersPagination.setPageCount(pageCount);
        usersPagination.setCurrentPageIndex(0);
        updateUsersTablePage(0);
    }

    private void refreshActivityPagination() {
        int pageCount = calculatePageCount(filteredActivityLogs.size(), ACTIVITY_LOGS_PER_PAGE);
        activityLogsPagination.setPageCount(pageCount);
        activityLogsPagination.setCurrentPageIndex(0);
        updateActivityLogsTablePage(0);
    }

    private int calculatePageCount(int totalItems, int rowsPerPage) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) rowsPerPage));
    }

    private void updateUsersTablePage(int pageIndex) {
        int fromIndex = pageIndex * USERS_PER_PAGE;
        int toIndex = Math.min(fromIndex + USERS_PER_PAGE, filteredUsers.size());

        if (filteredUsers.isEmpty() || fromIndex >= filteredUsers.size()) {
            usersTable.setItems(FXCollections.observableArrayList());
            usersPageInfoLabel.setText("No user records found");
            return;
        }

        usersTable.setItems(FXCollections.observableArrayList(filteredUsers.subList(fromIndex, toIndex)));
        usersPageInfoLabel.setText("Showing " + (fromIndex + 1) + "-" + toIndex + " of " + filteredUsers.size() + " user(s)");
    }

    private void updateActivityLogsTablePage(int pageIndex) {
        int fromIndex = pageIndex * ACTIVITY_LOGS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ACTIVITY_LOGS_PER_PAGE, filteredActivityLogs.size());

        if (filteredActivityLogs.isEmpty() || fromIndex >= filteredActivityLogs.size()) {
            activityLogsTable.setItems(FXCollections.observableArrayList());
            activityPageInfoLabel.setText("No activity logs found");
            return;
        }

        activityLogsTable.setItems(FXCollections.observableArrayList(filteredActivityLogs.subList(fromIndex, toIndex)));
        activityPageInfoLabel.setText("Showing " + (fromIndex + 1) + "-" + toIndex + " of " + filteredActivityLogs.size() + " log(s)");
    }

    @FXML
    private void handleClearUserFilter() {
        userSearchField.clear();
        applyUserFilter();
        setStatus("User filter cleared.");
    }

    @FXML
    private void handleClearActivityFilter() {
        activitySearchField.clear();
        applyActivityFilter();
        setStatus("Activity log filter cleared.");
    }

    /**
     * Exports the currently filtered activity logs to an Excel-readable .xls file.
     * Uses SpreadsheetML so it opens cleanly in Microsoft Excel without CSV formatting issues.
     */
    @FXML
    private void handleExportActivityLogsExcel() {
        if (filteredActivityLogs.isEmpty()) {
            setStatus("No activity logs available to export.");
            showAlert(Alert.AlertType.INFORMATION, "No activity logs available to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Activity Logs as Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xls)", "*.xls"));
        fileChooser.setInitialFileName("activity-logs.xls");

        File file = fileChooser.showSaveDialog(activityLogsTable.getScene().getWindow());
        if (file == null) {
            setStatus("Activity logs export cancelled.");
            return;
        }

        try {
            writeActivityLogsExcel(ensureExtension(file, "xls"));
            setStatus("Activity logs exported successfully.");
            showAlert(Alert.AlertType.INFORMATION, "Activity logs exported successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Failed to export activity logs.");
            showAlert(Alert.AlertType.ERROR, "Failed to export activity logs: " + e.getMessage());
        }
    }

    private void writeActivityLogsExcel(File file) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Activity Logs"));
        rows.add(List.of("Exported Records", String.valueOf(filteredActivityLogs.size())));
        rows.add(List.of(""));
        rows.add(List.of("Log ID", "User", "Action", "Details", "Date / Time"));

        for (ActivityLog log : filteredActivityLogs) {
            rows.add(List.of(
                    String.valueOf(log.getLogId()),
                    nullToDash(log.getUsername()),
                    nullToDash(log.getAction()),
                    nullToDash(log.getLogInDetails()),
                    log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(LOG_DATE_FORMATTER)
            ));
        }

        StringBuilder workbook = new StringBuilder();
        workbook.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        workbook.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
        workbook.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
                .append("xmlns:o=\"urn:schemas-microsoft-com:office:office\" ")
                .append("xmlns:x=\"urn:schemas-microsoft-com:office:excel\" ")
                .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");

        workbook.append("<Styles>")
                .append("<Style ss:ID=\"Title\"><Font ss:Bold=\"1\" ss:Size=\"16\" ss:Color=\"#7A0000\"/></Style>")
                .append("<Style ss:ID=\"Header\"><Font ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>")
                .append("<Interior ss:Color=\"#7A0000\" ss:Pattern=\"Solid\"/>")
                .append("<Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>")
                .append("<Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>")
                .append("<Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>")
                .append("<Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/></Borders></Style>")
                .append("<Style ss:ID=\"Cell\"><Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0CCCC\"/>")
                .append("<Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0CCCC\"/>")
                .append("<Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0CCCC\"/>")
                .append("<Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0CCCC\"/></Borders></Style>")
                .append("</Styles>\n");

        appendExcelWorksheet(workbook, "Activity Logs", rows, new int[]{70, 150, 170, 420, 160});
        workbook.append("</Workbook>");
        Files.writeString(file.toPath(), workbook.toString(), StandardCharsets.UTF_8);
    }

    private void appendExcelWorksheet(StringBuilder workbook, String sheetName, List<List<String>> rows, int[] columnWidths) {
        workbook.append("<Worksheet ss:Name=\"").append(xmlEscape(sheetName)).append("\">")
                .append("<Table>");

        for (int width : columnWidths) {
            workbook.append("<Column ss:Width=\"").append(width).append("\"/>");
        }

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            String style = rowIndex == 0 ? "Title" : rowIndex == 3 ? "Header" : rowIndex > 3 ? "Cell" : "";

            workbook.append("<Row>");
            for (String value : row) {
                workbook.append("<Cell");
                if (!style.isEmpty()) {
                    workbook.append(" ss:StyleID=\"").append(style).append("\"");
                }
                workbook.append("><Data ss:Type=\"String\">")
                        .append(xmlEscape(value == null ? "" : value))
                        .append("</Data></Cell>");
            }
            workbook.append("</Row>");
        }

        workbook.append("</Table></Worksheet>\n");
    }

    private File ensureExtension(File file, String extension) {
        String path = file.getAbsolutePath();
        if (path.toLowerCase(Locale.ENGLISH).endsWith("." + extension.toLowerCase(Locale.ENGLISH))) {
            return file;
        }
        return new File(path + "." + extension);
    }

    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void populateProfileFields() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            firstName.setText(currentUser.getFirstName());
            lastName.setText(currentUser.getLastName());
            username.setText(currentUser.getUsername());
        }
    }

    @FXML
    private void handleSaveProfile() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        String newFirstName = firstName.getText().trim();
        String newLastName = lastName.getText().trim();
        String newUsername = username.getText().trim();
        String currentPw = currentPassword.getText();
        String newPw = newPassword.getText();
        String confirmPw = confirmNewPassword.getText();

        if (newFirstName.isBlank() || newLastName.isBlank() || newUsername.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "First name, last name, and username cannot be empty.");
            return;
        }

        if (currentPw.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Please enter your current password to save changes.");
            return;
        }

        if (!BCrypt.checkpw(currentPw, currentUser.getUserPassword())) {
            showAlert(Alert.AlertType.ERROR, "Current password is incorrect.");
            return;
        }

        try {
            if (userRepository.existsByUsernameExcluding(newUsername, currentUser.getUserId())) {
                showAlert(Alert.AlertType.WARNING, "Username '" + newUsername + "' is already taken.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error: " + e.getMessage());
            return;
        }

        String finalHashedPassword;
        if (newPw.isBlank()) {
            finalHashedPassword = currentUser.getUserPassword();
        } else {
            if (!newPw.equals(confirmPw)) {
                showAlert(Alert.AlertType.WARNING, "New passwords do not match.");
                return;
            }
            finalHashedPassword = BCrypt.hashpw(newPw, BCrypt.gensalt());
        }

        try {
            boolean success = userRepository.updateCredentials(
                    currentUser.getUserId(),
                    newUsername,
                    newFirstName,
                    newLastName,
                    finalHashedPassword
            );

            if (success) {
                activityLogRepository.log(
                        "UPDATE_USER",
                        String.format("User account updated: %s", newUsername)
                );

                currentUser.setFirstName(newFirstName);
                currentUser.setLastName(newLastName);
                currentUser.setUsername(newUsername);
                currentUser.setUserPassword(finalHashedPassword);

                currentPassword.clear();
                newPassword.clear();
                confirmNewPassword.clear();

                loadUsersFromDatabase();
                loadActivityLogsFromDatabase();
                setStatus("Profile updated successfully.");
                showAlert(Alert.AlertType.INFORMATION, "Profile updated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to update profile. Please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteOwnAccount() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Are you sure you want to delete your account?");
        confirm.setContentText("This action is permanent and cannot be undone. You will be logged out immediately.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String activeUsername = currentUser.getUsername();

                    activityLogRepository.log(
                            "DELETE_OWN_ACCOUNT",
                            "User deleted their own account: " + activeUsername
                    );

                    userRepository.deleteUser(currentUser.getUserId());
                    SessionManager.clearSession();
                    SceneNavigator.switchTo("login/LoginView");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Failed to delete account: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void goToAddEmployee() {
        SceneNavigator.switchTo("addUserView");
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(query);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void setStatus(String message) {
        if (profileStatusLabel != null) {
            profileStatusLabel.setText(message == null ? "" : message);
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
