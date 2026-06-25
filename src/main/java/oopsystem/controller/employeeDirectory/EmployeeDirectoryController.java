package oopsystem.controller.employeeDirectory;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import oopsystem.factory.AlertFactory;
import oopsystem.model.Employee;
import oopsystem.service.EmployeeService;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class EmployeeDirectoryController implements Initializable {

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


    private final ObservableList<Employee> allEmployeesMasterList = FXCollections.observableArrayList();
    private FilteredList<Employee> filteredEmployees;

    private final ObservableList<Employee> visibleEmployeesPageList = FXCollections.observableArrayList();

    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
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

    @FXML
    public void goToAddEmployee(){
        SceneNavigator.switchTo("employeeDirectory/AddEmployeeView");
    }

    private void setupDataHeader(){
        EmployeeService employeeService = new EmployeeService();

        this.employeeCount.setText(String.valueOf(employeeService.getEmployeeCount()));
        this.activeEmployeeCount.setText(String.valueOf(employeeService.getActiveEmployeeCount()));
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

    private void setupActionButtonsColumn() {
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
                    showEditDialog(selectedEmployee);
                });

                deleteBtn.setOnAction(event -> {
                    Employee selectedEmployee = getTableView().getItems().get(getIndex());

                    // Wait for the user to click a button
                    Optional<ButtonType> result = AlertFactory.showDeleteConfirmation(selectedEmployee);
                    if (result.isPresent() && result.get() == ButtonType.OK) {

                        EmployeeService service = new EmployeeService();

                        boolean isDeleted = service.terminateEmployee(selectedEmployee);
                        if (isDeleted) {
                            refreshAfterDelete(selectedEmployee);
                            AlertFactory.employeeDeletionSuccess(selectedEmployee);
                        } else {

                            AlertFactory.employeeDeletionDatabaseError();
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

    private void loadEmployees() {
        EmployeeService employeeService = new EmployeeService();
        allEmployeesMasterList.setAll(employeeService.getAllEmployees());

        // Recalculate your page boundaries based on current row numbers
        resetPagination();
    }

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
            if (this.filteredEmployees == null) {
                return;
            }

            this.filteredEmployees.setPredicate(employee -> {
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
        pagination.setMaxPageIndicatorCount(Math.min(7, totalPagesCount));

        // Safely jump back to the first page frame
        pagination.setCurrentPageIndex(0);

        // Render the initial view frame manually
        updateTablePageFrame(0);
    }

    private void refreshAfterDelete(Employee deletedEmployee) {
        int previousPageIndex = pagination.getCurrentPageIndex();

        // Remove from the master list. The FilteredList will update from this source list.
        allEmployeesMasterList.removeIf(employee ->
                employee.getEmployeeId() == deletedEmployee.getEmployeeId()
        );

        // Refresh top summary cards/counts from the database.
        setupDataHeader();

        // Recalculate pagination based on the filtered list after deletion.
        int totalPagesCount = (int) Math.ceil((double) filteredEmployees.size() / ROWS_PER_PAGE);
        if (totalPagesCount == 0) totalPagesCount = 1;

        pagination.setPageCount(totalPagesCount);
        pagination.setMaxPageIndicatorCount(Math.min(7, totalPagesCount));

        int targetPageIndex = Math.min(previousPageIndex, totalPagesCount - 1);
        if (targetPageIndex < 0) targetPageIndex = 0;

        pagination.setCurrentPageIndex(targetPageIndex);
        updateTablePageFrame(targetPageIndex);
        employeeTable.refresh();
    }

    private void showEditDialog(Employee employee) {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle("Edit Employee");
        dialog.setHeaderText("Editing: " + employee.getFirstName() + " " + employee.getLastName());

        // --- Form Fields ---
        TextField firstNameField = new TextField(employee.getFirstName());
        TextField lastNameField = new TextField(employee.getLastName());
        TextField departmentField = new TextField(employee.getDepartment());
        TextField roleField = new TextField(employee.getRole());
        TextField contactField = new TextField(employee.getContactNumber());
        TextField emailField = new TextField(employee.getEmailAddress());

        // --- Layout ---
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        grid.add(new Label("First Name:"), 0, 0);  grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);   grid.add(lastNameField, 1, 1);
        grid.add(new Label("Department:"), 0, 2);  grid.add(departmentField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);        grid.add(roleField, 1, 3);
        grid.add(new Label("Contact:"), 0, 4);     grid.add(contactField, 1, 4);
        grid.add(new Label("Email:"), 0, 5);       grid.add(emailField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // --- Buttons ---
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // --- Result Converter ---
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String email = emailField.getText().trim();

                if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setHeaderText("Missing Fields");
                    alert.setContentText("First Name, Last Name, and Email are required.");
                    alert.showAndWait();
                    return null;
                }

                return new Employee(
                        employee.getEmployeeId(),
                        firstName,
                        lastName,
                        departmentField.getText().trim(),
                        roleField.getText().trim(),
                        contactField.getText().trim(),
                        email,
                        employee.isActiveStatus()
                );
            }
            return null;
        });

        // --- Handle Result ---
        dialog.showAndWait().ifPresent(updatedEmployee -> {
            EmployeeService employeeService = new EmployeeService();
            boolean employeeUpdated = employeeService.editEmployeeDetails(updatedEmployee);

            if (employeeUpdated) {
                // Refresh the master list in place so pagination/search stay intact
                int index = this.allEmployeesMasterList.indexOf(employee);
                if (index >= 0) {
                    this.allEmployeesMasterList.set(index, updatedEmployee);
                }
                updateTablePageFrame(pagination.getCurrentPageIndex());
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Update Failed");
                alert.setContentText("Could not update employee record.");
                alert.showAndWait();
            }
        });
    }
    

}
