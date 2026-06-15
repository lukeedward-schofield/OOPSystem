package oopsystem.controller;

import oopsystem.util.AppConfig;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import oopsystem.util.SceneNavigator;

public class LoginController {

    @FXML
    TextField  username;
    @FXML
    PasswordField password;
    @FXML
    Button login;


    @FXML
    public void login() {

        if (AppConfig.DEV_MODE) {
            SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");
            return;
        }

        // Real authentication goes here
    }
}
