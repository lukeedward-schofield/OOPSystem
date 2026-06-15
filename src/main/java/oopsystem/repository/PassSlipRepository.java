package oopsystem.repository;

import oopsystem.model.Employee;
import oopsystem.model.PassSlip;
import oopsystem.util.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for all pass_slip table operations.
 * Follows the same pattern as EmployeeRepository:
 *   - public method takes plain values or a model object
 *   - opens a connection, runs SQL, closes everything in try-with-resources
 *   - private mapRow() converts ResultSet → model

 * Three main responsibilities:
 *   1. issuePassSlip()   — INSERT a new record, return the generated PK
 *   2. searchEmployees() — live employee search used by the form's search field
 *   3. updateFilePath()  — store the PDF path after generation
 *   4. findById()        — reload a full record by PK (used after insert)
 */
public class PassSlipRepository {

    // =========================================================================
    // ISSUE PASS SLIP
    // =========================================================================

    /**
     * Inserts a new pass slip record into the database.

     * time_out is set to NOW() at the exact moment of INSERT — this avoids
     * any clock drift between the Java side and the DB server.

     * time_in, duration, file_path are left NULL; they are populated later
     * when the employee returns (Movement Logs module) and after PDF generation.

     * status is set to FALSE — employee has not yet returned.

     * Returns the auto-generated pass_slip_ID on success, or -1 on failure.
     */
    public int issuePassSlip(PassSlip slip, int issuedByUserId) {

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
                VALUES (?, ?, ?, ?, NOW(), ?,'OUT')
                """;

        String logSql = """
                INSERT INTO activity_logs (user_id, action, log_in_details)
                VALUES (?, ?, ?)
                """;

        Connection conn = null;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // start transaction

            // --- 1. Insert pass slip ---
            int generatedId;
            try (PreparedStatement stmt = conn.prepareStatement(slipSql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setInt(1, slip.getEmployeeId());
                stmt.setInt(2, slip.getIssuedBy());
                stmt.setString(3, slip.getReason());

                if (slip.getDestination() == null || slip.getDestination().isBlank()) {
                    stmt.setNull(4, Types.VARCHAR);
                } else {
                    stmt.setString(4, slip.getDestination());
                }

                stmt.setInt(5, slip.getDuration());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return -1;
                    }
                    generatedId = keys.getInt(1);
                }
            }

            // --- 2. Write activity log ---
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {

                String details = String.format(
                        "Pass slip #%d issued for employee_id=%d. Reason: %s. Duration: %d min.",
                        generatedId,
                        slip.getEmployeeId(),
                        slip.getReason(),
                        slip.getDuration()
                );

                logStmt.setInt(1, issuedByUserId);
                logStmt.setString(2, "ISSUE_PASS_SLIP");
                logStmt.setString(3, details);
                logStmt.executeUpdate();
            }

            conn.commit(); // both inserts succeeded
            return generatedId;

        } catch (SQLException e) {
            System.err.println("Error issuing pass slip: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }

        return -1;
    }

    // =========================================================================
    // RECORD TIME-IN  (called by Movement Logs module)
    // =========================================================================

    /**
     * Records the employee's return time for an open (status=false) pass slip.
     *
     * Calculates duration in minutes directly in SQL using PostgreSQL's
     * EXTRACT(EPOCH ...) function, keeping the Java side clean.
     *
     * Also flips status to TRUE so the slip is marked as completed.
     *
     * Returns true if a row was updated, false otherwise.
     */
    public boolean recordTimeIn(int passSlipId, LocalDateTime timeIn) {

        String sql = """
                UPDATE pass_slip
                SET
                    time_in  = ?,
                    duration = CAST(EXTRACT(EPOCH FROM (? - time_out)) / 60 AS INT),
                    status   = TRUE
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

    /**
     * Saves the PDF file path back to the pass_slip row after generation.
     *
     * Called by PassSlipIssuanceController after the PDF is written to disk,
     * so the file_path column is populated and the file can be retrieved later.
     */
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

    /**
     * Searches for active employees matching the query string against
     * first_name, last_name, or department using case-insensitive ILIKE.
     *
     * Only returns employees with active_status = TRUE so inactive staff
     * cannot receive a pass slip.
     *
     * Results are capped at 10 to keep the dropdown manageable.
     * Used by the live search listener in PassSlipIssuanceController.
     */
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
                while (rs.next()) {
                    results.add(mapEmployee(rs));
                }
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

    /**
     * Loads a single pass slip by its PK.
     *
     * Used by the controller right after issuePassSlip() returns the new ID,
     * so the full record (including the DB-generated time_out) is available
     * for display and PDF generation.
     *
     * Returns null if no record is found.
     */
    public PassSlip findById(int passSlipId) {

        String sql = "SELECT * FROM pass_slip WHERE pass_slip_ID = ?";

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, passSlipId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPassSlip(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding pass slip: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================================
    // GET LATEST TODAY PASS SLIP (used by Dashboard)
    // =========================================================================

    /**
     * Returns the most recent pass slip issued today, joined with employee name.
     * Used by DashboardController to populate the Official Pass Slip section.
     * Returns null if no pass slip was issued today.
     */
    public PassSlip getLatestTodayPassSlip() {

        String sql = """
                SELECT ps.*,
                       e.first_name || ' ' || e.last_name AS employee_name
                FROM pass_slip ps
                INNER JOIN employee e ON ps.employee_id = e.employee_id
                WHERE DATE(ps.time_out) = CURRENT_DATE
                ORDER BY ps.time_out DESC
                LIMIT 1
                """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                PassSlip slip = mapPassSlip(rs);
                slip.setEmployeeName(rs.getString("employee_name"));
                return slip;
            }

        } catch (SQLException e) {
            System.err.println("Error getting latest pass slip: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // =========================================================================
    // DASHBOARD STAT COUNTS
    // =========================================================================

    /**
     * Returns count of employees currently out (status = OUT) today.
     * Used by DashboardController for "Total Employees Out" stat card.
     */
    public int getEmployeesOutCount() {
        String sql = """
                SELECT COUNT(*) FROM pass_slip
                WHERE DATE(time_out) = CURRENT_DATE
                  AND status = 'OUT'
                """;
        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Returns count of overdue pass slips today.
     * Used by DashboardController for "Pending Returns" stat card.
     */
    public int getPendingReturnsCount() {
        String sql = """
                SELECT COUNT(*) FROM pass_slip
                WHERE DATE(time_out) = CURRENT_DATE
                  AND status = 'OVERDUE'
                """;
        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Returns total count of pass slips issued today.
     * Used by DashboardController for "Total Pass Slips Today" stat card.
     */
    public int getTotalPassSlipsToday() {
        String sql = """
                SELECT COUNT(*) FROM pass_slip
                WHERE DATE(time_out) = CURRENT_DATE
                """;
        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
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

        int rawDuration  = rs.getInt("duration");
        Integer duration = rs.wasNull() ? null : rawDuration;

        boolean rawStatus = rs.getBoolean("status");
        Boolean status    = rs.wasNull() ? null : rawStatus;

        return new PassSlip(
                rs.getInt("pass_slip_ID"),
                rs.getInt("employee_id"),
                rs.getInt("issued_by"),
                rs.getString("reason"),
                rs.getString("destination"),
                rs.getString("file_path"),
                timeInTs  != null ? timeInTs.toLocalDateTime()  : null,
                timeOutTs != null ? timeOutTs.toLocalDateTime() : null,
                duration,
                status
        );
    }

    /**
     * Mirrors EmployeeRepository.mapRow() exactly — same column names,
     * same constructor order — so both repositories stay in sync.
     */
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