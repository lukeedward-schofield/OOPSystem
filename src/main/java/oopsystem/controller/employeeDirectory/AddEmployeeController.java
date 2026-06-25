package oopsystem.controller.employeeDirectory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import oopsystem.factory.DialogFactory;
import oopsystem.model.Employee;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.EmployeeRepository;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.util.ResourceBundle;

public class AddEmployeeController implements Initializable {

    @FXML private VBox employeeMainArea;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField contactNumberField;
    @FXML private TextField departmentField;
    @FXML private TextField positionField;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1. Declare the listener as a concrete object instance variable
        final javafx.beans.value.ChangeListener<Scene> oneTimeSceneListener =
                new javafx.beans.value.ChangeListener<Scene>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
                        if (newScene != null) {
                            // 🌟 THE UNPLUG FIX: Immediately remove this listener from the property tracker
                            employeeMainArea.sceneProperty().removeListener(this);

                            // Queue the dialog to fire safely on the next rendering frame pulse
                            Platform.runLater(() -> {
                                DialogFactory.showPermissionDialog(newScene);
                            });
                        }
                    }
                };

        // 2. Attach the one-time listener to your layout node container
        employeeMainArea.sceneProperty().addListener(oneTimeSceneListener);
    }



    @FXML
    private void handleAddEmployee() {
//SWAP ROOT CONTAINER TO STACKPANE FOR OVERLAYING



//        VALIDATION
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String contact = contactNumberField.getText().trim();
        String department = departmentField.getText().trim();
        String position = positionField.getText().trim();

//        // 1. Validation for Blank Fields — Enforcing ALL fields must be filled up
//        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
//                contact.isBlank() || department.isBlank() || position.isBlank()) {
//
//            showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing Fields",
//                    "All fields are required. Please ensure First Name, Last Name, Email, " +
//                            "Contact Number, Department, and Position are filled up.");
//            return;
//        }
//
//        // 2. Format Validation: Ensure Contact Number contains ONLY integers
//        if (!contact.matches("\\d+")) {
//            showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Contact Number",
//                    "The Contact Number field must contain integers only (digits 0-9). " +
//                            "Please remove any letters, spaces, or special characters.");
//            contactNumberField.requestFocus();
//            return;
//        }
//
//        // 3. Database Validation: Check for Duplicate Email
//        if (repo.existsByEmail(email)) {
//            showAlert(Alert.AlertType.ERROR, "Duplicate Data Error", "Email Already Registered",
//                    "The email address '" + email + "' is already assigned to an employee.");
//            return;
//        }
//
//        // 4. Database Validation: Check for Duplicate Full Name (Case-Insensitive)
//        if (repo.existsByFullName(firstName, lastName)) {
//            showAlert(Alert.AlertType.ERROR, "Duplicate Data Error", "Employee Name Exists",
//                    "An employee named '" + firstName + " " + lastName + "' already exists in the directory.");
//            return;
//        }

        Scene scene = employeeMainArea.getScene();
        DialogFactory.showPermissionDialog(scene);

//        Employee employee = new Employee(
//                0,
//                firstName,
//                lastName,
//                department,
//                position,
//                contact,
//                email,
//                true
//        );
//
//        boolean success = repo.addEmployee(employee);
//
//        if (success) {
//
//            activityLogRepository.log(
//                    "CREATE_EMPLOYEE",
//                    String.format(
//                            "Employee created: %s %s (%s)",
//                            employee.getFirstName(),
//                            employee.getLastName(),
//                            employee.getDepartment()
//                    )
//            );
//
//            showAlert(
//                    Alert.AlertType.INFORMATION,
//                    "Employee Added",
//                    "Employee Successfully Added",
//                    String.format(
//                            "Employee %s %s has been added successfully.",
//                            employee.getFirstName(),
//                            employee.getLastName()
//                    )
//            );
//
//            SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
//            System.out.println("Employee added");
//            clearForm();
//        }
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