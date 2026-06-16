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
    private String status;
    private final LocalDateTime createdAt;
    private final int estimatedDuration;  // was: duration
    private final int actualDuration;
    private final boolean is_late;

    public MovementLog(
            int passSlipId,
            String employeeName,
            String department,
            String reason,
            String destination,
            LocalDateTime timeOut,
            LocalDateTime timeIn,
            int estimatedDuration,
            int actualDuration,
            String status,
            boolean is_late,
            LocalDateTime createdAt) {


        this.passSlipId = passSlipId;
        this.employeeName = employeeName;
        this.department = department;
        this.reason = reason;
        this.destination = destination;
        this.timeOut = timeOut;
        this.timeIn = timeIn;
        this.status = status;
        this.is_late = is_late;
        this.estimatedDuration = estimatedDuration;
        this.actualDuration = actualDuration;
        this.createdAt = createdAt;

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

    public int getEstimatedDuration() {
        return estimatedDuration;
    }

    public int getActualDuration() {
        return actualDuration;
    }

    public String getPassStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isLate() {
        return is_late;
    }
}
