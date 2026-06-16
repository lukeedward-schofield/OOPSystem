package oopsystem.controller;

import javafx.scene.control.*;
import oopsystem.model.User;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.UserRepository;
import oopsystem.util.AppConfig;

import javafx.fxml.FXML;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;

import java.sql.SQLException;

public class LoginController {

    @FXML
    TextField  usernameField;
    @FXML
    PasswordField passwordField;
    @FXML
    Button loginButton;
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    @FXML
    public void login() {

        if (AppConfig.DEV_MODE) {
            try {
                UserRepository repo = new UserRepository();
                // Load the first available user from DB as the dev session user
                User devUser = repo.findFirstUser();
                if (devUser != null) {
                    SessionManager.setCurrentUser(devUser);

                    activityLogRepository.log("LOGIN", "User " + devUser.getUsername() + " logged in");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            SceneNavigator.switchToMaximized("dashboard/DashboardView");
            return;
        }

        try {
            UserRepository repo = new UserRepository();
            User user = repo.authenticate(usernameField.getText(), passwordField.getText());

            if (user != null) {
                SessionManager.setCurrentUser(user);
                activityLogRepository.log("LOGIN", "User " + user.getUsername() + " logged in");

                SceneNavigator.switchToMaximized("employeeDirectory/EmployeeDirectoryView");
            } else {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setHeaderText("Invalid Credentials");

                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        confirm.close();
                    }
                });
                System.out.println("Invalid username or password");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
