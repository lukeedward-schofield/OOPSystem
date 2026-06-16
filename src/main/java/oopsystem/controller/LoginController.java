package oopsystem.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import oopsystem.model.User;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.UserRepository;
import oopsystem.util.AppConfig;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;

import java.sql.SQLException;

public class LoginController {

    @FXML TextField       usernameField;
    @FXML PasswordField   passwordField;
    @FXML Button          loginButton;

    // ADDED: loading overlay fields
    @FXML VBox            loadingOverlay;
    @FXML Label           loadingLabel;

    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    @FXML
    public void login() {


        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
        loginButton.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);

        new Thread(() -> {
            try {
                if (AppConfig.DEV_MODE) {
                    UserRepository repo = new UserRepository();
                    User devUser = repo.findFirstUser();
                    if (devUser != null) {
                        SessionManager.setCurrentUser(devUser);
                        activityLogRepository.log("LOGIN", "User " + devUser.getUsername() + " logged in");
                    }
                    Platform.runLater(() ->
                            SceneNavigator.switchToMaximized("dashboard/DashboardView")
                    );
                    return;
                }

                UserRepository repo = new UserRepository();
                User user = repo.authenticate(usernameField.getText(), passwordField.getText());

                Platform.runLater(() -> {
                    if (user != null) {
                        SessionManager.setCurrentUser(user);
                        activityLogRepository.log("LOGIN", "User " + user.getUsername() + " logged in");
                        SceneNavigator.switchToMaximized("dashboard/DashboardView");
                    } else {
                        // ADDED: hide loading and re-enable fields on failed login
                        loadingOverlay.setVisible(false);
                        loadingOverlay.setManaged(false);
                        loginButton.setDisable(false);
                        usernameField.setDisable(false);
                        passwordField.setDisable(false);

                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setHeaderText("Invalid Credentials");
                        alert.setContentText("Incorrect username or password. Please try again.");
                        alert.showAndWait();
                    }
                });

            } catch (SQLException e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    loadingOverlay.setManaged(false);
                    loginButton.setDisable(false);
                    usernameField.setDisable(false);
                    passwordField.setDisable(false);

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Connection Error");
                    alert.setContentText("Could not connect to the database. Please try again.");
                    alert.showAndWait();
                });
            }
        }, "login-thread").start();
    }
}