package oopsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import oopsystem.model.Employee;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.EmployeeRepository;
import oopsystem.repository.UserRepository;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AddUserController implements Initializable {

    @FXML private ComboBox<Employee> employeeComboBox;
    @FXML private Label departmentLabel;
    @FXML private Label roleLabel;
    @FXML private Label emailLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEmployees();
        setupEmployeeComboBoxListener();
    }

    private void loadEmployees() {
        try {
            List<Integer> takenIds = userRepository.findEmployeeIdsWithExistingUsers();
            List<Employee> allEmployees = employeeRepository.getAllEmployees();

            List<Employee> available = allEmployees.stream()
                    .filter(e -> !takenIds.contains(e.getEmployeeId()))
                    .toList();

            employeeComboBox.getItems().setAll(available);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to load employees: " + e.getMessage());
        }
    }

    private void setupEmployeeComboBoxListener() {
        employeeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedEmployee) -> {
            if (selectedEmployee != null) {
                departmentLabel.setText("Department: " + selectedEmployee.getDepartment());
                roleLabel.setText("Role: " + selectedEmployee.getRole());
                emailLabel.setText("Email: " + selectedEmployee.getEmailAddress());
            } else {
                departmentLabel.setText("Department: -");
                roleLabel.setText("Role: -");
                emailLabel.setText("Email: -");
            }
        });
    }

    @FXML
    private void handleCreateUser() {
        Employee selectedEmployee = employeeComboBox.getSelectionModel().getSelectedItem();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // --- Validation ---
        if (selectedEmployee == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an employee.");
            return;
        }
        if (username.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Username cannot be empty.");
            return;
        }
        if (password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Password cannot be empty.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Passwords do not match.");
            return;
        }

        try {
            if (userRepository.existsByUsername(username)) {
                showAlert(Alert.AlertType.WARNING, "Username already taken. Please choose another.");
                return;
            }

            boolean success = userRepository.createUser(
                    username,
                    password,
                    selectedEmployee.getEmployeeId(),
                    selectedEmployee.getFirstName(),
                    selectedEmployee.getLastName()
            );

            if (success) {

                ActivityLogRepository logRepo = new ActivityLogRepository();

                logRepo.log(
                        "CREATE_USER",
                        String.format(
                                "User account created: %s for employee %s %s",
                                username,
                                selectedEmployee.getFirstName(),
                                selectedEmployee.getLastName()
                        )
                );

                showAlert(Alert.AlertType.INFORMATION, "User account created successfully.");
                SceneNavigator.switchTo("ProfileView");
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to create user. Please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        SceneNavigator.switchTo("ProfileView");
    }

    private void clearForm() {
        employeeComboBox.getSelectionModel().clearSelection();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        departmentLabel.setText("Department: -");
        roleLabel.setText("Role: -");
        emailLabel.setText("Email: -");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}