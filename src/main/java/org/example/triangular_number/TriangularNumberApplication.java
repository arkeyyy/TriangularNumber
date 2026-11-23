package org.example.triangular_number;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TriangularNumberApplication extends Application {
    private TriangularController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("triangular-view.fxml"));
        Scene scene = new Scene(loader.load(), 760, 640);
        controller = loader.getController();
        stage.setTitle("Triangular Number");
        stage.setMinWidth(460);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }
}
