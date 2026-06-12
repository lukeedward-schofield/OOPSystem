package oopsystem.controller;

import javafx.fxml.FXML;
import oopsystem.util.SceneNavigator;

public class PassSlipIssuanceController {


    //NAVIGATION METHODS
//    @FXML public void goToDashboard(){SceneNavigator.switchTo("DashboardView");}
    @FXML
    public void goToPassSlipIssuance(){
        SceneNavigator.switchTo("passSlipIssuance/PassSlipIssuanceView");}
    //    @FXML public void goToMovementLogs(){SceneNavigator.switchTo("movementLogs/MovementLogsView");}
    @FXML public void goToEmployeeDirectory(){SceneNavigator.switchTo("employeeDirectory/EmployeeDirectoryView");}
    @FXML public void gotoReports(){SceneNavigator.switchTo("reports/ReportsView");}
}
