package oopsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import oopsystem.util.SceneNavigator;

public class NavbarController {

    public Button dashboardMenu;
    public Button passSlipMenu;
    public Button movementLogsMenu;
    public Button employeeDirectoryMenu;
    public Button reportsMenu;

    //    @FXML public void goToDashboard(){SceneNavigator.switchTo("dashboard/ashboardView");}
    @FXML public void goToPassSlipIssuance(){SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView");}
    //    @FXML public void goToMovementLogs(){SceneNavigator.switchTo("movementLogs/MovementLogsView");}
    @FXML public void goToEmployeeDirectory(){SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");}
    @FXML public void gotoReports(){SceneNavigator.switchTo("reports/ReportsView");}
}
