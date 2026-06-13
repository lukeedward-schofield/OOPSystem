package oopsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.util.ResourceBundle;

public class NavbarController implements Initializable {

    @FXML public Button dashboardMenu;
    @FXML public Button passSlipMenu;
    @FXML public Button movementLogsMenu;
    @FXML public Button employeeDirectoryMenu;
    @FXML public Button reportsMenu;
    @FXML public Button profileMenu;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dashboardMenu.setOnAction(e -> SceneNavigator.switchTo("dashboard/DashboardView"));
        passSlipMenu.setOnAction(e -> SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView"));
        movementLogsMenu.setOnAction(e -> SceneNavigator.switchTo("movementLogs/MovementLogsView"));
        employeeDirectoryMenu.setOnAction(e -> SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView"));
        reportsMenu.setOnAction(e -> SceneNavigator.switchTo("reports/ReportsView"));
        profileMenu.setOnAction(e -> SceneNavigator.switchTo("profile/ProfileView"));
    }
}