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
        System.out.println("LOG CALLED — userId: " + userId + " | action: " + action + " | details: " + details);

        if (userId == -1) {
            System.out.println("LOG SKIPPED — no active session");
            return;
        }

        String sql = "INSERT INTO activity_logs (user_id, action, log_in_details) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            pstmt.setString(3, details);
            pstmt.executeUpdate();
            System.out.println("LOG SUCCESS");

        } catch (SQLException e) {
            System.err.println("LOG FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * READ: Fetches all activity logs joined with username for the TableView.
     */
    public ObservableList<ActivityLog> findAll() throws SQLException {
        ObservableList<ActivityLog> logs = FXCollections.observableArrayList();

        // Changed INNER JOIN to LEFT JOIN to preserve orphan logs
        String sql = """
        SELECT al.log_id, al.user_id, al.action, al.log_in_details, al.created_at, u.username
        FROM activity_logs al
        LEFT JOIN users u ON al.user_id = u.user_id
        ORDER BY al.created_at DESC
        """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Handle the username being null gracefully
                String username = rs.getString("username");
                if (username == null) {
                    username = "Deleted User"; // Fallback text for the UI table column
                }

                logs.add(new ActivityLog(
                        rs.getInt("log_id"),
                        rs.getInt("user_id"), // rs.getInt() returns 0 if database value is NULL
                        rs.getString("action"),
                        rs.getString("log_in_details"),
                        rs.getTimestamp("created_at").toInstant().atZone(java.time.ZoneId.systemDefault()),
                        username
                ));
            }
        }
        return logs;
    }
}