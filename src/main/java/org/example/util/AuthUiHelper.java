package org.example.util;

import javafx.animation.FadeTransition;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.util.Duration;

public final class AuthUiHelper {

    private static final Duration MESSAGE_FADE = Duration.millis(280);

    private AuthUiHelper() {
    }

    public static void showError(Label messageLabel, String text) {
        applyMessage(messageLabel, text, "auth-message-error");
    }

    public static void showSuccess(Label messageLabel, String text) {
        applyMessage(messageLabel, text, "auth-message-success");
    }

    public static void clearMessage(Label messageLabel) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("auth-message-error", "auth-message-success");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.setOpacity(0);
    }

    public static void setFieldError(Control field, boolean error) {
        if (field == null) {
            return;
        }
        if (error) {
            if (!field.getStyleClass().contains("auth-field-error")) {
                field.getStyleClass().add("auth-field-error");
            }
        } else {
            field.getStyleClass().remove("auth-field-error");
        }
    }

    public static void clearFieldErrors(Control... fields) {
        for (Control field : fields) {
            setFieldError(field, false);
        }
    }

    private static void applyMessage(Label messageLabel, String text, String styleClass) {
        messageLabel.getStyleClass().removeAll("auth-message-error", "auth-message-success");
        messageLabel.getStyleClass().add(styleClass);
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(MESSAGE_FADE, messageLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
}

