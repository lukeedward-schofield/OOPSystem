package oopsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, Integer> idColumn;
    @FXML private TableColumn<Employee, String> departmentColumn;
    @FXML private TableColumn<Employee, String> positionColumn;
    @FXML private TableColumn<Employee, String> contactColumn;
    @FXML private TableColumn<Employee, Void> actionColumn;


    // =========================
    // REPOSITORY
    // =========================

    private final EmployeeRepository employeeRepository = new EmployeeRepository();


    // =========================
    // TABLE DATA
    // =========================

    private final ObservableList<Employee> employees = FXCollections.observableArrayList();


    // =========================
    // INITIALIZE
    // =========================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDataHeader();
        setupTableColumns();
        employeeTable.setItems(employees);

        setupActionButtonsColumn();

        loadEmployees();
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
                            employees.remove(selectedEmployee);
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

        employees.setAll(
                employeeRepository.getAllEmployees()
        );
    }

    //NAVIGATION METHODS
    @FXML
    public void addEmployee(){
        SceneNavigator.switchTo("employeeDirectory/AddEmployeeView");
    }
//    @FXML public void goToDashboard(){SceneNavigator.switchTo("dashboard/ashboardView");}
    @FXML public void goToPassSlipIssuance(){SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView");}
//    @FXML public void goToMovementLogs(){SceneNavigator.switchTo("movementLogs/MovementLogsView");}
    @FXML public void goToEmployeeDirectory(){SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");}
    @FXML public void gotoReports(){SceneNavigator.switchTo("reports/ReportsView");}
}
