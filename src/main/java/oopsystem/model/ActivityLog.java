package oopsystem.model;

import java.time.ZonedDateTime;

public class ActivityLog {

    private int logId;
    private int userId;
    private String action;
    private String logInDetails;
    private ZonedDateTime createdAt;
    private String username;

    public ActivityLog(int logId, int userId, String action, String logInDetails, ZonedDateTime createdAt, String username) {
        this.logId = logId;
        this.userId = userId;
        this.action = action;
        this.logInDetails = logInDetails;
        this.createdAt = createdAt;
        this.username = username;
    }

    public int getLogId() { return logId; }
    public int getUserId() { return userId; }
    public String getAction() { return action; }
    public String getLogInDetails() { return logInDetails; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public String getUsername() { return username; }
}