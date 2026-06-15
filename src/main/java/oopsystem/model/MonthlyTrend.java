package oopsystem.model;

/**
 * Model class for the monthly movement trend chart.
 *
 * The Figma/PDF report chart compares Official and Personal movement counts,
 * so each object stores one week label and the number of pass slips that fall
 * under each movement type.
 */
public class MonthlyTrend {
    private final String period;
    private final int officialCount;
    private final int personalCount;

    public MonthlyTrend(String period, int officialCount, int personalCount) {
        this.period = period;
        this.officialCount = officialCount;
        this.personalCount = personalCount;
    }

    // Week label displayed on the chart, for example: WK1.
    public String getPeriod() {
        return period;
    }

    // Pass slips whose reason is official or not marked as personal.
    public int getOfficialCount() {
        return officialCount;
    }

    // Pass slips whose reason contains "personal".
    public int getPersonalCount() {
        return personalCount;
    }
}
