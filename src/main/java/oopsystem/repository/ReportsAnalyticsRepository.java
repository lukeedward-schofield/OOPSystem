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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for the Reports and Analytics module.
 *
 * Main responsibility:
 * 1. Connect to the PostgreSQL/Supabase database through Database.getConnection().
 * 2. Run SQL queries for reports and analytics.
 * 3. Convert database rows into Java model objects.
 * 4. Return those model objects to the controller.
 *
 * This follows the project flow:
 * Controller -> Repository -> Database -> Repository creates Model -> Controller displays data.
 */
public class ReportsAnalyticsRepository {

    /**
     * Movement Logs marks a pass slip late when it exceeds the estimated duration
     * plus three grace minutes. Reports use the same rule so every module shows
     * the same overdue/compliance result.
     */
    private static final int GRACE_PERIOD_MINUTES = 0;

    /**
     * Gets the earliest and latest available pass slip dates from the database.
     *
     * The controller uses this for the initial date picker values. This prevents
     * the report screen from looking empty just because the selected date range
     * does not match the database records.
     */
    public LocalDate[] getAvailableDateRange() throws SQLException {
        String sql = """
                SELECT
                    MIN(CAST(COALESCE(time_out, created_at) AS DATE)) AS start_date,
                    MAX(CAST(COALESCE(time_out, created_at) AS DATE)) AS end_date
                FROM pass_slip
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                Date start = rs.getDate("start_date");
                Date end = rs.getDate("end_date");

                if (start != null && end != null) {
                    return new LocalDate[]{start.toLocalDate(), end.toLocalDate()};
                }
            }
        }

        // Fallback when the database has no pass slips yet.
        return new LocalDate[]{LocalDate.now().minusDays(30), LocalDate.now()};
    }

    /**
     * Updates open pass slips into OVERDUE when they already exceeded their
     * estimated duration plus grace period.
     *
     * This mirrors MovementLogRepository so Reports reflects the latest status
     * even before the Movement Logs page is opened.
     */
    public void syncOverdueStatuses() throws SQLException {
        String sql = """
                UPDATE pass_slip
                SET status = 'OVERDUE'
                WHERE status = 'OUT'
                  AND time_out IS NOT NULL
                  AND estimated_duration > 0
                  AND time_out + ((estimated_duration + ?) * INTERVAL '1 minute') < NOW()
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, GRACE_PERIOD_MINUTES);
            statement.executeUpdate();
        }
    }

    /**
     * Gets the main summary values shown in the top cards of the Reports screen.
     *
     * Values calculated:
     * - total pass slips
     * - employees currently out
     * - overdue pass slips
     * - average duration
     * - compliance rate
     * - comparison indicators beside each card
     */
    public ReportSummary getSummary(LocalDate startDate, LocalDate endDate) throws SQLException {
        SummaryMetrics current = getSummaryMetrics(startDate, endDate);

        long periodLength = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate previousEnd = startDate.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(periodLength - 1);
        SummaryMetrics previous = getSummaryMetrics(previousStart, previousEnd);

        double totalChangePercent = percentChange(current.totalPassSlips, previous.totalPassSlips);
        double complianceChange = current.complianceRate - previous.complianceRate;
        double averageDurationChange = current.averageDurationMinutes - previous.averageDurationMinutes;
        int overdueChange = current.overduePasses - previous.overduePasses;

        return new ReportSummary(
                current.totalPassSlips,
                current.currentlyOut,
                current.overduePasses,
                current.averageDurationMinutes,
                current.complianceRate,
                totalChangePercent,
                complianceChange,
                averageDurationChange,
                overdueChange
        );
    }

    /**
     * Gets how many pass slips were issued per department.
     *
     * The query joins pass_slip and employee because the pass_slip table stores
     * employee_id, while department is stored in the employee table.
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
                WHERE COALESCE(p.time_out, p.created_at) >= ?
                  AND COALESCE(p.time_out, p.created_at) < ?
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
     * Gets daily report data for the compliance and overdue monitoring table.
     */
    public List<DailyReport> getDailyReports(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    CAST(COALESCE(p.time_out, p.created_at) AS DATE) AS report_date,
                    COUNT(*) AS total_issued,
                    COUNT(*) FILTER (
                        WHERE p.status = 'RETURNED'
                          AND COALESCE(p.is_late, FALSE) = FALSE
                    ) AS returned_on_time,
                    COUNT(*) FILTER (
                        WHERE p.status = 'OVERDUE'
                           OR COALESCE(p.is_late, FALSE) = TRUE
                           OR (
                                p.status = 'OUT'
                                AND p.time_out IS NOT NULL
                                AND p.estimated_duration > 0
                                AND p.time_out + ((p.estimated_duration + ?) * INTERVAL '1 minute') < NOW()
                           )
                    ) AS overdue,
                    COALESCE(AVG(
                        CASE
                            WHEN p.actual_duration IS NOT NULL AND p.actual_duration > 0 THEN p.actual_duration
                            WHEN p.time_in IS NOT NULL AND p.time_out IS NOT NULL
                                THEN EXTRACT(EPOCH FROM (p.time_in - p.time_out)) / 60
                        END
                    ), 0) AS average_duration_minutes
                FROM pass_slip p
                WHERE COALESCE(p.time_out, p.created_at) >= ?
                  AND COALESCE(p.time_out, p.created_at) < ?
                GROUP BY CAST(COALESCE(p.time_out, p.created_at) AS DATE)
                ORDER BY report_date DESC
                """;

        List<DailyReport> dailyReports = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, GRACE_PERIOD_MINUTES);
            statement.setTimestamp(2, startTimestamp(startDate));
            statement.setTimestamp(3, endTimestampExclusive(endDate));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int totalIssued = rs.getInt("total_issued");
                    int returnedOnTime = rs.getInt("returned_on_time");
                    double complianceRate = totalIssued == 0 ? 0 : (returnedOnTime * 100.0) / totalIssued;

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
     * Gets weekly report data for the compliance and overdue monitoring table.
     *
     * This uses the same calculations as the daily report, but groups records by
     * the start date of each week. The controller reuses the DailyReport model so
     * the same TableView can display either daily or weekly summaries.
     */
    public List<DailyReport> getWeeklyReports(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    DATE_TRUNC('week', COALESCE(p.time_out, p.created_at))::DATE AS report_date,
                    COUNT(*) AS total_issued,
                    COUNT(*) FILTER (
                        WHERE p.status = 'RETURNED'
                          AND COALESCE(p.is_late, FALSE) = FALSE
                    ) AS returned_on_time,
                    COUNT(*) FILTER (
                        WHERE p.status = 'OVERDUE'
                           OR COALESCE(p.is_late, FALSE) = TRUE
                           OR (
                                p.status = 'OUT'
                                AND p.time_out IS NOT NULL
                                AND p.estimated_duration > 0
                                AND p.time_out + ((p.estimated_duration + ?) * INTERVAL '1 minute') < NOW()
                           )
                    ) AS overdue,
                    COALESCE(AVG(
                        CASE
                            WHEN p.actual_duration IS NOT NULL AND p.actual_duration > 0 THEN p.actual_duration
                            WHEN p.time_in IS NOT NULL AND p.time_out IS NOT NULL
                                THEN EXTRACT(EPOCH FROM (p.time_in - p.time_out)) / 60
                        END
                    ), 0) AS average_duration_minutes
                FROM pass_slip p
                WHERE COALESCE(p.time_out, p.created_at) >= ?
                  AND COALESCE(p.time_out, p.created_at) < ?
                GROUP BY DATE_TRUNC('week', COALESCE(p.time_out, p.created_at))::DATE
                ORDER BY report_date DESC
                """;

        List<DailyReport> weeklyReports = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, GRACE_PERIOD_MINUTES);
            statement.setTimestamp(2, startTimestamp(startDate));
            statement.setTimestamp(3, endTimestampExclusive(endDate));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int totalIssued = rs.getInt("total_issued");
                    int returnedOnTime = rs.getInt("returned_on_time");
                    double complianceRate = totalIssued == 0 ? 0 : (returnedOnTime * 100.0) / totalIssued;

                    Date sqlDate = rs.getDate("report_date");
                    LocalDate reportDate = sqlDate == null ? null : sqlDate.toLocalDate();

                    weeklyReports.add(new DailyReport(
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

        return weeklyReports;
    }

    /**
     * Gets monthly pass slip issuance data for the bar chart.
     *
     * The chart shows January to December for the selected year. This avoids
     * guessing Official/Personal values from the free-text reason field and
     * instead uses the existing pass_slip records directly.
     */
    public List<MonthlyTrend> getMonthlyTrends(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                WITH months AS (
                    SELECT
                        generate_series(1, 12) AS month_number
                ),
                monthly_counts AS (
                    SELECT
                        EXTRACT(MONTH FROM COALESCE(p.time_out, p.created_at))::INT AS month_number,
                        COUNT(*) AS issued_count
                    FROM pass_slip p
                    WHERE COALESCE(p.time_out, p.created_at) >= ?
                      AND COALESCE(p.time_out, p.created_at) < ?
                    GROUP BY EXTRACT(MONTH FROM COALESCE(p.time_out, p.created_at))::INT
                )
                SELECT
                    months.month_number,
                    TO_CHAR(TO_DATE(months.month_number::TEXT, 'MM'), 'FMMon') AS period,
                    COALESCE(monthly_counts.issued_count, 0) AS issued_count
                FROM months
                LEFT JOIN monthly_counts ON monthly_counts.month_number = months.month_number
                ORDER BY months.month_number
                """;

        List<MonthlyTrend> trends = new ArrayList<>();
        LocalDate yearStart = LocalDate.of(startDate.getYear(), 1, 1);
        LocalDate nextYearStart = yearStart.plusYears(1);

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, startTimestamp(yearStart));
            statement.setTimestamp(2, startTimestamp(nextYearStart));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    trends.add(new MonthlyTrend(
                            rs.getString("period"),
                            rs.getInt("issued_count")
                    ));
                }
            }
        }

        return trends;
    }

    /**
     * Runs the actual summary query for one date range.
     */
    private SummaryMetrics getSummaryMetrics(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                SELECT
                    COUNT(*) AS total_pass_slips,
                    COUNT(*) FILTER (
                        WHERE p.status IN ('OUT', 'OVERDUE')
                          AND p.time_in IS NULL
                    ) AS currently_out,
                    COUNT(*) FILTER (
                        WHERE p.status = 'OVERDUE'
                           OR COALESCE(p.is_late, FALSE) = TRUE
                           OR (
                                p.status = 'OUT'
                                AND p.time_out IS NOT NULL
                                AND p.estimated_duration > 0
                                AND p.time_out + ((p.estimated_duration + ?) * INTERVAL '1 minute') < NOW()
                           )
                    ) AS overdue_passes,
                    COALESCE(AVG(
                        CASE
                            WHEN p.actual_duration IS NOT NULL AND p.actual_duration > 0 THEN p.actual_duration
                            WHEN p.time_in IS NOT NULL AND p.time_out IS NOT NULL
                                THEN EXTRACT(EPOCH FROM (p.time_in - p.time_out)) / 60
                        END
                    ), 0) AS average_duration_minutes,
                    COUNT(*) FILTER (
                        WHERE p.status = 'RETURNED'
                          AND COALESCE(p.is_late, FALSE) = FALSE
                    ) AS returned_on_time
                FROM pass_slip p
                WHERE COALESCE(p.time_out, p.created_at) >= ?
                  AND COALESCE(p.time_out, p.created_at) < ?
                """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, GRACE_PERIOD_MINUTES);
            statement.setTimestamp(2, startTimestamp(startDate));
            statement.setTimestamp(3, endTimestampExclusive(endDate));

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_pass_slips");
                    int returnedOnTime = rs.getInt("returned_on_time");
                    double complianceRate = total == 0 ? 0 : (returnedOnTime * 100.0) / total;

                    return new SummaryMetrics(
                            total,
                            rs.getInt("currently_out"),
                            rs.getInt("overdue_passes"),
                            rs.getDouble("average_duration_minutes"),
                            complianceRate
                    );
                }
            }
        }

        return new SummaryMetrics(0, 0, 0, 0, 0);
    }

    /**
     * Calculates percentage change safely, including cases where previous value is zero.
     */
    private double percentChange(double currentValue, double previousValue) {
        if (previousValue == 0) {
            return currentValue == 0 ? 0 : 100;
        }

        return ((currentValue - previousValue) / previousValue) * 100.0;
    }

    /**
     * Converts a LocalDate into the start of that day.
     */
    private Timestamp startTimestamp(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    /**
     * Converts the selected end date into an exclusive timestamp.
     * Example: selecting 2026-05-24 includes all records until 2026-05-25 00:00:00.
     */
    private Timestamp endTimestampExclusive(LocalDate date) {
        return Timestamp.valueOf(date.plusDays(1).atStartOfDay());
    }

    /**
     * Small private container used only inside this repository.
     */
    private static class SummaryMetrics {
        private final int totalPassSlips;
        private final int currentlyOut;
        private final int overduePasses;
        private final double averageDurationMinutes;
        private final double complianceRate;

        private SummaryMetrics(int totalPassSlips,
                               int currentlyOut,
                               int overduePasses,
                               double averageDurationMinutes,
                               double complianceRate) {
            this.totalPassSlips = totalPassSlips;
            this.currentlyOut = currentlyOut;
            this.overduePasses = overduePasses;
            this.averageDurationMinutes = averageDurationMinutes;
            this.complianceRate = complianceRate;
        }
    }
}
