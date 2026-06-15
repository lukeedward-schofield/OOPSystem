package oopsystem.controller;

import oopsystem.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import oopsystem.model.User;
import oopsystem.repository.UserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField username;
    @FXML private PasswordField currentPassword;
    @FXML private PasswordField newPassword;
    @FXML private PasswordField confirmNewPassword;

    // --- YOUR TABLES AND COLUMNS INJECTIONS FROM FXML ---
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> departmentColumn;
    @FXML private TableColumn<User, String> roleColumn;// Void because it holds custom buttons, not data text

    // The data container linked directly to your UI table view
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final UserRepository userRepository = new UserRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1. LINK TEXT DATA: Match table text columns to your User class property names
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // 2. CONNECT DATA CONTAINER: Point your table view to your live list wrapper
        usersTable.setItems(userList);

        // 3. ADD BUTTONS DYNAMICALLY: Build the edit/delete layout rows

        // 4. POPULATE DATA: Run database retrieval to fill rows
        loadUsersFromDatabase();

        populateProfileFields();
    }
    /**
     * =========================================================================
     * BACKGROUND PROCESS: POPULATING THE DATA FROM THE REPOSITORY LAYER
     * =========================================================================
     */
    private void loadUsersFromDatabase() {
        // Run database calls in a background Task thread so your user interface stays responsive
        Task<ObservableList<User>> fetchTask = new Task<>() {
            @Override
            protected ObservableList<User> call() throws Exception {
                // Returns an ObservableList populated via the INNER JOIN query from your Repository class
                return userRepository.findAllUsersWithEmployeeDetails();
            }
        };

        // When the database fetching completes, push the final results into your live UI list tracking wrapper
        fetchTask.setOnSucceeded(event -> {
            userList.setAll(fetchTask.getValue());

            for (User user : userList) {
                System.out.println(
                        user.getUsername()
                                + " | "
                                + user.getDepartment()
                                + " | "
                                + user.getRole()
                );
            }
        });
        fetchTask.setOnFailed(event -> fetchTask.getException().printStackTrace());

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true); // Closes the background processing thread if the application window is terminated
        thread.start();
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

        System.out.println("change button clicked");
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        String newFirstName = firstName.getText().trim();
        String newLastName = lastName.getText().trim();
        String newUsername = username.getText().trim();
        String currentPw = currentPassword.getText();
        String newPw = newPassword.getText();
        String confirmPw = confirmNewPassword.getText();

        // --- Validation ---
        if (newFirstName.isBlank() || newLastName.isBlank() || newUsername.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "First name, last name, and username cannot be empty.");
            return;
        }

        if (currentPw.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Please enter your current password to save changes.");
            return;
        }

        // Verify current password against stored hash
        if (!BCrypt.checkpw(currentPw, currentUser.getUserPassword())) {
            showAlert(Alert.AlertType.ERROR, "Current password is incorrect.");
            return;
        }

        // Check if new username is taken by someone else
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

        // Determine final password — keep existing if no new password entered
        String finalHashedPassword;
        if (newPw.isBlank()) {
            // No change to password — keep the current hash
            finalHashedPassword = currentUser.getUserPassword();
        } else {
            if (!newPw.equals(confirmPw)) {
                showAlert(Alert.AlertType.WARNING, "New passwords do not match.");
                return;
            }
            finalHashedPassword = BCrypt.hashpw(newPw, BCrypt.gensalt());
        }

        // --- Save ---
        try {
            boolean success = userRepository.updateCredentials(
                    currentUser.getUserId(),
                    newUsername,
                    newFirstName,
                    newLastName,
                    finalHashedPassword
            );

            if (success) {
                // Update the session so the navbar/other screens reflect changes immediately
                currentUser.setFirstName(newFirstName);
                currentUser.setLastName(newLastName);
                currentUser.setUsername(newUsername);
                currentUser.setUserPassword(finalHashedPassword);

                currentPassword.clear();
                newPassword.clear();
                confirmNewPassword.clear();

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

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goToAddEmployee(){
        SceneNavigator.switchTo("addUserView");
    }
}
