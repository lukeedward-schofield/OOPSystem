package oopsystem.repository;

import oopsystem.model.Employee;
import oopsystem.model.PassSlip;
import oopsystem.util.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PassSlipRepository {

    // =========================================================================
    // ISSUE PASS SLIP + ACTIVITY LOG  (single transaction)
    // =========================================================================

    /**
     * Inserts a new pass slip and writes an activity_log entry atomically.
     *
     * Column notes matching the actual DB schema:
     *   estimated_duration  — minutes entered by staff at issuance
     *   status              — pass_slip_status enum, starts as 'OUT'
     *   time_out            — set to NOW() by the DB server
     *   time_in / actual_duration  — NULL until the employee returns
     *
     * Returns the generated pass_slip_ID, or -1 on failure.
     */
    public int issuePassSlip(PassSlip slip, int issuedByUserId) {

        String checkSql = """
            SELECT 1 FROM pass_slip
            WHERE employee_id = ?
              AND status = 'OUT'
            LIMIT 1
            FOR UPDATE
            """;

        String slipSql = """
            INSERT INTO pass_slip (
                employee_id,
                issued_by,
                reason,
                destination,
                time_out,
                estimated_duration,
                status
            )
            VALUES (?, ?, ?, ?, NOW(), ?, 'OUT')
            """;

        String logSql = """
            INSERT INTO activity_logs (user_id, action, log_in_details)
            VALUES (?, ?, ?)
            """;

        Connection conn = null;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // Check inside the transaction with a row-level lock.
            // If another request is mid-insert for the same employee,
            // this blocks until that transaction commits or rolls back.
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, slip.getEmployeeId());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // Employee already has an open slip — abort cleanly
                        conn.rollback();
                        return -2; // distinct code so controller can show the right message
                    }
                }
            }

            // 1. Insert pass slip
            int generatedId;
            try (PreparedStatement stmt = conn.prepareStatement(slipSql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setInt(1, slip.getEmployeeId());
                stmt.setInt(2, slip.getIssuedBy());
                stmt.setString(3, slip.getReason());

                if (slip.getDestination() == null || slip.getDestination().isBlank()) {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                } else {
                    stmt.setString(4, slip.getDestination());
                }

                stmt.setInt(5, slip.getEstimatedDuration());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return -1;
                    }
                    generatedId = keys.getInt(1);
                }
            }

            // 2. Write activity log
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                String details = String.format(
                        "Pass slip #%d issued for employee_id=%d. Reason: %s. Est. duration: %d min.",
                        generatedId,
                        slip.getEmployeeId(),
                        slip.getReason(),
                        slip.getEstimatedDuration()
                );
                logStmt.setInt(1, issuedByUserId);
                logStmt.setString(2, "ISSUE_PASS_SLIP");
                logStmt.setString(3, details);
                logStmt.executeUpdate();
            }

            conn.commit();
            return generatedId;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Error issuing pass slip: " + e.getMessage());
            e.printStackTrace();

        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); }
                catch (SQLException ex) { ex.printStackTrace(); }
            }
        }

        return -1;
    }

    // =========================================================================
    // CHECK FOR OPEN PASS SLIP
    // =========================================================================

    /**
     * Returns true if the employee currently has an open pass slip (status = 'OUT').
     *
     * Called by the controller before issuePassSlip() to prevent duplicate
     * active slips for the same employee.
     */
    public boolean hasOpenPassSlip(int employeeId) {

        String sql = """
                SELECT 1 FROM pass_slip
                WHERE employee_id = ?
                  AND status = 'OUT'
                LIMIT 1
                """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking open pass slip: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================================
    // RECORD TIME-IN  (called by Movement Logs module)
    // =========================================================================

    /**
     * Records the employee's actual return time.
     * Calculates the real duration in minutes (time_in - time_out).
     * Flips status to 'IN'.
     */
    public boolean recordTimeIn(int passSlipId, LocalDateTime timeIn) {

        String sql = """
                UPDATE pass_slip
                SET
                    time_in         = ?,
                    actual_duration = CAST(EXTRACT(EPOCH FROM (? - time_out)) / 60 AS INT),
                    status          = 'IN'
                WHERE pass_slip_ID = ?
                  AND time_in IS NULL
                """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            Timestamp ts = Timestamp.valueOf(timeIn);
            stmt.setTimestamp(1, ts);
            stmt.setTimestamp(2, ts);
            stmt.setInt(3, passSlipId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error recording time-in: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================================
    // UPDATE FILE PATH
    // =========================================================================

    public boolean updateFilePath(int passSlipId, String filePath) {

        String sql = "UPDATE pass_slip SET file_path = ? WHERE pass_slip_ID = ?";

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, filePath);
            stmt.setInt(2, passSlipId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating file path: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================================
    // SEARCH EMPLOYEES
    // =========================================================================

    public List<Employee> searchEmployees(String query) {

        String sql = """
                SELECT *
                FROM employee
                WHERE active_status = TRUE
                  AND (
                      first_name  ILIKE ?
                   OR last_name   ILIKE ?
                   OR department  ILIKE ?
                  )
                ORDER BY last_name, first_name
                LIMIT 10
                """;

        List<Employee> results = new ArrayList<>();
        String pattern = "%" + query + "%";

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) results.add(mapEmployee(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error searching employees: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    // =========================================================================
    // FIND BY ID
    // =========================================================================

    public PassSlip findById(int passSlipId) {

        String sql = "SELECT * FROM pass_slip WHERE pass_slip_ID = ?";

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, passSlipId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapPassSlip(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error finding pass slip: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================================
    // DASHBOARD QUERIES
    // =========================================================================

    /**
     * Returns the most recently issued pass slip, regardless of date.
     * Used by the Dashboard "Recent Pass Slip" card so it always shows
     * something as long as at least one pass slip exists in the table.
     */
    public PassSlip getLatestTodayPassSlip() {
        String sql = """
        SELECT ps.pass_slip_ID,
               ps.employee_id,
               ps.issued_by,
               ps.reason,
               ps.destination,
               ps.file_path,
               ps.time_in,
               ps.time_out,
               ps.estimated_duration,
               ps.actual_duration,
               ps.status,
               e.first_name || ' ' || e.last_name AS employee_name
        FROM pass_slip ps
        INNER JOIN employee e ON ps.employee_id = e.employee_id
        ORDER BY ps.time_out DESC
        LIMIT 1
        """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                Timestamp timeInTs  = rs.getTimestamp("time_in");
                Timestamp timeOutTs = rs.getTimestamp("time_out");

                // Correctly handle primitive to nullable Integer conversions
                int rawEstDuration  = rs.getInt("estimated_duration");
                Integer estDuration = rs.wasNull() ? null : rawEstDuration;

                int rawDuration  = rs.getInt("actual_duration");
                Integer duration = rs.wasNull() ? null : rawDuration;

                String statusStr = rs.getString("status");

                // Calls the matching 11-argument database constructor perfectly
                PassSlip slip = new PassSlip(
                        rs.getInt("pass_slip_ID"),
                        rs.getInt("employee_id"),
                        rs.getInt("issued_by"),
                        rs.getString("reason"),
                        rs.getString("destination"),
                        rs.getString("file_path"),
                        timeInTs  != null ? timeInTs.toLocalDateTime()  : null,
                        timeOutTs != null ? timeOutTs.toLocalDateTime() : null,
                        estDuration,
                        duration,
                        statusStr
                );

                slip.setEmployeeName(rs.getString("employee_name"));
                return slip;
            }
        } catch (SQLException e) {
            System.err.println("Error getting latest pass slip: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public int getEmployeesOutCount() {
        String sql = "SELECT COUNT(*) FROM pass_slip WHERE DATE(time_out) = CURRENT_DATE AND status = 'OUT'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getPendingReturnsCount() {
        String sql = "SELECT COUNT(*) FROM pass_slip WHERE DATE(time_out) = CURRENT_DATE AND status = 'OVERDUE'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTotalPassSlipsToday() {
        String sql = "SELECT COUNT(*) FROM pass_slip WHERE DATE(time_out) = CURRENT_DATE";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // =========================================================================
    // PRIVATE MAPPERS
    // =========================================================================

    private PassSlip mapPassSlip(ResultSet rs) throws SQLException {

        Timestamp timeInTs  = rs.getTimestamp("time_in");
        Timestamp timeOutTs = rs.getTimestamp("time_out");

        int rawEstDuration  = rs.getInt("estimated_duration");
        Integer estDuration = rs.wasNull() ? null : rawEstDuration;

        int rawDuration  = rs.getInt("actual_duration");
        Integer duration = rs.wasNull() ? null : rawDuration;

        // status is a PostgreSQL enum — read as String, never as boolean
        String status = rs.getString("status");

        return new PassSlip(
                rs.getInt("pass_slip_ID"),
                rs.getInt("employee_id"),
                rs.getInt("issued_by"),
                rs.getString("reason"),
                rs.getString("destination"),
                rs.getString("file_path"),
                timeInTs  != null ? timeInTs.toLocalDateTime()  : null,
                timeOutTs != null ? timeOutTs.toLocalDateTime() : null,
                estDuration,
                duration,
                status
        );
    }

    private Employee mapEmployee(ResultSet rs) throws SQLException {
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
}