package oopsystem.repository;



import oopsystem.model.Employee;
import oopsystem.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeRepository {

    public List<Employee> getAllEmployees() {

        List<Employee> employees = new ArrayList<>();

        String sql = "SELECT * FROM employee ORDER BY employee_id";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()){
                employees.add(mapRow(rs));
            }
        } catch (SQLException e) {e.printStackTrace();}

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
            INSERT INTO employee (first_name,last_name,department,role,contact_number,email_address,active_status)
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

    public int getActiveEmployeeCount(){
        int employeeOnLeaveCount = 0;
        String sql = """
        SELECT COUNT(*)
        FROM employee
        WHERE active_status= TRUE""";

        try(Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
            if(rs.next()){
                employeeOnLeaveCount = rs.getInt(1);
            }
        } catch (SQLException e){
            // Log the error to your console so you can debug it later
            System.err.println("Error fetching active employee count: " + e.getMessage());
            e.printStackTrace();
            // The method will safely return 0 instead of crashing your app
        }

        return employeeOnLeaveCount;
    }

    public int getEmployeeCount(){
        int employeeCount = 0;

        String sql = """
        SELECT COUNT(*)
        FROM employee""";

        try(Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
            if(rs.next()){
                employeeCount = rs.getInt(1);
            }
        } catch (SQLException e) {
            // Log the error to your console
            System.err.println("Error fetching total employee count: " + e.getMessage());
            e.printStackTrace();
            // Defensively returns 0 if the database fails
        }

        return employeeCount;

    }

    // Inside your EmployeeRepository.java class
    public boolean deleteEmployeeById(int employeeId) {
        String sql = "DELETE FROM employee WHERE employee_id = ?";

        // Using try-with-resources to automatically close connection and statement
        try (Connection conn = Database.getConnection(); // Adjust this to match your connection getter
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, employeeId);
            int rowsAffected = pstmt.executeUpdate();

            // Returns true if a row was successfully deleted
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Database Error: Could not delete employee.");
            return false;
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM employee WHERE email_address = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean existsByFullName(String firstName, String lastName) {
        String sql = "SELECT COUNT(*) FROM employee WHERE LOWER(first_name) = LOWER(?) AND LOWER(last_name) = LOWER(?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}