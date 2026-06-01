package oopsystem;


import javafx.application.Application;
import oopsystem.app.EmployeeApplication;
import oopsystem.model.Employee;
import oopsystem.repository.EmployeeRepository;

import java.sql.SQLException;
import java.util.List;


public class Launcher {

    public static void main(String[] args) throws SQLException {

        EmployeeRepository repo = new EmployeeRepository();

        List<Employee> employees = repo.findAll();

        for(Employee employee : employees){
            System.out.println("ID: " + employee.getId());
            System.out.println("First Name: " + employee.getText());
        }

//        Application.launch(EmployeeApplication.class, args);
    }

}
