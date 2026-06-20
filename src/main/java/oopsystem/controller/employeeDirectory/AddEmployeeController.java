package oopsystem.controller.employeeDirectory;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import oopsystem.model.Employee;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.EmployeeRepository;
import oopsystem.util.SceneNavigator;

public class AddEmployeeController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField contactNumberField;
    @FXML private TextField departmentField;
    @FXML private TextField positionField;

    private final EmployeeRepository repo =
            new EmployeeRepository();

    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();


    @FXML
    private void handleAddEmployee() {

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String contact = contactNumberField.getText().trim();
        String department = departmentField.getText().trim();
        String position = positionField.getText().trim();

        // 1. Validation for Blank Fields — Enforcing ALL fields must be filled up
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                contact.isBlank() || department.isBlank() || position.isBlank()) {

            showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing Fields",
                    "All fields are required. Please ensure First Name, Last Name, Email, " +
                            "Contact Number, Department, and Position are filled up.");
            return;
        }

        // 2. Format Validation: Ensure Contact Number contains ONLY integers
        if (!contact.matches("\\d+")) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Contact Number",
                    "The Contact Number field must contain integers only (digits 0-9). " +
                            "Please remove any letters, spaces, or special characters.");
            contactNumberField.requestFocus();
            return;
        }

        // 3. Database Validation: Check for Duplicate Email
        if (repo.existsByEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Duplicate Data Error", "Email Already Registered",
                    "The email address '" + email + "' is already assigned to an employee.");
            return;
        }

        // 4. Database Validation: Check for Duplicate Full Name (Case-Insensitive)
        if (repo.existsByFullName(firstName, lastName)) {
            showAlert(Alert.AlertType.ERROR, "Duplicate Data Error", "Employee Name Exists",
                    "An employee named '" + firstName + " " + lastName + "' already exists in the directory.");
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

            activityLogRepository.log(
                    "CREATE_EMPLOYEE",
                    String.format(
                            "Employee created: %s %s (%s)",
                            employee.getFirstName(),
                            employee.getLastName(),
                            employee.getDepartment()
                    )
            );

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Employee Added",
                    "Employee Successfully Added",
                    String.format(
                            "Employee %s %s has been added successfully.",
                            employee.getFirstName(),
                            employee.getLastName()
                    )
            );

            SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
            System.out.println("Employee added");
            clearForm();
        }
    }

    // Helper method to keep alert popup code clean and reusable
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
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
        SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
    }
}