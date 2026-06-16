package oopsystem.model;

/**
 * Model class for the monthly pass slip issuance chart.
 *
 * Each object stores one month label and the number of pass slips issued
 * during that month.
 */
public class MonthlyTrend {
    private final String period;
    private final int issuedCount;

    public MonthlyTrend(String period, int issuedCount) {
        this.period = period;
        this.issuedCount = issuedCount;
    }

    // Month label displayed on the chart, for example: Jan, Feb, Mar.
    public String getPeriod() {
        return period;
    }

    // Total number of pass slips issued during the month.
    public int getIssuedCount() {
        return issuedCount;
    }
}
