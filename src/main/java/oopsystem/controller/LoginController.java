package oopsystem.controller;

import oopsystem.model.User;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.UserRepository;
import oopsystem.util.AppConfig;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
            SceneNavigator.switchToMaximized("employeeDirectory/EmployeeDirectoryView");
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
                System.out.println("Invalid username or password");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
