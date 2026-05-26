package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;
import org.example.services.HistorialService;
import org.example.services.InstalledProgramsService;
import org.example.services.ProgramasService;
import org.example.services.SettingsService;
import org.example.services.SystemStatsService;
import org.example.util.NotificationUtil;
import org.example.util.SceneNavigator;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;

public class ConfiguracionController {

    private static final DecimalFormat SIZE = new DecimalFormat("0.0");

    @FXML private ScrollPane rootPane;
    @FXML private CheckBox animacionesToggle;
    @FXML private CheckBox modoOscuroToggle;
    @FXML private ComboBox<String> densidadCombo;
    @FXML private CheckBox escaneoAutoToggle;
    @FXML private CheckBox incluirControladoresToggle;
    @FXML private CheckBox actualizacionAutoToggle;
    @FXML private CheckBox mensajesToggle;
    @FXML private CheckBox alertasToggle;
    @FXML private Label javaVerLabel;
    @FXML private Label osLabel;
    @FXML private Label ramLabel;
    @FXML private Label diskLabel;
    @FXML private Label cpuLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator scanProgress;
    @FXML private Button scanNowButton;
    @FXML private Button resetButton;
    @FXML private Button testToastButton;
    @FXML private Button clearHistoryButton;

    private final SettingsService settingsService = SettingsService.getInstance();
    private final HistorialService historialService = HistorialService.getInstance();
    private boolean initializing = true;

    @FXML
    private void initialize() {
        rootPane.setOpacity(0);
        setupBindings();
        setupChangeFeedback();
        loadSystemInfo();
        setStatus("Configuracion lista. Los cambios se guardan automaticamente.");
        initializing = false;
        playEntranceAnimation();
    }

    private void setupBindings() {
        densidadCombo.getItems().setAll("Compacta", "Normal", "Espaciosa");

        animacionesToggle.selectedProperty().bindBidirectional(settingsService.animacionesActivasProperty());
        modoOscuroToggle.selectedProperty().bindBidirectional(settingsService.modoOscuroProperty());
        densidadCombo.valueProperty().bindBidirectional(settingsService.densidadUiProperty());
        escaneoAutoToggle.selectedProperty().bindBidirectional(settingsService.escaneoAutomaticoProperty());
        incluirControladoresToggle.selectedProperty().bindBidirectional(settingsService.incluirControladoresProperty());
        actualizacionAutoToggle.selectedProperty().bindBidirectional(settingsService.actualizacionAutomaticaProperty());
        mensajesToggle.selectedProperty().bindBidirectional(settingsService.mensajesSistemaProperty());
        alertasToggle.selectedProperty().bindBidirectional(settingsService.alertasVisualesProperty());
    }

    private void setupChangeFeedback() {
        animacionesToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                recordSetting("Animaciones", newValue ? "activadas" : "desactivadas"));

        modoOscuroToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            recordSetting("Modo oscuro", newValue ? "activado" : "desactivado");
            applySceneSettings();
        });

        densidadCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            recordSetting("Densidad de interfaz", newValue);
            applySceneSettings();
        });

        escaneoAutoToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                recordSetting("Escaneo automatico", newValue ? "activado" : "desactivado"));

        incluirControladoresToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                recordSetting("Controladores de sistema", newValue ? "incluidos" : "ocultos"));

        actualizacionAutoToggle.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                org.example.services.SystemMonitorService.getInstance().refreshNow();
            }
            recordSetting("Preferencia de actualizacion", newValue ? "activada" : "guardada como manual");
        });

        mensajesToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                recordSetting("Mensajes del sistema", newValue ? "activados" : "silenciados"));

        alertasToggle.selectedProperty().addListener((obs, oldValue, newValue) ->
                recordSetting("Alertas visuales", newValue ? "activadas" : "desactivadas"));
    }

    private void loadSystemInfo() {
        javaVerLabel.setText(System.getProperty("java.version"));
        osLabel.setText(System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        cpuLabel.setText(ManagementFactory.getOperatingSystemMXBean().getName());

        SystemStatsService.SystemSnapshot snapshot = SystemStatsService.getInstance().capture();
        ramLabel.setText(formatBytes(snapshot.totalMemoryBytes()) + " RAM fisica");
        diskLabel.setText(formatBytes(snapshot.freeDiskBytes()) + " libres de " + formatBytes(snapshot.totalDiskBytes()));
    }

    @FXML
    private void handleScanNow() {
        scanNowButton.setDisable(true);
        scanProgress.setVisible(true);
        scanProgress.setManaged(true);
        setStatus("Escaneando programas instalados...");
        historialService.addEvent("Escaneo manual", "Iniciado desde Configuracion", HistorialService.CATEGORIA_PROGRAMAS);

        Task<InstalledProgramsService.ScanResult> task = new Task<>() {
            @Override
            protected InstalledProgramsService.ScanResult call() {
                return new InstalledProgramsService().scanInstalledPrograms();
            }
        };

        task.setOnSucceeded(event -> {
            scanNowButton.setDisable(false);
            scanProgress.setVisible(false);
            scanProgress.setManaged(false);

            InstalledProgramsService.ScanResult result = task.getValue();
            if (result.ok()) {
                ProgramasService.getInstance().reemplazarInstalados(result.programas());
                setStatus("Escaneo completado: " + result.programas().size() + " programas detectados.");
                historialService.addEvent("Escaneo completado", result.programas().size() + " programas detectados", HistorialService.CATEGORIA_SISTEMA);
                showToast("Escaneo completado");
            } else {
                setStatus(result.errorMessage());
                historialService.addEvent("Error de escaneo", result.errorMessage(), HistorialService.CATEGORIA_ERROR);
            }
        });

        task.setOnFailed(event -> {
            scanNowButton.setDisable(false);
            scanProgress.setVisible(false);
            scanProgress.setManaged(false);
            setStatus("No se pudo ejecutar el escaneo.");
            historialService.addEvent("Error de escaneo", "Fallo el escaneo desde Configuracion", HistorialService.CATEGORIA_ERROR);
        });

        Thread thread = new Thread(task, "settings-manual-scan");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleTestNotification() {
        setStatus("Notificacion de prueba enviada.");
        showToast("OptiScan Pro: notificaciones activas");
        historialService.addEvent("Prueba de notificacion", "El usuario envio una alerta visual de prueba", HistorialService.CATEGORIA_SISTEMA);
    }

    @FXML
    private void handleClearHistory() {
        historialService.clearHistory();
        setStatus("Historial limpiado correctamente.");
        showToast("Historial limpiado");
    }

    @FXML
    private void handleResetDefaults() {
        settingsService.setAnimacionesActivas(true);
        settingsService.setModoOscuro(false);
        settingsService.setDensidadUi("Normal");
        settingsService.setEscaneoAutomatico(true);
        settingsService.setIncluirControladores(false);
        settingsService.setActualizacionAutomatica(true);
        settingsService.setMensajesSistema(true);
        settingsService.setAlertasVisuales(true);
        applySceneSettings();
        setStatus("Preferencias restablecidas a valores recomendados.");
        historialService.addEvent("Configuracion restablecida", "Valores recomendados aplicados", HistorialService.CATEGORIA_SISTEMA);
        showToast("Configuracion restablecida");
    }

    private void recordSetting(String setting, String value) {
        if (initializing) {
            return;
        }
        String message = setting + ": " + value;
        setStatus(message);
        historialService.addEvent("Configuracion actualizada", message, HistorialService.CATEGORIA_SISTEMA);
    }

    private void applySceneSettings() {
        Platform.runLater(() -> {
            if (rootPane.getScene() != null) {
                SceneNavigator.applyDynamicThemes(rootPane.getScene());
            }
        });
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showToast(String message) {
        if (rootPane.getScene() != null) {
            NotificationUtil.showToast(rootPane.getScene(), message);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 GB";
        }
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        if (gb >= 1) {
            return SIZE.format(gb) + " GB";
        }
        return SIZE.format(bytes / 1024.0 / 1024.0) + " MB";
    }

    private void playEntranceAnimation() {
        if (!settingsService.isAnimacionesActivas()) {
            rootPane.setOpacity(1);
            return;
        }
        FadeTransition fadeIn = new FadeTransition(Duration.millis(360), rootPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
}
