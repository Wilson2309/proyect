package org.example.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.services.SettingsService;

public class NotificationUtil {

    public static void showToast(Scene scene, String message) {
        SettingsService settings = SettingsService.getInstance();
        if (!settings.isMensajesSistema() || !settings.isAlertasVisuales()) {
            return;
        }

        Platform.runLater(() -> {
            Window window = scene == null ? null : scene.getWindow();
            if (window == null) {
                return;
            }

            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            Label label = new Label(message);
            label.setStyle(
                    "-fx-background-color: #111827;" +
                    "-fx-text-fill: white;" +
                    "-fx-padding: 12px 24px;" +
                    "-fx-background-radius: 12px;" +
                    "-fx-font-family: 'Segoe UI Variable', sans-serif;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: 600;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);"
            );

            StackPane pane = new StackPane(label);
            pane.setAlignment(Pos.CENTER);
            pane.setOpacity(0);
            popup.getContent().add(pane);

            popup.show(window, window.getX() + window.getWidth() / 2 - 140, window.getY() + window.getHeight() - 96);
            popup.setX(window.getX() + window.getWidth() / 2 - label.getWidth() / 2);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), pane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(220), pane);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e2 -> popup.hide());
                fadeOut.play();
            });
            delay.play();
        });
    }
}
