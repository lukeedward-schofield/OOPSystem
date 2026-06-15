package oopsystem.repository;

import oopsystem.model.ActivityLog;
import oopsystem.util.Database;
import oopsystem.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class ActivityLogRepository {

    /**
     * INSERT: Logs a user action into activity_logs.
     * Call this after every CRUD action across all controllers.
     */
    public void log(String action, String details) {
        int userId = SessionManager.getLoggedInUserId();
        if (userId == -1) return; // No active session, skip logging

        String sql = "INSERT INTO activity_logs (user_id, action, log_in_details) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            pstmt.setString(3, details);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            // Never crash the app because of a logging failure
            System.err.println("Failed to write activity log: " + e.getMessage());
        }
    }

    /**
     * READ: Fetches all activity logs joined with username for the TableView.
     */
    public ObservableList<ActivityLog> findAll() throws SQLException {
        ObservableList<ActivityLog> logs = FXCollections.observableArrayList();

        String sql = """
            SELECT al.log_id, al.user_id, al.action, al.log_in_details, al.created_at, u.username
            FROM activity_logs al
            INNER JOIN users u ON al.user_id = u.user_id
            ORDER BY al.created_at DESC
            """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(new ActivityLog(
                        rs.getInt("log_id"),
                        rs.getInt("user_id"),
                        rs.getString("action"),
                        rs.getString("log_in_details"),
                        rs.getTimestamp("created_at").toInstant().atZone(java.time.ZoneId.systemDefault()),
                        rs.getString("username")
                ));
            }
        }
        return logs;
    }
}