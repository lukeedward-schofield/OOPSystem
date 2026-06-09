package oopsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import oopsystem.model.Employee;
import oopsystem.repository.EmployeeRepository;
import oopsystem.util.SceneNavigator;

public class AddEmployeeController {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField contactNumberField;

    @FXML
    private TextField departmentField;

    @FXML
    private TextField positionField;

    private final EmployeeRepository repo =
            new EmployeeRepository();


    @FXML
    private void handleAddEmployee() {

        String firstName = firstNameField.getText().trim();

        String lastName = lastNameField.getText().trim();

        String email = emailField.getText().trim();

        String contact = contactNumberField.getText().trim();

        String department = departmentField.getText().trim();

        String position = positionField.getText().trim();

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {

            System.out.println("Required fields missing");

            return;
        }

        Employee employee = new Employee(
                0,
                firstName,
                lastName,
                department,
                position,
                contact,
                email,
                true
        );

        boolean success = repo.addEmployee(employee);

        if (success) {
            SceneNavigator.switchTo("EmployeeDirectoryView");
            System.out.println("Employee added");
            clearForm();
        }
    }

    private void clearForm() {

        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        contactNumberField.clear();
        departmentField.clear();
        positionField.clear();
    }

    @FXML
    private void handleCancel() {
        clearForm();
    }
}