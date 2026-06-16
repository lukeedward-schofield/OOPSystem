package oopsystem;


import javafx.application.Application;
import oopsystem.app.EmployeeApplication;
import oopsystem.model.Employee;
import oopsystem.repository.EmployeeRepository;

import java.sql.SQLException;
import java.util.List;


public class   Launcher {

    public static void main(String[] args) throws SQLException {
        Application.launch(EmployeeApplication.class, args);
    }

}
