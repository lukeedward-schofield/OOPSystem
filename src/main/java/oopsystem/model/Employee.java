package oopsystem.model;

import javafx.beans.property.*;

public class Employee {

    private final IntegerProperty employeeId;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty department;
    private final StringProperty role;
    private final StringProperty contactNumber;
    private final StringProperty emailAddress;
    private final BooleanProperty activeStatus;

    public Employee(
            int employeeId,
            String firstName,
            String lastName,
            String department,
            String role,
            String contactNumber,
            String emailAddress,
            boolean activeStatus) {

        this.employeeId = new SimpleIntegerProperty(employeeId);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.department = new SimpleStringProperty(department);
        this.role = new SimpleStringProperty(role);
        this.contactNumber = new SimpleStringProperty(contactNumber);
        this.emailAddress = new SimpleStringProperty(emailAddress);
        this.activeStatus = new SimpleBooleanProperty(activeStatus);
    }

    public int getEmployeeId() {
        return employeeId.get();
    }

    public String getFirstName() {
        return firstName.get();
    }

    public String getLastName() {
        return lastName.get();
    }

    public String getDepartment() {
        return department.get();
    }

    public String getRole() {
        return role.get();
    }

    public String getContactNumber() {
        return contactNumber.get();
    }

    public String getEmailAddress() {
        return emailAddress.get();
    }

    public boolean isActiveStatus() {
        return activeStatus.get();
    }

    @Override
    public String toString() {
        return firstName.get() + " " + lastName.get();
    }
}