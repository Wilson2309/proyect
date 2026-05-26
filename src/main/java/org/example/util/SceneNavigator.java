package org.example.util;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.example.services.SettingsService;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class SceneNavigator {

    private static final Duration FADE_OUT = Duration.millis(220);
    private static final Duration FADE_IN = Duration.millis(320);

    private SceneNavigator() {
    }

    public static void transition(
            Stage stage,
            Parent currentRoot,
            Class<?> resourceOwner,
            String fxmlPath,
            double width,
            double height
    ) {
        boolean animate = SettingsService.getInstance().isAnimacionesActivas();

        if (animate) {
            FadeTransition fadeOut = new FadeTransition(FADE_OUT, currentRoot);
            fadeOut.setFromValue(currentRoot.getOpacity() > 0 ? currentRoot.getOpacity() : 1);
            fadeOut.setToValue(0);

            fadeOut.setOnFinished(event -> loadAndFadeIn(stage, resourceOwner, fxmlPath, width, height, true));
            fadeOut.play();
        } else {
            loadAndFadeIn(stage, resourceOwner, fxmlPath, width, height, false);
        }
    }

    private static void loadAndFadeIn(
            Stage stage,
            Class<?> resourceOwner,
            String fxmlPath,
            double width,
            double height,
            boolean animate
    ) {
        try {
            URL location = Objects.requireNonNull(
                    resourceOwner.getResource(fxmlPath),
                    "FXML no encontrado: " + fxmlPath
            );

            FXMLLoader loader = new FXMLLoader(location);
            Parent root = loader.load();
            root.setOpacity(animate ? 0 : 1);

            Scene scene = new Scene(root, width, height);
            stage.setTitle("OptiScan Pro");
            stage.setScene(scene);
            
            applyDynamicThemes(scene);

            if (animate) {
                FadeTransition fadeIn = new FadeTransition(FADE_IN, root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyDynamicThemes(Scene scene) {
        SettingsService settings = SettingsService.getInstance();
        
        URL darkUrl = SceneNavigator.class.getResource("/org/example/styles/dark-theme.css");
        if (darkUrl != null) {
            String darkThemeUrl = darkUrl.toExternalForm();
            if (settings.isModoOscuro()) {
                if (!scene.getStylesheets().contains(darkThemeUrl)) {
                    scene.getStylesheets().add(darkThemeUrl);
                }
            } else {
                scene.getStylesheets().remove(darkThemeUrl);
            }
        }
        
        scene.getRoot().getStyleClass().removeAll("compact-mode", "spacious-mode");
        if ("Compacta".equals(settings.getDensidadUi())) {
            scene.getRoot().getStyleClass().add("compact-mode");
        } else if ("Espaciosa".equals(settings.getDensidadUi())) {
            scene.getRoot().getStyleClass().add("spacious-mode");
        }
    }
}
