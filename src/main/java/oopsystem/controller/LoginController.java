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
            User devUser = new User(1, "dev", "", "Dev", "User", true, null, 1);
            SessionManager.setCurrentUser(devUser);
            activityLogRepository.log("LOGIN", "User dev logged in (DEV MODE)");
            SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
            return;
        }

        try {
            UserRepository repo = new UserRepository();
            User user = repo.authenticate(usernameField.getText(), passwordField.getText());

            if (user != null) {
                SessionManager.setCurrentUser(user);
                activityLogRepository.log("LOGIN", "User " + user.getUsername() + " logged in");
                SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
            } else {
                System.out.println("Invalid username or password");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
