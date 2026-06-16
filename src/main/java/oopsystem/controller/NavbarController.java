package oopsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import oopsystem.util.SceneNavigator;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class NavbarController implements Initializable {

    @FXML private Button dashboardMenu;
    @FXML private Button passSlipMenu;
    @FXML private Button movementLogsMenu;
    @FXML private Button employeeDirectoryMenu;
    @FXML private Button reportsMenu;
    @FXML private Button profileMenu;
    @FXML private Button logoutBtn;

    private List<Button> menuButtons;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        menuButtons = Arrays.asList(
                dashboardMenu, passSlipMenu, movementLogsMenu,
                employeeDirectoryMenu, reportsMenu, profileMenu
        );

        dashboardMenu.setOnAction(e -> handleNavigation("DashboardView"));
        passSlipMenu.setOnAction(e -> handleNavigation("PassSlipIssuanceView"));
        movementLogsMenu.setOnAction(e -> handleNavigation("MovementLogsView"));
        employeeDirectoryMenu.setOnAction(e -> handleNavigation("EmployeeDirectoryView"));
        reportsMenu.setOnAction(e -> handleNavigation("ReportsView"));
        profileMenu.setOnAction(e -> handleNavigation("SettingsView"));
        logoutBtn.setOnAction(e -> SceneNavigator.switchTo("LoginView"));

        highlightCurrentPage();
    }

    private void handleNavigation(String viewName) {
        SceneNavigator.switchTo(viewName);

    }

    private void highlightCurrentPage() {
        String page = SceneNavigator.getCurrentPage();
        Button target = switch (page) {
            case "DashboardView"         -> dashboardMenu;
            case "PassSlipIssuanceView"  -> passSlipMenu;
            case "MovementLogsView"      -> movementLogsMenu;
            case "EmployeeDirectoryView" -> employeeDirectoryMenu;
            case "ReportsView"           -> reportsMenu;
            case "SettingsView"          -> profileMenu;
            default                      -> dashboardMenu;
        };

        for (Button btn : menuButtons) {
            if (btn != null) btn.getStyleClass().remove("active-menu");
        }
        if (target != null && !target.getStyleClass().contains("active-menu")) {
            target.getStyleClass().add("active-menu");
        }
    }


    public void setActiveMenu(String menuKey) {
        Button target = switch (menuKey) {
            case "dashboard"         -> dashboardMenu;
            case "passSlip"          -> passSlipMenu;
            case "movementLogs"      -> movementLogsMenu;
            case "employeeDirectory" -> employeeDirectoryMenu;
            case "reports"           -> reportsMenu;
            case "profile"           -> profileMenu;
            default                  -> null;
        };

        for (Button btn : menuButtons) {
            if (btn != null) btn.getStyleClass().remove("active-menu");
        }
        if (target != null && !target.getStyleClass().contains("active-menu")) {
            target.getStyleClass().add("active-menu");
        }
    }
}