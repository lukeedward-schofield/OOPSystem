package oopsystem.repository;

import oopsystem.model.Employee;
import oopsystem.model.PassSlip;
import oopsystem.util.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

public class PassSlipRepository {

    // =========================================================================
    // ISSUE PASS SLIP + ACTIVITY LOG  (single transaction)
    // =========================================================================

    /**
     * Inserts a new pass slip and writes an activity_log entry atomically.
     *
     * Column notes matching the actual DB schema:
     * estimated_duration  — minutes entered by staff at issuance
     * status              — pass_slip_status enum, starts as 'OUT'
     * time_out            — set to NOW() by the DB server
     * time_in / actual_duration  — NULL until the employee returns
     *
     * Returns the generated pass_slip_ID, or -1 on failure.
     */
    public int issuePassSlip(PassSlip slip, int issuedByUserId) {

        String checkSql = """
        SELECT 1 FROM pass_slip
        WHERE employee_id = ?
        AND status IN ('OUT', 'OVERDUE', 'Unresolved')
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
     * Returns true if the employee currently has an open pass slip (status = 'OUT' or 'OVERDUE').
     *
     * Called by the controller before issuePassSlip() to prevent duplicate
     * active slips for the same employee.
     */
    public boolean hasOpenPassSlip(int employeeId) {

        String sql = """
            SELECT 1 FROM pass_slip
            WHERE employee_id = ?
            AND status IN ('OUT', 'OVERDUE', 'Unresolved')
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
// MARK STALE SLIPS AS UNRESOLVED
// =========================================================================

    /**
     * Marks OUT/OVERDUE pass slips as UNRESOLVED when:
     *   - The slip is from a previous calendar day, OR
     *   - The slip is from today but current time is past the office cut-off
     *
     * Called by PassSlipIssuanceController.resolveStalePassSlips() on load.
     * Returns the number of rows updated.
     */
    public int markStaleAsUnresolved(LocalDate today, LocalTime now, LocalTime cutOff) {

        String sql = """
            UPDATE pass_slip
            SET status = 'Unresolved'
            WHERE status IN ('OUT', 'OVERDUE')
              AND (
                  DATE(time_out) < ?
                  OR (DATE(time_out) = ? AND time_out::time >= ?)
              )
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setDate(1, java.sql.Date.valueOf(today));
            stmt.setDate(2, java.sql.Date.valueOf(today));
            stmt.setTime(3, java.sql.Time.valueOf(cutOff));

            return stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error marking stale slips: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * DEBUG ONLY — marks ALL current OUT/OVERDUE slips as UNRESOLVED
     * regardless of date or time. Used with DEBUG_FORCE_UNRESOLVED = true
     * in PassSlipIssuanceController to test the UNRESOLVED state without
     * waiting for cut-off time or a new calendar day.
     *
     * Never call this in production flow.
     */
    public int markAllOpenAsUnresolved() {

        String sql = """
            UPDATE pass_slip
            SET status = 'Unresolved'
            WHERE status IN ('OUT', 'OVERDUE')
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            return stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error force-marking slips as unresolved: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
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
// FIND UNRESOLVED PASS SLIP FOR EMPLOYEE
// =========================================================================

    /**
     * Finds the most recent Unresolved pass slip for a given employee.
     * Used by the resolution dialog to show the details of the unresolved slip.
     * Returns null if none found.
     */
    public PassSlip findUnresolvedByEmployee(int employeeId) {

        String sql = """
            SELECT * FROM pass_slip
            WHERE employee_id = ?
              AND status = 'Unresolved'
            ORDER BY time_out DESC
            LIMIT 1
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, employeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapPassSlip(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding unresolved slip: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

// =========================================================================
// RESOLVE UNRESOLVED PASS SLIP
// =========================================================================

    /**
     * Resolves an Unresolved pass slip by recording the time-in, remarks,
     * actual duration, is_late flag, and updating the status.
     *
     * @param passSlipId  The ID of the Unresolved slip to resolve.
     * @param timeIn      The actual return time (NOW() or staff-entered).
     * @param remarks     Staff notes on why the slip was unresolved.
     * @param returnedLate  True → status becomes 'RETURNED LATE', false → 'RETURNED'
     * @return true if the update succeeded.
     */
    public boolean resolveUnresolvedSlip(int passSlipId,
                                         LocalDateTime timeIn,
                                         String remarks,
                                         boolean returnedLate) {
        String sql = """
            UPDATE pass_slip
            SET
                time_in         = ?,
                actual_duration = CAST(EXTRACT(EPOCH FROM (? - time_out)) / 60 AS INT),
                remarks         = ?,
                is_late         = ?,
                status          = 'RETURNED'
            WHERE pass_slip_ID = ?
              AND status = 'Unresolved'
            """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            Timestamp ts = Timestamp.valueOf(timeIn);
            stmt.setTimestamp(1, ts);
            stmt.setTimestamp(2, ts);
            stmt.setString(3, remarks == null || remarks.isBlank() ? null : remarks.trim());
            stmt.setBoolean(4, returnedLate);
            stmt.setInt(5, passSlipId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error resolving unresolved slip: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
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

                int rawEstDuration  = rs.getInt("estimated_duration");
                Integer estDuration = rs.wasNull() ? null : rawEstDuration;

                int rawDuration  = rs.getInt("actual_duration");
                Integer duration = rs.wasNull() ? null : rawDuration;

                String statusStr = rs.getString("status");

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
        String sql = "SELECT COUNT(*) FROM pass_slip WHERE status = 'OUT'";
        return executeCountQuery(sql);
    }

    public int getPendingReturnsCount() {
        String sql = "SELECT COUNT(*) FROM pass_slip WHERE status = 'OVERDUE'";
        return executeCountQuery(sql);
    }

    public int getTotalPassSlipsToday() {
        // No date filter — counts all pass slips ever issued
        String sql = "SELECT COUNT(*) FROM pass_slip";
        return executeCountQuery(sql);
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
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