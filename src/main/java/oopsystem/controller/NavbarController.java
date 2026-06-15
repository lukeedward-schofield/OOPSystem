package oopsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

public class NavbarController implements Initializable {

    @FXML public Button dashboardMenu;
    @FXML public Button passSlipMenu;
    @FXML public Button movementLogsMenu;
    @FXML public Button employeeDirectoryMenu;
    @FXML public Button reportsMenu;
    @FXML public Button profileMenu;
    @FXML public Button logoutBtn;

    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dashboardMenu.setOnAction(e -> SceneNavigator.switchTo("dashboard/DashboardView"));
        passSlipMenu.setOnAction(e -> SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView"));
        movementLogsMenu.setOnAction(e -> SceneNavigator.switchTo("movementLogs/MovementLogsView"));
        employeeDirectoryMenu.setOnAction(e -> SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView"));
        reportsMenu.setOnAction(e -> SceneNavigator.switchTo("reports/ReportsView"));
        profileMenu.setOnAction(e -> SceneNavigator.switchTo("profile/ProfileView"));

        logoutBtn.setOnAction(e -> handleLogout());
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                activityLogRepository.log("LOGOUT", "User " + SessionManager.getLoggedInUsername() + " logged out");
                SessionManager.clearSession();
                SceneNavigator.switchTo("login/LoginView");
            }
        });
    }
}