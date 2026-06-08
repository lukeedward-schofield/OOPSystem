package oopsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {

    private static Stage stage;

    // Called once in Main/EmployeeApplication to register the primary stage
    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchTo(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneNavigator.class.getResource("/oopsystem/" + fxmlName + ".fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlName);
            e.printStackTrace();
        }
    }

    // Use this if you need to access the controller after loading
    public static <T> T switchToAndGetController(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneNavigator.class.getResource("/oopsystem/view/" + fxmlName + ".fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            return loader.getController();
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlName);
            e.printStackTrace();
            return null;
        }
    }

    public static Stage getStage() {
        return stage;
    }
}
