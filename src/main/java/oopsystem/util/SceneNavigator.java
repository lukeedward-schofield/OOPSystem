package oopsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Handles switching between JavaFX pages in the system.
 *
 * Important sizing fix:
 * Instead of creating a brand-new Scene every time a page changes, this class
 * reuses the existing Scene and only replaces its root. This preserves the
 * current window size, maximized state, and full-screen state while navigating.
 */
public class SceneNavigator {

    private static Stage stage;

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    /**
     * Normal page navigation. This preserves whatever size/state the user is
     * already using, including maximized/full-screen mode.
     */
    public static void switchTo(String fxmlName) {
        switchToInternal(fxmlName, false);
    }

    /**
     * Used after login. The login window starts small, but the main dashboard
     * should open maximized instead of staying at the login window size.
     */
    public static void switchToMaximized(String fxmlName) {
        switchToInternal(fxmlName, true);
    }

    private static void switchToInternal(String fxmlName, boolean maximizeAfterLoad) {
        try {
            if (stage == null) {
                throw new IllegalStateException("SceneNavigator stage was not initialized. Call SceneNavigator.setStage(stage) first.");
            }

            String[] paths = {
                    "/oopsystem/dashboard/" + fxmlName + ".fxml",
                    "/oopsystem/employeeDirectory/" + fxmlName + ".fxml",
                    "/oopsystem/passSlipIssuance/" + fxmlName + ".fxml",
                    "/oopsystem/movementLogs/" + fxmlName + ".fxml",
                    "/oopsystem/reports/" + fxmlName + ".fxml",
                    "/oopsystem/login/" + fxmlName + ".fxml",
                    "/oopsystem/profile/" + fxmlName + ".fxml",
                    "/oopsystem/" + fxmlName + ".fxml"
            };

            FXMLLoader loader = null;
            for (String path : paths) {
                var url = SceneNavigator.class.getResource(path);
                if (url != null) {
                    loader = new FXMLLoader(url);
                    break;
                }
            }

            if (loader == null) {
                throw new RuntimeException("FXML NOT FOUND: " + fxmlName);
            }

            Parent root = loader.load();

            // Save the current window state before changing pages.
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            Scene currentScene = stage.getScene();

            if (currentScene == null) {
                // First load only. Use a reasonable default size.
                stage.setScene(new Scene(root, 1200, 760));
            } else {
                // Main fix: keep the same Scene so the Stage does not resize.
                currentScene.setRoot(root);

                // If the user is not maximized/full-screen, preserve the exact window size.
                if (!wasMaximized && !wasFullScreen && !maximizeAfterLoad) {
                    stage.setWidth(currentWidth);
                    stage.setHeight(currentHeight);
                }
            }

            if (maximizeAfterLoad) {
                stage.setFullScreen(false);
                stage.setMaximized(true);
            } else {
                // Restore maximized/full-screen states after normal navigation.
                stage.setMaximized(wasMaximized);
                stage.setFullScreen(wasFullScreen);
            }

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
