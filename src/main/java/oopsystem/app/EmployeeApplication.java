package oopsystem.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import oopsystem.util.SceneNavigator;

import java.io.IOException;

public class EmployeeApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        SceneNavigator.setStage(stage);

        FXMLLoader fxmlLoader = new FXMLLoader(
                EmployeeApplication.class.getResource("/oopsystem/login/LoginView.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
