package oopsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {

    private static Stage stage;
    private static String currentPage;

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    public static String getCurrentPage() {
        return currentPage;
    }

    public static void switchTo(String fxmlName) {
        try {
            if (stage == null) {
                throw new IllegalStateException("Stage has not been set in SceneNavigator.");
            }

            FXMLLoader loader = createLoader(fxmlName);
            currentPage = normalizePageName(fxmlName);

            Parent root = loader.load();
            Scene currentScene = stage.getScene();

            if (currentScene == null) {
                stage.setScene(new Scene(root));
            } else {
                currentScene.setRoot(root);
            }

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void switchToMaximized(String fxmlName) {
        switchTo(fxmlName);
        if (stage != null) {
            stage.setMaximized(true);
            stage.show();
        }
    }

    private static FXMLLoader createLoader(String fxmlName) {
        String normalized = normalizePageName(fxmlName);

        String[] paths = {
                "/oopsystem/" + normalized + ".fxml",
                "/oopsystem/dashboard/" + normalized + ".fxml",
                "/oopsystem/employeeDirectory/" + normalized + ".fxml",
                "/oopsystem/passSlipIssuance/" + normalized + ".fxml",
                "/oopsystem/movementLogs/" + normalized + ".fxml",
                "/oopsystem/reports/" + normalized + ".fxml",
                "/oopsystem/login/" + normalized + ".fxml",
                "/oopsystem/profile/" + normalized + ".fxml"
        };

        for (String path : paths) {
            var url = SceneNavigator.class.getResource(path);
            if (url != null) {
                return new FXMLLoader(url);
            }
        }

        throw new RuntimeException("FXML NOT FOUND: " + fxmlName);
    }

    private static String normalizePageName(String fxmlName) {
        String normalized = fxmlName;

        if (normalized.startsWith("/oopsystem/")) {
            normalized = normalized.substring("/oopsystem/".length());
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".fxml")) {
            normalized = normalized.substring(0, normalized.length() - ".fxml".length());
        }

        return normalized;
    }
}
