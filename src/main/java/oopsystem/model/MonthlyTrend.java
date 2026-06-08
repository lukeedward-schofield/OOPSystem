package oopsystem.model;

/**
 * Model class for the monthly movement trend chart.
 *
 * Each object represents one month and the number of issued, returned,
 * and overdue pass slips for that month.
 */
public class MonthlyTrend {
    private final String period;
    private final int totalIssued;
    private final int returned;
    private final int overdue;

    public MonthlyTrend(String period, int totalIssued, int returned, int overdue) {
        this.period = period;
        this.totalIssued = totalIssued;
        this.returned = returned;
        this.overdue = overdue;
    }

    // Month label displayed on the chart, for example: 2026-05.
    public String getPeriod() {
        return period;
    }

    // Total pass slips issued during the month.
    public int getTotalIssued() {
        return totalIssued;
    }

    // Number of issued pass slips with a recorded time-in.
    public int getReturned() {
        return returned;
    }

    // Number of pass slips considered overdue during the month.
    public int getOverdue() {
        return overdue;
    }
}
