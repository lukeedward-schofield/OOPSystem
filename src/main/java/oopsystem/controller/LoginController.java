package oopsystem.controller;

import oopsystem.model.User;
import oopsystem.repository.UserRepository;
import oopsystem.util.AppConfig;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import oopsystem.util.SceneNavigator;

import java.sql.SQLException;

public class LoginController {

    @FXML
    TextField  usernameField;
    @FXML
    PasswordField passwordField;
    @FXML
    Button loginButton;


    @FXML
    public void login() {

        if (AppConfig.DEV_MODE) {
            SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
            return;
        }

        try {
            UserRepository repo = new UserRepository();

            System.out.println("Username: " + usernameField.getText());
            System.out.println("Password: " + passwordField.getText());

            User user = repo.authenticate(
                    usernameField.getText(),
                    passwordField.getText()
            );

            if (user != null) {
                SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
            } else {
                System.out.println("Invalid username or password");
            }

        } catch (SQLException e) {

            e.printStackTrace();

        }
    }
}
