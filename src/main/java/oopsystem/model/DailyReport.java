package oopsystem.model;

import java.time.LocalDate;

/**
 * Model class for daily report rows.
 *
 * This is used by the Daily Compliance and Overdue Monitoring table in JavaFX.
 */
public class DailyReport {
    private final LocalDate reportDate;
    private final int totalIssued;
    private final int returnedOnTime;
    private final int overdue;
    private final double averageDurationMinutes;
    private final double complianceRate;

    public DailyReport(LocalDate reportDate,
                       int totalIssued,
                       int returnedOnTime,
                       int overdue,
                       double averageDurationMinutes,
                       double complianceRate) {
        this.reportDate = reportDate;
        this.totalIssued = totalIssued;
        this.returnedOnTime = returnedOnTime;
        this.overdue = overdue;
        this.averageDurationMinutes = averageDurationMinutes;
        this.complianceRate = complianceRate;
    }

    // Date being summarized.
    public LocalDate getReportDate() {
        return reportDate;
    }

    // Total pass slips issued on this date.
    public int getTotalIssued() {
        return totalIssued;
    }

    // Number of pass slips that were returned within the allowed/default duration.
    public int getReturnedOnTime() {
        return returnedOnTime;
    }

    // Number of pass slips that exceeded the allowed/default duration.
    public int getOverdue() {
        return overdue;
    }

    // Average duration for returned pass slips on this date, in minutes.
    public double getAverageDurationMinutes() {
        return averageDurationMinutes;
    }

    // Returned-on-time percentage for this date.
    public double getComplianceRate() {
        return complianceRate;
    }
}
