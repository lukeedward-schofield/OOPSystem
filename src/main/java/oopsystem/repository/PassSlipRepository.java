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
    public int issuePassSlip(PassSlip slip) {

        String sql = """
                INSERT INTO pass_slip (
                    employee_id,
                    issued_by,
                    reason,
                    destination,
                    time_out,
                    status
                )
                VALUES (?, ?, ?, ?, NOW(), FALSE)
                """;

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, slip.getEmployeeId());
            stmt.setInt(2, slip.getIssuedBy());
            stmt.setString(3, slip.getReason());

            // destination is nullable in the schema — use setNull if blank
            if (slip.getDestination() == null || slip.getDestination().isBlank()) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, slip.getDestination());
            }

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error issuing pass slip: " + e.getMessage());
            e.printStackTrace();
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