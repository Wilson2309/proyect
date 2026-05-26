package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {

    private static final String LOGIN_FXML = "/org/example/views/LoginView.fxml";
    private static final double LOGIN_WIDTH = 480;
    private static final double LOGIN_HEIGHT = 640;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        getClass().getResource(LOGIN_FXML),
                        "No se encontró LoginView.fxml en src/main/resources/org/example/views/"
                )
        );

        Scene scene = new Scene(loader.load(), LOGIN_WIDTH, LOGIN_HEIGHT);

        stage.setTitle("OptiScan Pro");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

