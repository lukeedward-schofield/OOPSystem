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

    public ReportSummary(int totalPassSlips,
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

    // Total number of pass slips issued within the selected date range.
    public int getTotalPassSlips() {
        return totalPassSlips;
    }

    // Number of employees with no recorded time-in yet.
    public int getCurrentlyOut() {
        return currentlyOut;
    }

    // Number of open pass slips that already exceeded the allowed/default duration.
    public int getOverduePasses() {
        return overduePasses;
    }

    // Average time spent outside, stored in minutes for easier calculations.
    public double getAverageDurationMinutes() {
        return averageDurationMinutes;
    }

    // Percentage of pass slips returned within the allowed/default duration.
    public double getComplianceRate() {
        return complianceRate;
    }
}
