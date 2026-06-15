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

    // Extra fields fetched via JOIN for your TableView
    private String department;
    private String role;

    // Full constructor for loading data
    public User(){

    }

    public User(int userId, String username, String userPassword, String firstName, String lastName,
                boolean activeStatus, Timestamp createdAt, int employeeId, String department, String role) {
        this.userId = userId;
        this.username = username;
        this.userPassword = userPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.activeStatus = activeStatus;
        this.createdAt = createdAt;
        this.employeeId = employeeId;
        this.department = department;
        this.role = role;
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
    public String getDepartment() { return department; }
    public String getRole() { return role; }

    // Setters (For fields you want to update inside your app)
    public void setUserId(int userId){this.userId = userId;}
    public void setUserPassword(String userPassword){this.userPassword = userPassword;}
    public void setUsername(String username){ this.username = username; }
    public void setFirstName(String firstName){ this.firstName = firstName; }
    public void setLastName(String lastName){ this.lastName = lastName; }
    public void setActiveStatus(boolean activeStatus) { this.activeStatus = activeStatus; }
    public void setCreatedAt(Timestamp createdAt){this.createdAt = createdAt;}
    public void setEmployeeId(int employeeId) {this.employeeId = employeeId;}
    public void setDepartment(String department){ this.department = department; }
    public void setRole(String role) { this.role = role; }

}
