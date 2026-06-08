package oopsystem.repository;



import oopsystem.model.Employee;
import oopsystem.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {

    public List<Employee> getAllEmployees() {

        List<Employee> employees = new ArrayList<>();

        String sql = """
                SELECT *
                FROM employee
                ORDER BY employee_id
                """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {

                employees.add(mapRow(rs));
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return employees;
    }

    private Employee mapRow(ResultSet rs) throws SQLException {

        return new Employee(

                rs.getInt("employee_id"),

                rs.getString("first_name"),

                rs.getString("last_name"),

                rs.getString("department"),

                rs.getString("role"),

                rs.getString("contact_number"),

                rs.getString("email_address"),

                rs.getBoolean("active_status")
        );
    }

    public boolean addEmployee(Employee employee) {

        String sql = """
        INSERT INTO employee (
            first_name,
            last_name,
            department,
            role,
            contact_number,
            email_address,
            active_status
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, employee.getFirstName());
            stmt.setString(2, employee.getLastName());
            stmt.setString(3, employee.getDepartment());
            stmt.setString(4, employee.getRole());
            stmt.setString(5, employee.getContactNumber());
            stmt.setString(6, employee.getEmailAddress());
            stmt.setBoolean(7, employee.isActiveStatus());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}