package oopsystem.repository;

import oopsystem.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import oopsystem.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public User authenticate(String username, String password) throws SQLException {

        String sql = """
        SELECT *
        FROM users
        WHERE username = ?
        AND user_password = ?
        AND active_status = true
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("user_password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getBoolean("active_status"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("employee_id")
                );
            }

            return null;
        }
    }

    public boolean createUser(String username, String password, int employeeId, String firstName, String lastName) throws SQLException {
        String sql = "INSERT INTO users (username, user_password, first_name, last_name, active_status, employee_id) " +
                "VALUES (?, ?, ?, ?, true, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, firstName);
            pstmt.setString(4, lastName);
            pstmt.setInt(5, employeeId);

            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    /**
     * READ: Fetches all users joined with their employee details for the TableView
     */
    public ObservableList<User> findAllUsersWithEmployeeDetails() throws SQLException {

        ObservableList<User> users = FXCollections.observableArrayList();

        // SQL query with INNER JOIN to pull department and role from your employee schema
        String sql = "SELECT u.user_id, u.username, u.user_password, u.first_name, u.last_name, " +
                "u.active_status, u.created_at, u.employee_id, e.department, e.role " +
                "FROM users u " +
                "INNER JOIN employee e ON u.employee_id = e.employee_id";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("user_password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getBoolean("active_status"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("employee_id")
                ));
            }
        }
        return users;
    }


    /**
     * READ: Returns list of employee IDs that already have a user account
     */
    public List<Integer> findEmployeeIdsWithExistingUsers() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT employee_id FROM users";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ids.add(rs.getInt("employee_id"));
            }
        }
        return ids;
    }
    /**
     * UPDATE: Modifies the user credentials and baseline metadata
     */
    public void updateUser(int userId, String newUsername, String newFirstName, String newLastName) throws SQLException {
        String query = "UPDATE users SET username = ?, first_name = ?, last_name = ? WHERE user_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newUsername);
            pstmt.setString(2, newFirstName);
            pstmt.setString(3, newLastName);
            pstmt.setInt(4, userId);

            pstmt.executeUpdate();
        }
    }

    /**
     * DELETE: Permanently drops a user account row
     */
    public void deleteUser(int userId) throws SQLException {
        String query = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * CREATE: Handles creating a brand-new Employee and linking a User account to it in one Transaction
     */
    public void createEmployeeAndUser(String empName, String dept, String role,
                                      String username, String rawPassword, String firstName, String lastName) throws SQLException {

        String insertEmpSql = "INSERT INTO employee (name, department, role) VALUES (?, ?, ?)";
        String insertUserSql = "INSERT INTO users (username, user_password, first_name, last_name, active_status, employee_id) VALUES (?, ?, ?, ?, true, ?)";

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Start a database Transaction

            int generatedEmployeeId = 0;

            // 1. Insert Employee and fetch the new primary key ID
            try (PreparedStatement empStmt = conn.prepareStatement(insertEmpSql, Statement.RETURN_GENERATED_KEYS)) {
                empStmt.setString(1, empName);
                empStmt.setString(2, dept);
                empStmt.setString(3, role);
                empStmt.executeUpdate();

                try (ResultSet rs = empStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedEmployeeId = rs.getInt(1); // This is your newly created employee_id
                    }
                }
            }

            // 2. Insert User and pass that generated employee_id as the foreign key pointer
            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql)) {
                userStmt.setString(1, username);
                userStmt.setString(2, rawPassword); // Tip: Hash this password before inserting in production!
                userStmt.setString(3, firstName);
                userStmt.setString(4, lastName);
                userStmt.setInt(5, generatedEmployeeId);
                userStmt.executeUpdate();
            }

            conn.commit(); // Save both operations to the database permanently [1]

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback(); // Cancel everything if either the employee or user query crashes [1]
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}