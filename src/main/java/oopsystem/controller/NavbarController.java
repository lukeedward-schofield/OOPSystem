package oopsystem.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NavbarController implements Initializable {

    @FXML public Button dashboardMenu;
    @FXML public Button passSlipMenu;
    @FXML public Button movementLogsMenu;
    @FXML public Button employeeDirectoryMenu;
    @FXML public Button reportsMenu;
    @FXML public Button profileMenu;
    @FXML public Button logoutBtn;

    /* LOADING */
    @FXML private VBox              navLoadingOverlay;
    @FXML private ProgressIndicator navSpinner;
    @FXML private Label             navLoadingLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dashboardMenu.setOnAction(e         -> navigateTo("dashboard/DashboardView",                  "Dashboard"));
        passSlipMenu.setOnAction(e          -> navigateTo("passSlipIssuance/PassSlipIssuanceView",    "Issue Pass Slip"));
        movementLogsMenu.setOnAction(e      -> navigateTo("movementLogs/MovementLogsView",            "Movement Logs"));
        employeeDirectoryMenu.setOnAction(e -> navigateTo("employeeDirectory/EmployeeDirectoryView",  "Employee Directory"));
        reportsMenu.setOnAction(e           -> navigateTo("reports/ReportsView",                      "Reports"));
        profileMenu.setOnAction(e           -> navigateTo("profile/ProfileView",                      "Settings"));
        logoutBtn.setOnAction(e             -> handleLogout());

        setActiveMenu(SceneNavigator.getCurrentPage());
    }

    private void navigateTo(String view, String label) {
        // show overlay
        navLoadingLabel.setText("Loading " + label + "...");
        navLoadingOverlay.setVisible(true);
        navLoadingOverlay.setManaged(true);

        // disable all buttons so user can't double-click
        setButtonsDisabled(true);

        // short delay so the spinner renders before the scene switch
        new Thread(() -> {
            try { Thread.sleep(120); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                SceneNavigator.switchTo(view);
                // reset in case same controller reuses this nav instance
                navLoadingOverlay.setVisible(false);
                navLoadingOverlay.setManaged(false);
                setButtonsDisabled(false);
            });
        }, "nav-loader").start();
    }

    private void setButtonsDisabled(boolean disabled) {
        List<Button> all = List.of(
                dashboardMenu, passSlipMenu, movementLogsMenu,
                employeeDirectoryMenu, reportsMenu, profileMenu, logoutBtn
        );
        all.forEach(b -> b.setDisable(disabled));
    }

    private void setActiveMenu(String currentPage) {
        List<Button> buttons = List.of(
                dashboardMenu, passSlipMenu, movementLogsMenu,
                employeeDirectoryMenu, reportsMenu, profileMenu
        );
        buttons.forEach(b -> b.getStyleClass().remove("active-menu"));

        Button activeButton = resolveActiveButton(currentPage);
        if (activeButton != null && !activeButton.getStyleClass().contains("active-menu")) {
            activeButton.getStyleClass().add("active-menu");
        }
    }

    private Button resolveActiveButton(String currentPage) {
        if (currentPage == null || currentPage.isBlank()) return null;

        String page = currentPage.toLowerCase();

        if (page.contains("dashboard"))                                     return dashboardMenu;
        if (page.contains("passslipissuance") || page.contains("passslip")) return passSlipMenu;
        if (page.contains("movementlogs")     || page.contains("movementlog")) return movementLogsMenu;
        if (page.contains("employeedirectory") || page.contains("addemployee")) return employeeDirectoryMenu;
        if (page.contains("reports"))                                       return reportsMenu;
        if (page.contains("profile")          || page.contains("settings")) return profileMenu;

        return null;
    }

    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                SessionManager.clearSession();
                SceneNavigator.switchTo("login/LoginView");
            }
        });
    }
}