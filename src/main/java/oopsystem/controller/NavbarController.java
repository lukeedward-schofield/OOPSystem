package oopsystem.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
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
        navLoadingLabel.setText("Loading " + label + "...");
        navLoadingOverlay.setVisible(true);
        navLoadingOverlay.setManaged(true);

        setButtonsDisabled(true);

        new Thread(() -> {
            try { Thread.sleep(120); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                SceneNavigator.switchTo(view);
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

        if (page.contains("dashboard"))                                        return dashboardMenu;
        if (page.contains("passslipissuance") || page.contains("passslip"))   return passSlipMenu;
        if (page.contains("movementlogs")     || page.contains("movementlog")) return movementLogsMenu;
        if (page.contains("employeedirectory") || page.contains("addemployee")) return employeeDirectoryMenu;
        if (page.contains("reports"))                                          return reportsMenu;
        if (page.contains("profile")          || page.contains("settings"))   return profileMenu;

        return null;
    }

    private void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/oopsystem/components/LogoutScreen.fxml")
            );
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}