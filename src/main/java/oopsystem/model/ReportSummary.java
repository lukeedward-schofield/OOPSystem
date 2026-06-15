package oopsystem.model;

/**
 * Model class for the summary cards in the Reports and Analytics screen.
 *
 * This object only stores calculated report values. It does not connect to the
 * database directly. The repository calculates these values from the database,
 * creates a ReportSummary object, then sends it to the controller.
 */
public class ReportSummary {
    private final int totalPassSlips;
    private final int currentlyOut;
    private final int overduePasses;
    private final double averageDurationMinutes;
    private final double complianceRate;

    // These values are used for the small + / - indicators beside each card.
    // They are calculated by comparing the selected date range with the previous
    // date range of the same length, so the UI is no longer hardcoded.
    private final double totalPassSlipsChangePercent;
    private final double complianceRateChange;
    private final double averageDurationChangeMinutes;
    private final int overduePassesChange;

    public ReportSummary(int totalPassSlips,
                         int currentlyOut,
                         int overduePasses,
                         double averageDurationMinutes,
                         double complianceRate) {
        this(totalPassSlips, currentlyOut, overduePasses, averageDurationMinutes, complianceRate,
                0, 0, 0, 0);
    }

    public ReportSummary(int totalPassSlips,
                         int currentlyOut,
                         int overduePasses,
                         double averageDurationMinutes,
                         double complianceRate,
                         double totalPassSlipsChangePercent,
                         double complianceRateChange,
                         double averageDurationChangeMinutes,
                         int overduePassesChange) {
        this.totalPassSlips = totalPassSlips;
        this.currentlyOut = currentlyOut;
        this.overduePasses = overduePasses;
        this.averageDurationMinutes = averageDurationMinutes;
        this.complianceRate = complianceRate;
        this.totalPassSlipsChangePercent = totalPassSlipsChangePercent;
        this.complianceRateChange = complianceRateChange;
        this.averageDurationChangeMinutes = averageDurationChangeMinutes;
        this.overduePassesChange = overduePassesChange;
    }

    // Total number of pass slips issued within the selected date range.
    public int getTotalPassSlips() {
        return totalPassSlips;
    }

    // Number of employees with an open or overdue pass slip.
    public int getCurrentlyOut() {
        return currentlyOut;
    }

    // Number of pass slips marked as overdue or late.
    public int getOverduePasses() {
        return overduePasses;
    }

    // Average time spent outside, stored in minutes for easier calculations.
    public double getAverageDurationMinutes() {
        return averageDurationMinutes;
    }

    // Percentage of pass slips returned on time.
    public double getComplianceRate() {
        return complianceRate;
    }

    // Percent difference of total pass slips versus the previous period.
    public double getTotalPassSlipsChangePercent() {
        return totalPassSlipsChangePercent;
    }

    // Percentage-point difference of compliance rate versus the previous period.
    public double getComplianceRateChange() {
        return complianceRateChange;
    }

    // Difference in average duration, in minutes, versus the previous period.
    public double getAverageDurationChangeMinutes() {
        return averageDurationChangeMinutes;
    }

    // Difference of overdue pass count versus the previous period.
    public int getOverduePassesChange() {
        return overduePassesChange;
    }
}
