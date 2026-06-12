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
           ps.duration,
           ps.status,
           ps.created_at
       FROM pass_slip ps
       INNER JOIN employee e
           ON ps.employee_id = e.employee_id
       ORDER BY ps.created_at DESC
        """;

    public List<MovementLog> getAllMovementLogs() {

        List<MovementLog> logs = new ArrayList<>();

        try (
                Connection conn = Database.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement(GET_ALL_MOVEMENT_LOGS);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                logs.add(
                        new MovementLog(
                                rs.getInt("pass_slip_id"),
                                rs.getString("first_name") + " " + rs.getString("last_name"),
                                rs.getString("department"),
                                rs.getString("reason"),
                                rs.getString("destination"),
                                rs.getTimestamp("time_out") != null
                                        ? rs.getTimestamp("time_out").toLocalDateTime()
                                        : null,
                                rs.getTimestamp("time_in") != null
                                        ? rs.getTimestamp("time_in").toLocalDateTime()
                                        : null,
                                rs.getInt("duration"),
                                rs.getBoolean("status"),
                                rs.getTimestamp("created_at") != null
                                        ? rs.getTimestamp("created_at").toLocalDateTime()
                                        : null
                        )
                );

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return logs;
    }

}
