package oopsystem.model;

/**
 * Model class for department-based pass slip usage.
 *
 * Each object represents one row in the Department Usage table.
 */
public class DepartmentUsage {
    private final String department;
    private final int totalSlips;
    private final double percentage;

    public DepartmentUsage(String department, int totalSlips, double percentage) {
        this.department = department;
        this.totalSlips = totalSlips;
        this.percentage = percentage;
    }

    // Department name from the employee table.
    public String getDepartment() {
        return department;
    }

    // Number of pass slips issued by employees under this department.
    public int getTotalSlips() {
        return totalSlips;
    }

    // Department's share of the total pass slips for the selected date range.
    public double getPercentage() {
        return percentage;
    }
}
