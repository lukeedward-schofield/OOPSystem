package oopsystem.repository;

import oopsystem.model.Employee;
import oopsystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {
    public EmployeeRepository()
    {

    }

    public List<Employee> findAll() throws SQLException
    {
        String query = "SELECT * FROM employee";
        List<Employee> employees = new ArrayList();

        try(Connection connection = Database.getConnecttion();
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
           ){
                while(rs.next())
                {
                    employees.add(new Employee(rs.getInt("employee_id"), rs.getString("first_name")));
                }
            }
        return employees;
     }


     public void create(){
        String query = "";
     }
}
