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
            String[] paths = {
                    "/oopsystem/dashboard/" + fxmlName + ".fxml",
                    "/oopsystem/employeeDirectory/" + fxmlName + ".fxml",
                    "/oopsystem/passSlipIssuance/" + fxmlName + ".fxml",
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
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}