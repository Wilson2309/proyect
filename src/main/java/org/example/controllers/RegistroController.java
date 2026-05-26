package org.example.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.services.AuthService;
import org.example.services.AuthService.RegisterResult;
import org.example.services.HistorialService;
import org.example.util.AuthUiHelper;
import org.example.util.FormSubmitHelper;
import org.example.util.SceneNavigator;

public class RegistroController {

    private static final String LOGIN_FXML = "/org/example/views/LoginView.fxml";

    private static final double LOGIN_WIDTH = 480;
    private static final double LOGIN_HEIGHT = 640;
    private static final Duration SUCCESS_DELAY = Duration.seconds(1.6);

    private final AuthService authService = AuthService.getInstance();
    private final HistorialService historialService = HistorialService.getInstance();

    @FXML
    private StackPane rootPane;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button registerButton;

    @FXML
    private Button backToLoginButton;

    @FXML
    private void initialize() {
        AuthUiHelper.clearMessage(messageLabel);
        FormSubmitHelper.bindPrimaryAction(
                registerButton,
                nameField,
                emailField,
                passwordField,
                confirmPasswordField
        );
    }

    @FXML
    private void handleRegister() {
        AuthUiHelper.clearFieldErrors(nameField, emailField, passwordField, confirmPasswordField);
        AuthUiHelper.clearMessage(messageLabel);

        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        RegisterResult result = authService.register(email, password, confirm, name);

        switch (result) {
            case EMPTY_FIELDS -> {
                AuthUiHelper.showError(messageLabel, "Complete todos los campos.");
                AuthUiHelper.setFieldError(nameField, name == null || name.isBlank());
                AuthUiHelper.setFieldError(emailField, email == null || email.isBlank());
                AuthUiHelper.setFieldError(passwordField, password == null || password.isBlank());
                AuthUiHelper.setFieldError(confirmPasswordField, confirm == null || confirm.isBlank());
            }
            case EMAIL_EXISTS -> {
                AuthUiHelper.showError(messageLabel, "Este correo ya está registrado. Inicie sesión.");
                AuthUiHelper.setFieldError(emailField, true);
            }
            case PASSWORD_MISMATCH -> {
                AuthUiHelper.showError(messageLabel, "Las contraseñas no coinciden.");
                AuthUiHelper.setFieldError(passwordField, true);
                AuthUiHelper.setFieldError(confirmPasswordField, true);
            }
            case SUCCESS -> onRegisterSuccess(email, name);
        }
    }

    private void onRegisterSuccess(String email, String name) {
        historialService.addEvent(
                "Registro de cuenta",
                "Nueva cuenta creada: " + email + " (" + name + ")",
                HistorialService.CATEGORIA_AUTH
        );

        setFormDisabled(true);
        AuthUiHelper.showSuccess(messageLabel, "Cuenta creada correctamente");

        PauseTransition pause = new PauseTransition(SUCCESS_DELAY);
        pause.setOnFinished(event -> {
            setFormDisabled(false);
            goBackToLogin();
        });
        pause.play();
    }

    @FXML
    private void goBackToLogin() {
        AuthUiHelper.clearMessage(messageLabel);
        SceneNavigator.transition(
                (Stage) backToLoginButton.getScene().getWindow(),
                rootPane,
                RegistroController.class,
                LOGIN_FXML,
                LOGIN_WIDTH,
                LOGIN_HEIGHT
        );
    }

    private void setFormDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        emailField.setDisable(disabled);
        passwordField.setDisable(disabled);
        confirmPasswordField.setDisable(disabled);
        registerButton.setDisable(disabled);
        backToLoginButton.setDisable(disabled);
    }
}
