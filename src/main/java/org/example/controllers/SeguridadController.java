package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.models.HistorialItem;
import org.example.models.SecurityThreat;
import org.example.services.HistorialService;
import org.example.services.InstalledProgramsService;
import org.example.services.ProgramasService;
import org.example.services.SecurityService;
import org.example.services.SecurityService.SecurityScanResult;
import org.example.services.SettingsService;
import org.example.util.NotificationUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SeguridadController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SecurityService securityService = SecurityService.getInstance();
    private final ProgramasService programasService = ProgramasService.getInstance();
    private final HistorialService historialService = HistorialService.getInstance();
    private final SettingsService settingsService = SettingsService.getInstance();

    @FXML private ScrollPane rootPane;
    @FXML private VBox contentRoot;
    @FXML private VBox securityStateCard;
    @FXML private Label stateIconLabel;
    @FXML private Label stateLabel;
    @FXML private Label stateSummaryLabel;
    @FXML private Label scoreLabel;
    @FXML private ProgressBar scoreProgress;
    @FXML private Label quickMetaLabel;
    @FXML private Button quickScanButton;
    @FXML private Button deepScanButton;
    @FXML private ProgressIndicator scanProgress;
    @FXML private ProgressBar deepProgressBar;
    @FXML private Label scanStatusLabel;
    @FXML private FlowPane metricsFlow;
    @FXML private Label threatsMetricLabel;
    @FXML private Label riskMetricLabel;
    @FXML private Label programsMetricLabel;
    @FXML private Label systemMetricLabel;
    @FXML private Label lastScanMetricLabel;
    @FXML private VBox threatsList;
    @FXML private VBox recommendationsList;
    @FXML private VBox activityList;

    private Task<SecurityScanResult> currentTask;

    @FXML
    private void initialize() {
        metricsFlow.prefWrapLengthProperty().bind(contentRoot.widthProperty().subtract(32));
        historialService.getEvents().addListener((javafx.collections.ListChangeListener<HistorialItem>) change -> refreshActivity());
        updateView(securityService.getLastResult());
        refreshActivity();
        playEntranceAnimation();
    }

    @FXML
    private void handleQuickScan() {
        runScan(false);
    }

    @FXML
    private void handleDeepScan() {
        runScan(true);
    }

    @FXML
    private void handleActionHover(javafx.scene.input.MouseEvent event) {
        if (!settingsService.isAnimacionesActivas() || !(event.getSource() instanceof Node node)) {
            return;
        }
        ScaleTransition scale = new ScaleTransition(Duration.millis(140), node);
        scale.setToX(1.02);
        scale.setToY(1.02);
        scale.play();
    }

    @FXML
    private void handleActionExit(javafx.scene.input.MouseEvent event) {
        if (!settingsService.isAnimacionesActivas() || !(event.getSource() instanceof Node node)) {
            return;
        }
        ScaleTransition scale = new ScaleTransition(Duration.millis(140), node);
        scale.setToX(1);
        scale.setToY(1);
        scale.play();
    }

    private void runScan(boolean deepScan) {
        if (currentTask != null && currentTask.isRunning()) {
            return;
        }

        setScanning(true, deepScan);
        String scanName = deepScan ? "Analisis profundo" : "Escaneo rapido";
        historialService.addEvent(scanName, "Iniciado desde Seguridad", HistorialService.CATEGORIA_SISTEMA);

        currentTask = new Task<>() {
            @Override
            protected SecurityScanResult call() throws Exception {
                updateProgress(0.12, 1);
                ensureProgramsLoaded();
                updateProgress(0.44, 1);
                if (deepScan) {
                    Thread.sleep(450);
                    updateProgress(0.72, 1);
                    Thread.sleep(350);
                }
                SecurityScanResult result = deepScan
                        ? securityService.runDeepScan()
                        : securityService.runQuickScan();
                updateProgress(1, 1);
                return result;
            }
        };

        deepProgressBar.progressProperty().bind(currentTask.progressProperty());

        currentTask.setOnSucceeded(event -> {
            deepProgressBar.progressProperty().unbind();
            deepProgressBar.setProgress(1);
            setScanning(false, deepScan);
            SecurityScanResult result = currentTask.getValue();
            updateView(result);
            registerResult(scanName, result);
            showToast(scanName + " completado");
        });

        currentTask.setOnFailed(event -> {
            deepProgressBar.progressProperty().unbind();
            deepProgressBar.setProgress(0);
            setScanning(false, deepScan);
            scanStatusLabel.setText("No se pudo completar el analisis.");
            historialService.addEvent("Error de seguridad", "Fallo el analisis del sistema", HistorialService.CATEGORIA_ERROR);
            showToast("No se pudo completar el analisis");
        });

        Thread thread = new Thread(currentTask, deepScan ? "security-deep-scan" : "security-quick-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void ensureProgramsLoaded() throws InterruptedException {
        if (!programasService.getProgramas().isEmpty()) {
            return;
        }
        InstalledProgramsService.ScanResult result = new InstalledProgramsService().scanInstalledPrograms();
        if (result.ok()) {
            CountDownLatch latch = new CountDownLatch(1);
            javafx.application.Platform.runLater(() -> {
                try {
                    programasService.reemplazarInstalados(result.programas());
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
        }
    }

    private void updateView(SecurityScanResult result) {
        updateState(result);
        updateMetrics(result);
        updateThreats(result);
        updateRecommendations(result);
    }

    private void updateState(SecurityScanResult result) {
        securityStateCard.getStyleClass().removeAll("state-safe", "state-attention", "state-risk");
        String icon = switch (result.status()) {
            case SAFE -> "OK";
            case ATTENTION -> "!";
            case RISK -> "X";
        };
        String style = switch (result.status()) {
            case SAFE -> "state-safe";
            case ATTENTION -> "state-attention";
            case RISK -> "state-risk";
        };

        securityStateCard.getStyleClass().add(style);
        stateIconLabel.setText(icon);
        stateLabel.setText(result.status().getDisplayName());
        scoreLabel.setText(result.score() + "/100");
        scoreProgress.setProgress(result.score() / 100.0);

        if (result.scannedAt() == null) {
            stateSummaryLabel.setText("Ejecute un analisis para calcular el estado real de seguridad.");
            quickMetaLabel.setText("Sin analisis reciente");
        } else {
            stateSummaryLabel.setText(result.threats().isEmpty()
                    ? "No se detectaron amenazas relevantes en el ultimo analisis."
                    : result.threats().size() + " hallazgos requieren revision.");
            quickMetaLabel.setText("Ultimo analisis: " + result.scannedAt().format(TIME_FORMAT));
        }
    }

    private void updateMetrics(SecurityScanResult result) {
        threatsMetricLabel.setText(String.valueOf(result.threats().size()));
        riskMetricLabel.setText(result.highThreats() > 0 ? "Alto" : result.mediumThreats() > 0 ? "Medio" : "Bajo");
        programsMetricLabel.setText(String.valueOf(result.programsAnalyzed()));
        systemMetricLabel.setText(result.firewallState().getDisplayName());
        lastScanMetricLabel.setText(result.scannedAt() == null ? "Pendiente" : result.scannedAt().format(TIME_FORMAT));
    }

    private void updateThreats(SecurityScanResult result) {
        threatsList.getChildren().clear();
        List<SecurityThreat> threats = result.sortedThreats();
        if (threats.isEmpty()) {
            threatsList.getChildren().add(createEmptyLabel("No hay amenazas detectadas. El sistema se ve estable."));
            return;
        }

        for (SecurityThreat threat : threats) {
            threatsList.getChildren().add(createThreatRow(threat));
        }
    }

    private HBox createThreatRow(SecurityThreat threat) {
        HBox row = new HBox(12);
        row.getStyleClass().add("threat-row");

        Label icon = new Label(switch (threat.getRiskLevel()) {
            case HIGH -> "!";
            case MEDIUM -> "i";
            case LOW -> "ok";
        });
        icon.getStyleClass().addAll("threat-icon", riskStyle(threat.getRiskLevel()));

        VBox info = new VBox(4);
        Label title = new Label(threat.getName());
        title.getStyleClass().add("threat-title");
        Label description = new Label(threat.getDescription());
        description.getStyleClass().add("threat-description");
        description.setWrapText(true);
        Label recommendation = new Label(threat.getRecommendation());
        recommendation.getStyleClass().add("threat-recommendation");
        recommendation.setWrapText(true);
        info.getChildren().addAll(title, description, recommendation);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox meta = new VBox(6);
        meta.getStyleClass().add("threat-meta");
        Label badge = new Label(threat.getRiskLevel().getDisplayName());
        badge.getStyleClass().addAll("risk-badge", riskStyle(threat.getRiskLevel()));
        Label date = new Label(threat.getDetectedAtFormatted());
        date.getStyleClass().add("threat-date");
        meta.getChildren().addAll(badge, date);

        row.getChildren().addAll(icon, info, meta);
        animateNode(row);
        return row;
    }

    private void updateRecommendations(SecurityScanResult result) {
        recommendationsList.getChildren().clear();
        int index = 0;
        for (String recommendation : result.recommendations()) {
            recommendationsList.getChildren().add(createRecommendation(recommendation, index++));
        }
    }

    private HBox createRecommendation(String text, int index) {
        HBox row = new HBox(10);
        row.getStyleClass().add("recommendation-item");
        Label icon = new Label(index == 0 ? "i" : ">");
        icon.getStyleClass().add("recommendation-icon");
        Label label = new Label(text);
        label.getStyleClass().add("recommendation-text");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);
        row.getChildren().addAll(icon, label);
        animateNode(row);
        return row;
    }

    private void refreshActivity() {
        activityList.getChildren().clear();
        List<HistorialItem> items = historialService.getEvents().stream()
                .filter(item -> item.getTipoEvento().toLowerCase().contains("seguridad")
                        || item.getTipoEvento().toLowerCase().contains("escaneo")
                        || item.getTipoEvento().toLowerCase().contains("analisis")
                        || item.getTipoEvento().toLowerCase().contains("amenaza"))
                .limit(5)
                .toList();

        if (items.isEmpty()) {
            activityList.getChildren().add(createEmptyLabel("Aun no hay actividad de seguridad."));
            return;
        }

        for (HistorialItem item : items) {
            HBox row = new HBox(10);
            row.getStyleClass().add("security-activity-item");
            Label icon = new Label(item.getIcono());
            icon.getStyleClass().add("activity-icon");
            VBox text = new VBox(3);
            Label title = new Label(item.getTipoEvento());
            title.getStyleClass().add("activity-title");
            Label desc = new Label(item.getDescripcion());
            desc.getStyleClass().add("activity-desc");
            desc.setWrapText(true);
            Label time = new Label(item.getFechaHoraFormateada());
            time.getStyleClass().add("activity-time");
            text.getChildren().addAll(title, desc, time);
            HBox.setHgrow(text, Priority.ALWAYS);
            row.getChildren().addAll(icon, text);
            activityList.getChildren().add(row);
        }
    }

    private void registerResult(String scanName, SecurityScanResult result) {
        historialService.addEvent(
                scanName + " completado",
                result.threats().size() + " amenazas, puntuacion " + result.score() + "/100",
                HistorialService.CATEGORIA_SISTEMA
        );
        for (SecurityThreat threat : result.threats()) {
            historialService.addEvent(
                    "Amenaza detectada",
                    threat.getName() + " - Riesgo " + threat.getRiskLevel().getDisplayName(),
                    HistorialService.CATEGORIA_ERROR
            );
        }
    }

    private void setScanning(boolean scanning, boolean deepScan) {
        quickScanButton.setDisable(scanning);
        deepScanButton.setDisable(scanning);
        scanProgress.setVisible(scanning);
        scanProgress.setManaged(scanning);
        deepProgressBar.setVisible(scanning);
        deepProgressBar.setManaged(scanning);
        if (scanning) {
            scanStatusLabel.setText(deepScan ? "Ejecutando analisis profundo..." : "Ejecutando escaneo rapido...");
        } else {
            scanStatusLabel.setText("Listo para analizar.");
        }
    }

    private String riskStyle(SecurityThreat.RiskLevel level) {
        return switch (level) {
            case HIGH -> "risk-high";
            case MEDIUM -> "risk-medium";
            case LOW -> "risk-low";
        };
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("security-empty");
        label.setWrapText(true);
        return label;
    }

    private void showToast(String message) {
        if (rootPane.getScene() != null) {
            NotificationUtil.showToast(rootPane.getScene(), message);
        }
    }

    private void playEntranceAnimation() {
        if (!settingsService.isAnimacionesActivas()) {
            rootPane.setOpacity(1);
            return;
        }
        rootPane.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(360), rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void animateNode(Node node) {
        if (!settingsService.isAnimacionesActivas()) {
            return;
        }
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}
