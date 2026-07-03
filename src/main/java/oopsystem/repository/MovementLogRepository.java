package oopsystem.repository;

import oopsystem.model.MovementLog;
import oopsystem.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MovementLogRepository {

    private static final String GET_ALL_MOVEMENT_LOGS = """
            SELECT
                ps.pass_slip_id,
                e.first_name,
                e.last_name,
                e.department,
                ps.reason,
                ps.destination,
                ps.time_out,
                ps.time_in,
                ps.estimated_duration,
                ps.actual_duration,
                ps.status,
                ps.is_late,
                ps.created_at,
                ps.remarks
                FROM pass_slip ps
            INNER JOIN employee e
                ON ps.employee_id = e.employee_id
            ORDER BY ps.created_at DESC
            """;

    private static final String RECORD_TIME_IN = """
            UPDATE pass_slip
            SET time_in         = NOW(),
                actual_duration = EXTRACT(EPOCH FROM (NOW() - time_out))::INT / 60,
                status          = 'RETURNED',
                is_late         = (EXTRACT(EPOCH FROM (NOW() - time_out))::INT / 60) > (estimated_duration)
            WHERE pass_slip_id = ?
              AND status IN ('OUT', 'OVERDUE')
            """;

    private static final String MARK_OVERDUE = """
            UPDATE pass_slip
            SET status = 'OVERDUE'
            WHERE status = 'OUT'
              AND time_out IS NOT NULL
              AND estimated_duration > 0
              AND time_out + ((estimated_duration) * INTERVAL '1 minute') < NOW()
            """;

    public List<MovementLog> getAllMovementLogs() {
        syncOverdueStatuses(); // ← syncs DB before fetching

        List<MovementLog> logs = new ArrayList<>();

        try (
                Connection conn        = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(GET_ALL_MOVEMENT_LOGS);
                ResultSet rs           = stmt.executeQuery()
        ) {
            while (rs.next()) {
                logs.add(new MovementLog(
                        rs.getInt("pass_slip_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("department"),
                        rs.getString("reason"),
                        rs.getString("destination"),
                        rs.getTimestamp("time_out")   != null ? rs.getTimestamp("time_out").toLocalDateTime()   : null,
                        rs.getTimestamp("time_in")    != null ? rs.getTimestamp("time_in").toLocalDateTime()    : null,
                        rs.getInt("estimated_duration"),
                        rs.getInt("actual_duration"),
                        rs.getString("status"),
                        rs.getBoolean("is_late"),
                        rs.getString("remarks"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
                ));
                System.out.println(
                        "ID: " + rs.getInt("pass_slip_id")
                                + " Status: [" + rs.getString("status") + "]"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return logs;
    }

    public boolean recordTimeIn(int passSlipId) {
        try (
                Connection conn        = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(RECORD_TIME_IN)
        ) {
            stmt.setInt(1, passSlipId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void syncOverdueStatuses() {
        try (
                Connection conn        = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(MARK_OVERDUE)
        ) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}