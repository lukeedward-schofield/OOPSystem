package oopsystem.model;

import java.time.LocalDateTime;

public class MovementLog {

    private final int passSlipId;
    private final String employeeName;
    private final String department;
    private final String reason;
    private final String destination;
    private final LocalDateTime timeOut;
    private final LocalDateTime timeIn;
    private final int duration;
    private  String status;
    private final LocalDateTime createdAt;

    public MovementLog(
            int passSlipId,
            String employeeName,
            String department,
            String reason,
            String destination,
            LocalDateTime timeOut,
            LocalDateTime timeIn,
            int duration,
            String status,
            LocalDateTime createdAt){

        this.passSlipId = passSlipId;
        this.employeeName = employeeName;
        this.department = department;
        this.reason = reason;
        this.destination = destination;
        this.timeOut = timeOut;
        this.timeIn = timeIn;
        this.duration = duration;
        this.status = status;
        this.createdAt = createdAt;

        resolveOverdue();
    }

    private void resolveOverdue() {
        if ("OUT".equals(status) && timeOut != null && duration > 0) {
            LocalDateTime deadline = timeOut.plusMinutes(duration + 3);
            if (LocalDateTime.now().isAfter(deadline)) {
                this.status = "OVERDUE";
            }
        }
    }

    public int getPassSlipId() {
        return passSlipId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDepartment() {
        return department;
    }

    public String getReason() {
        return reason;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    public LocalDateTime getTimeIn() {
        return timeIn;
    }

    public int getDuration() {
        return duration;
    }

    public String getPassStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
