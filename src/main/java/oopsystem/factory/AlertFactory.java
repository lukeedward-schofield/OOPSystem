package oopsystem.factory;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import oopsystem.model.Employee;

import java.util.Optional;

public class AlertFactory {

    public static Optional<ButtonType> showDeleteConfirmation(Employee selectedEmployee){
        // 1. Show a confirmation popup alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Employee");
        alert.setHeaderText("Are you sure you want to delete this employee?");
        alert.setContentText("Employee ID: " + selectedEmployee.getEmployeeId() + "\nThis action cannot be undone.");

        return alert.showAndWait();
    }

    public static void employeeDeletionSuccess(Employee selectedEmployee){
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Employee Deleted");
        successAlert.setHeaderText("Employee record deleted successfully.");
        successAlert.setContentText(selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName()
                + " has been removed from the employee directory.");
        successAlert.showAndWait();
    }

    public static void employeeDeletionDatabaseError(){
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Database Error");
        errorAlert.setHeaderText("Deletion Failed");
        errorAlert.setContentText("Could not drop employee record from the database.");
        errorAlert.showAndWait();
    }
}
