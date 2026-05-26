package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.services.AuthService;
import org.example.services.AuthService.LoginResult;
import org.example.services.HistorialService;
import org.example.util.AuthUiHelper;
import org.example.util.FormSubmitHelper;
import org.example.util.SceneNavigator;

public class LoginController {

    private static final String REGISTRO_FXML = "/org/example/views/RegistroView.fxml";
    private static final String SHELL_FXML = "/org/example/views/ShellView.fxml";

    private static final double LOGIN_WIDTH = 480;
    private static final double LOGIN_HEIGHT = 640;
    private static final double REGISTRO_HEIGHT = 720;
    private static final double SHELL_WIDTH = 1080;
    private static final double SHELL_HEIGHT = 720;

    private final AuthService authService = AuthService.getInstance();
    private final HistorialService historialService = HistorialService.getInstance();

    @FXML
    private StackPane rootPane;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private void initialize() {
        AuthUiHelper.clearMessage(messageLabel);
        FormSubmitHelper.bindPrimaryAction(loginButton, usernameField, passwordField);
    }

    @FXML
    private void handleLogin() {
        AuthUiHelper.clearFieldErrors(usernameField, passwordField);
        AuthUiHelper.clearMessage(messageLabel);

        String user = usernameField.getText();
        String password = passwordField.getText();

        LoginResult result = authService.login(user, password);

        switch (result) {
            case EMPTY_FIELDS -> {
                AuthUiHelper.showError(messageLabel, "Complete usuario y contraseña.");
                AuthUiHelper.setFieldError(usernameField, user == null || user.isBlank());
                AuthUiHelper.setFieldError(passwordField, password == null || password.isBlank());
            }
            case USER_NOT_FOUND -> {
                AuthUiHelper.showError(messageLabel, "La cuenta no existe. Regístrese primero.");
                AuthUiHelper.setFieldError(usernameField, true);
                historialService.addEvent(
                        "Intento de inicio de sesión",
                        "Cuenta no encontrada: " + user,
                        HistorialService.CATEGORIA_ERROR
                );
            }
            case WRONG_PASSWORD -> {
                AuthUiHelper.showError(messageLabel, "Contraseña incorrecta");
                AuthUiHelper.setFieldError(passwordField, true);
                historialService.addEvent(
                        "Intento de inicio de sesión",
                        "Contraseña incorrecta para: " + user,
                        HistorialService.CATEGORIA_ERROR
                );
            }
            case SUCCESS -> {
                authService.setCurrentSession(user);
                historialService.addEvent(
                        "Inicio de sesión exitoso",
                        "Usuario autenticado: " + user,
                        HistorialService.CATEGORIA_AUTH
                );
                SceneNavigator.transition(
                        (Stage) loginButton.getScene().getWindow(),
                        rootPane,
                        LoginController.class,
                        SHELL_FXML,
                        SHELL_WIDTH,
                        SHELL_HEIGHT
                );
            }
        }
    }

    @FXML
    private void goToRegister() {
        AuthUiHelper.clearMessage(messageLabel);
        SceneNavigator.transition(
                (Stage) registerLink.getScene().getWindow(),
                rootPane,
                LoginController.class,
                REGISTRO_FXML,
                LOGIN_WIDTH,
                REGISTRO_HEIGHT
        );
    }
}
