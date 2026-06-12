package oopsystem.controller;

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
    public void login(){

        //authenticate first
<<<<<<< HEAD
        SceneNavigator.switchTo("Movementlogview");
=======
        SceneNavigator.switchTo("EmployeeDirectoryView");
>>>>>>> ed9fd4240ec981cf695e21e86b2c078354bd00f0
    }
}
