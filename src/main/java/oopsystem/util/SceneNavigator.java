package oopsystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {

    private static Stage stage;

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchTo(String fxmlName) {
        try {
            String path = "/oopsystem/" + fxmlName + ".fxml";

            FXMLLoader loader = new FXMLLoader(
                    SceneNavigator.class.getResource(path)
            );

            if (loader.getLocation() == null) {
                throw new RuntimeException("FXML NOT FOUND: " + path);
            }

            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}