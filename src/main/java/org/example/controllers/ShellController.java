package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

import org.example.services.AuthService;
import org.example.util.SceneNavigator;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Controlador del shell principal con diseño Premium SaaS.
 */
public class ShellController {

    @FXML private BorderPane shellRoot;
    @FXML private VBox sidebar;
    
    @FXML private Button dashboardButton;
    @FXML private Button programasButton;
    @FXML private Button seguridadButton;
    @FXML private Button limpiezaButton;
    @FXML private Button historialButton;
    @FXML private Button perfilButton;
    @FXML private Button configuracionButton;
    @FXML private Button logoutButton;

    @FXML private StackPane contentArea;
    @FXML private Label mainContentLabel;

    private List<Button> navButtons;

    @FXML
    private void initialize() {
        navButtons = Arrays.asList(
            dashboardButton, programasButton, 
            seguridadButton, limpiezaButton, 
            historialButton, perfilButton,
            configuracionButton
        );
        
        // Cargar vista por defecto y marcar botón activo
        goToDashboard();
        
        setupDynamicThemeListeners();
        startBackgroundScanIfEnabled();
    }
    
    private void setupDynamicThemeListeners() {
        Platform.runLater(() -> {
            javafx.scene.Scene scene = shellRoot.getScene();
            if (scene != null) {
                org.example.services.SettingsService settings = org.example.services.SettingsService.getInstance();
                
                settings.modoOscuroProperty().addListener((obs, oldVal, newVal) -> {
                    SceneNavigator.applyDynamicThemes(scene);
                });
                
                settings.densidadUiProperty().addListener((obs, oldVal, newVal) -> {
                    SceneNavigator.applyDynamicThemes(scene);
                });
            }
        });
    }

    private void startBackgroundScanIfEnabled() {
        if (org.example.services.SettingsService.getInstance().isEscaneoAutomatico()) {
            new Thread(() -> {
                try {
                    // Simular delay de arranque
                    Thread.sleep(1500);
                    org.example.services.InstalledProgramsService service = new org.example.services.InstalledProgramsService();
                    var result = service.scanInstalledPrograms();
                    if (result.ok()) {
                        Platform.runLater(() -> {
                            org.example.services.ProgramasService.getInstance().reemplazarInstalados(result.programas());
                            org.example.services.HistorialService.getInstance().addEvent("Escaneo Automático", "Completado en segundo plano", "Sistema");
                            org.example.util.NotificationUtil.showToast(shellRoot.getScene(), "Escaneo de programas completado");
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void setActiveButton(Button activeBtn) {
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
            }
        }
        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("active")) {
                activeBtn.getStyleClass().add("active");
            }
        }
    }

    /**
     * Carga una vista FXML y la coloca como único hijo del área central.
     */
    private void loadView(String fxmlPath) {
        try {
            URL location = Objects.requireNonNull(
                    ShellController.class.getResource(fxmlPath),
                    "Recurso FXML no encontrado en classpath: " + fxmlPath
            );

            FXMLLoader loader = new FXMLLoader(location);
            Node view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof DashboardController dashboardController) {
                dashboardController.setShellController(this);
            }

            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToDashboard() {
        setActiveButton(dashboardButton);
        loadView("/org/example/views/DashboardView.fxml");
    }

    @FXML
    public void goToProgramas() {
        setActiveButton(programasButton);
        loadView("/org/example/views/ProgramasView.fxml");
    }

    @FXML
    public void goToSeguridad() {
        setActiveButton(seguridadButton);
        loadView("/org/example/views/SeguridadView.fxml");
    }

    @FXML
    public void goToLimpieza() {
        setActiveButton(limpiezaButton);
        loadView("/org/example/views/LimpiezaView.fxml");
    }

    @FXML
    public void goToHistorial() {
        setActiveButton(historialButton);
        loadView("/org/example/views/HistorialView.fxml");
    }

    @FXML
    private void goToPerfil() {
        setActiveButton(perfilButton);
        loadView("/org/example/views/PerfilView.fxml");
    }

    
    @FXML
    private void goToConfiguracion() {
        setActiveButton(configuracionButton);
        loadView("/org/example/views/ConfiguracionView.fxml");
    }


    @FXML
    private void handleLogout() {
        AuthService.getInstance().logout();
        
        SceneNavigator.transition(
                (Stage) logoutButton.getScene().getWindow(),
                shellRoot,
                ShellController.class,
                "/org/example/views/LoginView.fxml",
                480,
                640
        );
    }
}
