package oopsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import oopsystem.model.Employee;
import oopsystem.model.User;

public class ProfileController {

    @FXML
    private TableView<User> userTable;
    private TableColumn<User, String> usernameColumn;
    private TableColumn<User, String> departmentColumn;
    private TableColumn<User, String> roleColumn;
    private TableColumn<User, Integer> actionColumn;

}
