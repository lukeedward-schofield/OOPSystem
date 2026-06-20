package oopsystem.components;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import oopsystem.util.SceneNavigator;
import oopsystem.util.SessionManager;

public class LogoutDialogController {

    @FXML private Button cancelBtn;
    @FXML private Button logoutBtn;

    @FXML
    private void handleCancel() {
        ((Stage) cancelBtn.getScene().getWindow()).close();
    }

    @FXML
    private void handleLogout() {
        ((Stage) logoutBtn.getScene().getWindow()).close();
        SessionManager.clearSession();
        SceneNavigator.switchTo("login/LoginView");
    }
}