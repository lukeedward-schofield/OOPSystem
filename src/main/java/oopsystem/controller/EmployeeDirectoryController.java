package oopsystem.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import oopsystem.model.Employee;
import oopsystem.repository.EmployeeRepository;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.sql.SQLException;
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
