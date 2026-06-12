package oopsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.HBox;
import oopsystem.model.Employee;
import oopsystem.repository.EmployeeRepository;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class EmployeeDirectoryController implements Initializable {

    // =========================
    // FXML COMPONENTS
    // =========================
    @FXML private Label employeeCount;
    @FXML private Label activeEmployeeCount;

    @FXML private TextField searchField;


    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, Integer> idColumn;
    @FXML private TableColumn<Employee, String> departmentColumn;
    @FXML private TableColumn<Employee, String> positionColumn;
    @FXML private TableColumn<Employee, String> contactColumn;
    @FXML private TableColumn<Employee, Void> actionColumn;

    @FXML private Pagination pagination;


    // =========================
    // REPOSITORY
    // =========================

    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final ObservableList<Employee> allEmployeesMasterList = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredEmployees;

    // =========================
    // TABLE DATA & PAGINATION VARIABLES
    // =========================
    // This acts as your master list containing ALL data loaded from your database
    private final ObservableList<Employee> visibleEmployeesPageList = FXCollections.observableArrayList();

    // This holds only the maximum 10 rows shown on screen at any given time
    private int currentPageIndex = 0;
    private static final int ROWS_PER_PAGE = 10;
    // =========================
    // INITIALIZE
    // =========================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDataHeader();
        setupTableColumns();
        employeeTable.setItems(visibleEmployeesPageList);
        setupActionButtonsColumn();

        // 1. Initialize your filteredEmployees array here so it is NEVER null
        filteredEmployees = new FilteredList<>(allEmployeesMasterList, p -> true);

        // 2. Set up your pagination listener ONCE right here
        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateTablePageFrame(newValue.intValue());
            }
        });

        // 3. Load your data rows
        loadEmployees();

        // 4. Start watching the search field text last
        setupSearchFilter();
    }


    // =========================
    // TABLE CONFIGURATION
    // =========================

    private void setupDataHeader(){
        this.employeeCount.setText(String.valueOf(this.employeeRepository.getEmployeeCount()));
        this.activeEmployeeCount.setText(String.valueOf(this.employeeRepository.getActiveEmployeeCount()));
    }

    private void setupTableColumns() {

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName())
        );

        idColumn.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));

        positionColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        contactColumn.setCellValueFactory(new PropertyValueFactory<>("emailAddress"));
    }

    private void setupActionButtonsColumn()
    {
        actionColumn.setCellFactory( param -> new TableCell<Employee, Void>()
        {
                private final HBox layoutContainer = new HBox();
                private final Button editBtn = new Button("Edit");
                private final Button deleteBtn = new Button("Delete");

            {
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");

                layoutContainer.getChildren().addAll(editBtn, deleteBtn);

                editBtn.setOnAction(event -> {
                    Employee selectedEmployee = getTableView().getItems().get(getIndex());

                    System.out.println("User clicked EDIT on row index: " + getIndex());
                    System.out.println("Extracted Hidden Database ID: " + selectedEmployee.getEmployeeId());
                    System.out.println("Extracted Target Username: " + selectedEmployee.getEmployeeId());

                    // TODO: Put your view transition or overlay activation logic here!
                });

                deleteBtn.setOnAction(event -> {
                    Employee selectedEmployee = getTableView().getItems().get(getIndex());

                    // 1. Show a confirmation popup alert
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Employee");
                    alert.setHeaderText("Are you sure you want to delete this employee?");
                    alert.setContentText("Employee ID: " + selectedEmployee.getEmployeeId() + "\nThis action cannot be undone.");

                    // 2. Wait for the user to click a button
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {

                        // 3. Attempt to delete from the PostgreSQL Database
                        boolean isDeleted = employeeRepository.deleteEmployeeById(selectedEmployee.getEmployeeId());

                        if (isDeleted) {
                            // 4. If DB deletion succeeds, drop it from the ObservableList to update the TableView instantly
                            allEmployeesMasterList.remove(selectedEmployee);
                            System.out.println("Successfully deleted from DB and UI.");
                        } else {
                            // 5. Show error alert if the database query fails
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("Database Error");
                            errorAlert.setHeaderText("Deletion Failed");
                            errorAlert.setContentText("Could not drop employee record from the database.");
                            errorAlert.showAndWait();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(layoutContainer);
                }
            }
        });
    }

    // =========================
    // LOAD EMPLOYEES
    // =========================

    private void loadEmployees() {
        // Fetch records from repository directly into master list
        allEmployeesMasterList.setAll(employeeRepository.getAllEmployees());

        // Recalculate your page boundaries based on current row numbers
        resetPagination();
    }

    /**
     * Slices the large master list into chunks of 10 rows maximum
     * based on your current page location tracking context variable.
     */
    private void updateTablePageFrame(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredEmployees.size());

        if (fromIndex <= toIndex && fromIndex >= 0) {
            visibleEmployeesPageList.setAll(filteredEmployees.subList(fromIndex, toIndex));
        } else {
            visibleEmployeesPageList.clear();
        }
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Safe check: If the list isn't ready yet, skip processing to prevent a crash
            if (filteredEmployees == null) {
                return;
            }

            filteredEmployees.setPredicate(employee -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();

                if (employee.getFirstName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (employee.getLastName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (String.valueOf(employee.getEmployeeId()).contains(lowerCaseFilter)) {
                    return true;
                } else if (employee.getDepartment() != null && employee.getDepartment().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (employee.getRole() != null && employee.getRole().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });

            resetPagination();
        });
    }

    private void resetPagination() {
        // Count total pages based on current filtered count, not master list count
        int totalPagesCount = (int) Math.ceil((double) filteredEmployees.size() / ROWS_PER_PAGE);
        if (totalPagesCount == 0) totalPagesCount = 1;

        pagination.setPageCount(totalPagesCount);

        // Safely jump back to the first page frame
        pagination.setCurrentPageIndex(0);

        // Render the initial view frame manually
        updateTablePageFrame(0);
    }



    // =========================
    // PAGINATION BUTTON ACTIONS
    // =========================

    @FXML
    public void addEmployee(){
        SceneNavigator.switchTo("employeeDirectory/AddEmployeeView");
    }
}
