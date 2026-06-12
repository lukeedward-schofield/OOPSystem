package oopsystem.model;

import java.sql.Timestamp;

public class User {
    private int userId;
    private String username;
    private String userPassword;
    private String firstName;
    private String lastName;
    private boolean activeStatus;
    private Timestamp createdAt;
    private int employeeId; // Your foreign key link!

    // Constructor
    public User(int userId, String username, String userPassword, String firstName,
                String lastName, boolean activeStatus, Timestamp createdAt, int employeeId) {
        this.userId = userId;
        this.username = username;
        this.userPassword = userPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.activeStatus = activeStatus;
        this.createdAt = createdAt;
        this.employeeId = employeeId;
    }

    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUserPassword() { return userPassword; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isActiveStatus() { return activeStatus; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getEmployeeId() { return employeeId; }

    // Setters (For fields you want to update inside your app)
    public void setUsername(String username) { this.username = username; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setActiveStatus(boolean activeStatus) { this.activeStatus = activeStatus; }
}
