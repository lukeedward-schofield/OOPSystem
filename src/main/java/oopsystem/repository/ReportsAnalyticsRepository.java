package oopsystem.repository;

import oopsystem.model.DailyReport;
import oopsystem.model.DepartmentUsage;
import oopsystem.model.MonthlyTrend;
import oopsystem.model.ReportSummary;
import oopsystem.util.Database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for the Reports and Analytics module.
 *
 * Main responsibility:
 * 1. Connect to the PostgreSQL/Supabase database through Database.getConnecttion().
 * 2. Run SQL queries for reports and analytics.
 * 3. Convert database rows into Java model objects.
 * 4. Return those model objects to the controller.
 *
 * This follows the project flow:
 * Controller -> Repository -> Database -> Repository creates Model -> Controller displays data.
 */
public class ReportsAnalyticsRepository {

    /**
     * Default allowed pass duration used for compliance and overdue calculations.
     *
     * Example:
     * If this is 120 minutes, a pass slip is considered on-time when the employee
     * returns within 2 hours after time_out.
     *
     * Later, this value can be replaced by a value from the System Settings module.
     */
    private static final int DEFAULT_PASS_DURATION_MINUTES = 120;

    /**
     * Gets the main summary values shown in the top cards of the Reports screen.
     *
     * Values calculated:
     * - total pass slips
     * - employees currently out
     * - overdue pass slips
     * - average duration
     * - compliance rate
     */
    public ReportSummary getSummary(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    COUNT(*) AS total_pass_slips,
                    COUNT(*) FILTER (WHERE p.time_in IS NULL) AS currently_out,
                    COUNT(*) FILTER (
                        WHERE p.time_in IS NULL
                        AND p.time_out < NOW() - (? * INTERVAL '1 minute')
                    ) AS overdue_passes,
                    COALESCE(AVG(
                        CASE
                            WHEN p.time_in IS NOT NULL AND p.time_out IS NOT NULL
                            THEN EXTRACT(EPOCH FROM (p.time_in - p.time_out)) / 60
                        END
                    ), 0) AS average_duration_minutes,
                    COUNT(*) FILTER (
                        WHERE p.time_in IS NOT NULL
                        AND p.time_out IS NOT NULL
                        AND p.time_in <= p.time_out + (? * INTERVAL '1 minute')
                    ) AS returned_on_time
                FROM pass_slip p
                WHERE p.time_out >= ? AND p.time_out < ?
                """;

        // try-with-resources automatically closes the connection, statement, and result set.
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // PreparedStatement prevents SQL injection and safely inserts values into the query.
            statement.setInt(1, DEFAULT_PASS_DURATION_MINUTES);
            statement.setInt(2, DEFAULT_PASS_DURATION_MINUTES);
            statement.setTimestamp(3, startTimestamp(startDate));
            statement.setTimestamp(4, endTimestampExclusive(endDate));

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_pass_slips");
                    int returnedOnTime = rs.getInt("returned_on_time");

                    // Avoid division by zero when there are no pass slips in the selected range.
                    double complianceRate = total == 0 ? 0 : (returnedOnTime * 100.0) / total;

                    return new ReportSummary(
                            total,
                            rs.getInt("currently_out"),
                            rs.getInt("overdue_passes"),
                            rs.getDouble("average_duration_minutes"),
                            complianceRate
                    );
                }
            }
        }

        // Safe fallback when the query returns no data.
        return new ReportSummary(0, 0, 0, 0, 0);
    }

    /**
     * Gets how many pass slips were issued per department.
     *
     * The query joins pass_slip and employee because the pass_slip table stores
     * employee_id, while the department is stored in the employee table.
     */
    public List<DepartmentUsage> getDepartmentUsage(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    COALESCE(e.department, 'Unassigned') AS department,
                    COUNT(*) AS total_slips,
                    CASE
                        WHEN SUM(COUNT(*)) OVER () = 0 THEN 0
                        ELSE (COUNT(*) * 100.0 / SUM(COUNT(*)) OVER ())
                    END AS percentage
                FROM pass_slip p
                INNER JOIN employee e ON e.employee_id = p.employee_id
                WHERE p.time_out >= ? AND p.time_out < ?
                GROUP BY COALESCE(e.department, 'Unassigned')
                ORDER BY total_slips DESC, department ASC
                """;

        List<DepartmentUsage> departments = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, startTimestamp(startDate));
            statement.setTimestamp(2, endTimestampExclusive(endDate));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    departments.add(new DepartmentUsage(
                            rs.getString("department"),
                            rs.getInt("total_slips"),
                            rs.getDouble("percentage")
                    ));
                }
            }
        }

        return departments;
    }

    /**
     * Gets daily report data for the table.
     *
     * One row is returned per date. This makes it easier for the controller to
     * display the daily compliance and overdue monitoring table.
     */
    public List<DailyReport> getDailyReports(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    CAST(p.time_out AS DATE) AS report_date,
                    COUNT(*) AS total_issued,
                    COUNT(*) FILTER (
                        WHERE p.time_in IS NOT NULL
                        AND p.time_out IS NOT NULL
                        AND p.time_in <= p.time_out + (? * INTERVAL '1 minute')
                    ) AS returned_on_time,
                    COUNT(*) FILTER (
                        WHERE p.time_in IS NULL
                        AND p.time_out < NOW() - (? * INTERVAL '1 minute')
                    ) AS overdue,
                    COALESCE(AVG(
                        CASE
                            WHEN p.time_in IS NOT NULL AND p.time_out IS NOT NULL
                            THEN EXTRACT(EPOCH FROM (p.time_in - p.time_out)) / 60
                        END
                    ), 0) AS average_duration_minutes
                FROM pass_slip p
                WHERE p.time_out >= ? AND p.time_out < ?
                GROUP BY CAST(p.time_out AS DATE)
                ORDER BY report_date DESC
                """;

        List<DailyReport> dailyReports = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, DEFAULT_PASS_DURATION_MINUTES);
            statement.setInt(2, DEFAULT_PASS_DURATION_MINUTES);
            statement.setTimestamp(3, startTimestamp(startDate));
            statement.setTimestamp(4, endTimestampExclusive(endDate));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int totalIssued = rs.getInt("total_issued");
                    int returnedOnTime = rs.getInt("returned_on_time");
                    double complianceRate = totalIssued == 0 ? 0 : (returnedOnTime * 100.0) / totalIssued;

                    // Convert java.sql.Date from the database into java.time.LocalDate for JavaFX/model use.
                    Date sqlDate = rs.getDate("report_date");
                    LocalDate reportDate = sqlDate == null ? null : sqlDate.toLocalDate();

                    dailyReports.add(new DailyReport(
                            reportDate,
                            totalIssued,
                            returnedOnTime,
                            rs.getInt("overdue"),
                            rs.getDouble("average_duration_minutes"),
                            complianceRate
                    ));
                }
            }
        }

        return dailyReports;
    }

    /**
     * Gets monthly trend data for the bar chart.
     *
     * The chart compares issued, returned, and overdue pass slips per month.
     */
    public List<MonthlyTrend> getMonthlyTrends(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    TO_CHAR(DATE_TRUNC('month', p.time_out), 'YYYY-MM') AS period,
                    COUNT(*) AS total_issued,
                    COUNT(*) FILTER (WHERE p.time_in IS NOT NULL) AS returned,
                    COUNT(*) FILTER (
                        WHERE p.time_in IS NULL
                        AND p.time_out < NOW() - (? * INTERVAL '1 minute')
                    ) AS overdue
                FROM pass_slip p
                WHERE p.time_out >= ? AND p.time_out < ?
                GROUP BY DATE_TRUNC('month', p.time_out)
                ORDER BY DATE_TRUNC('month', p.time_out)
                """;

        List<MonthlyTrend> trends = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, DEFAULT_PASS_DURATION_MINUTES);
            statement.setTimestamp(2, startTimestamp(startDate));
            statement.setTimestamp(3, endTimestampExclusive(endDate));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    trends.add(new MonthlyTrend(
                            rs.getString("period"),
                            rs.getInt("total_issued"),
                            rs.getInt("returned"),
                            rs.getInt("overdue")
                    ));
                }
            }
        }

        return trends;
    }

    /**
     * Converts a LocalDate into the start of that day.
     * Example: 2026-05-24 becomes 2026-05-24 00:00:00.
     */
    private Timestamp startTimestamp(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    /**
     * Converts the selected end date into an exclusive timestamp.
     *
     * Example:
     * If the user selects 2026-05-24 as the end date, this method returns
     * 2026-05-25 00:00:00, so all records during May 24 are included.
     */
    private Timestamp endTimestampExclusive(LocalDate date) {
        return Timestamp.valueOf(date.plusDays(1).atStartOfDay());
    }
}
