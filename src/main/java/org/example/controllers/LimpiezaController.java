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
import org.example.models.CleaningItem;
import org.example.models.CleaningItem.CleaningCategory;
import org.example.models.HistorialItem;
import org.example.models.Programa;
import org.example.services.CleaningService;
import org.example.services.CleaningService.CleaningResult;
import org.example.services.CleaningService.CleaningScanResult;
import org.example.services.HistorialService;
import org.example.services.ProgramasService;
import org.example.services.SettingsService;
import org.example.util.NotificationUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class LimpiezaController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CleaningService cleaningService = CleaningService.getInstance();
    private final HistorialService historialService = HistorialService.getInstance();
    private final ProgramasService programasService = ProgramasService.getInstance();
    private final SettingsService settingsService = SettingsService.getInstance();

    @FXML private ScrollPane rootPane;
    @FXML private VBox contentRoot;
    @FXML private VBox cleaningStateCard;
    @FXML private Label stateIconLabel;
    @FXML private Label stateLabel;
    @FXML private Label stateSummaryLabel;
    @FXML private Label scoreLabel;
    @FXML private Label recoverableHeroLabel;
    @FXML private ProgressBar scoreProgress;
    @FXML private Button analyzeButton;
    @FXML private Button cleanNowButton;
    @FXML private Button freeRamButton;
    @FXML private Button uninstallButton;
    @FXML private ProgressIndicator scanProgress;
    @FXML private ProgressBar actionProgress;
    @FXML private Label actionStatusLabel;
    @FXML private FlowPane metricsFlow;
    @FXML private Label tempMetricLabel;
    @FXML private Label cacheMetricLabel;
    @FXML private Label recoverableMetricLabel;
    @FXML private Label filesMetricLabel;
    @FXML private Label lastAnalysisMetricLabel;
    @FXML private VBox categoryBars;
    @FXML private VBox cleaningItemsList;
    @FXML private VBox recommendationsList;
    @FXML private VBox programsList;
    @FXML private VBox activityList;

    private CleaningScanResult currentResult = CleaningScanResult.empty();
    private Task<?> currentTask;

    @FXML
    private void initialize() {
        metricsFlow.prefWrapLengthProperty().bind(contentRoot.widthProperty().subtract(32));
        historialService.getEvents().addListener((javafx.collections.ListChangeListener<HistorialItem>) change -> refreshActivity());
        updateView(cleaningService.getLastResult());
        refreshProgramsToReview();
        refreshActivity();
        playEntranceAnimation();
    }

    @FXML
    private void handleAnalyze() {
        if (isBusy()) {
            return;
        }
        setBusy(true, "Analizando archivos temporales y cache...");
        historialService.addEvent("Analisis de limpieza", "Iniciado desde Limpieza", HistorialService.CATEGORIA_SISTEMA);

        Task<CleaningScanResult> task = new Task<>() {
            @Override
            protected CleaningScanResult call() throws Exception {
                updateProgress(0.18, 1);
                Thread.sleep(180);
                updateProgress(0.48, 1);
                CleaningScanResult result = cleaningService.analyzeSystem();
                updateProgress(1, 1);
                return result;
            }
        };
        bindTask(task);
        task.setOnSucceeded(event -> {
            finishBusy();
            updateView(task.getValue());
            historialService.addEvent(
                    "Analisis de limpieza completado",
                    cleaningService.formatBytes(task.getValue().recoverableBytes()) + " recuperables detectados",
                    HistorialService.CATEGORIA_SISTEMA
            );
            showToast("Analisis de limpieza completado");
        });
        task.setOnFailed(event -> {
            finishBusy();
            actionStatusLabel.setText("No se pudo completar el analisis.");
            historialService.addEvent("Error de limpieza", "Fallo el analisis de archivos", HistorialService.CATEGORIA_ERROR);
        });
        startTask(task, "cleaning-analyze");
    }

    @FXML
    private void handleCleanNow() {
        if (isBusy()) {
            return;
        }
        if (currentResult.items().isEmpty()) {
            handleAnalyze();
            return;
        }
        setBusy(true, "Ejecutando limpieza segura...");
        Task<CleaningResult> task = new Task<>() {
            @Override
            protected CleaningResult call() throws Exception {
                updateProgress(0.25, 1);
                CleaningResult result = cleaningService.cleanSafeItems(currentResult.items());
                updateProgress(1, 1);
                return result;
            }
        };
        bindTask(task);
        task.setOnSucceeded(event -> {
            finishBusy();
            CleaningResult result = task.getValue();
            updateView(cleaningService.getLastResult());
            String summary = result.deletedCount() + " archivos eliminados, " + cleaningService.formatBytes(result.releasedBytes()) + " liberados";
            actionStatusLabel.setText(summary);
            historialService.addEvent("Archivos eliminados", summary, HistorialService.CATEGORIA_SISTEMA);
            showToast("Limpieza completada");
        });
        task.setOnFailed(event -> {
            finishBusy();
            actionStatusLabel.setText("No se pudo ejecutar la limpieza.");
            historialService.addEvent("Error de limpieza", "Fallo la limpieza segura", HistorialService.CATEGORIA_ERROR);
        });
        startTask(task, "cleaning-clean-now");
    }

    @FXML
    private void handleFreeRam() {
        cleaningService.requestMemoryOptimization();
        actionStatusLabel.setText("Liberacion ligera de RAM solicitada.");
        historialService.addEvent("RAM optimizada", "Se solicito limpieza de memoria de la aplicacion", HistorialService.CATEGORIA_SISTEMA);
        showToast("Optimizacion de RAM solicitada");
    }

    @FXML
    private void handleOpenUninstaller() {
        boolean opened = cleaningService.openWindowsUninstaller() || cleaningService.openProgramsModuleFallback();
        if (opened) {
            actionStatusLabel.setText("Se abrio el gestor de desinstalacion de Windows.");
            historialService.addEvent("Desinstalador abierto", "Acceso seguro para borrar programas", HistorialService.CATEGORIA_PROGRAMAS);
        } else {
            actionStatusLabel.setText("No se pudo abrir el desinstalador de Windows.");
        }
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

    private void updateView(CleaningScanResult result) {
        currentResult = result;
        updateState(result);
        updateMetrics(result);
        updateCategoryBars(result);
        updateItems(result);
        updateRecommendations(result);
    }

    private void updateState(CleaningScanResult result) {
        cleaningStateCard.getStyleClass().removeAll("state-optimal", "state-recommended", "state-saturated");
        String style = switch (result.status()) {
            case OPTIMAL -> "state-optimal";
            case RECOMMENDED -> "state-recommended";
            case SATURATED -> "state-saturated";
        };
        String icon = switch (result.status()) {
            case OPTIMAL -> "OK";
            case RECOMMENDED -> "!";
            case SATURATED -> "X";
        };
        cleaningStateCard.getStyleClass().add(style);
        stateIconLabel.setText(icon);
        stateLabel.setText(result.status().getDisplayName());
        scoreLabel.setText(result.score() + "/100");
        scoreProgress.setProgress(result.score() / 100.0);
        recoverableHeroLabel.setText(cleaningService.formatBytes(result.recoverableBytes()));

        if (result.analyzedAt() == null) {
            stateSummaryLabel.setText("Ejecute un analisis para estimar espacio recuperable y acciones recomendadas.");
        } else {
            stateSummaryLabel.setText(result.filesAnalyzed() + " archivos analizados. "
                    + cleaningService.formatBytes(result.recoverableBytes()) + " recuperables de forma segura.");
        }
    }

    private void updateMetrics(CleaningScanResult result) {
        tempMetricLabel.setText(String.valueOf(result.temporaryFiles()));
        cacheMetricLabel.setText(String.valueOf(result.cacheFiles()));
        recoverableMetricLabel.setText(cleaningService.formatBytes(result.recoverableBytes()));
        filesMetricLabel.setText(String.valueOf(result.filesAnalyzed()));
        lastAnalysisMetricLabel.setText(result.analyzedAt() == null ? "Pendiente" : result.analyzedAt().format(TIME_FORMAT));
    }

    private void updateCategoryBars(CleaningScanResult result) {
        categoryBars.getChildren().clear();
        long total = Math.max(1, result.recoverableBytes());
        for (Map.Entry<CleaningCategory, Long> entry : result.bytesByCategory().entrySet()) {
            categoryBars.getChildren().add(createCategoryBar(entry.getKey(), entry.getValue(), total));
        }
    }

    private VBox createCategoryBar(CleaningCategory category, long bytes, long total) {
        VBox box = new VBox(6);
        HBox header = new HBox(8);
        Label title = new Label(category.getDisplayName());
        title.getStyleClass().add("category-title");
        Label value = new Label(cleaningService.formatBytes(bytes));
        value.getStyleClass().add("category-value");
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().addAll(title, value);

        ProgressBar bar = new ProgressBar(bytes / (double) total);
        bar.getStyleClass().addAll("category-progress", categoryStyle(category));
        bar.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(header, bar);
        return box;
    }

    private void updateItems(CleaningScanResult result) {
        cleaningItemsList.getChildren().clear();
        if (result.items().isEmpty()) {
            cleaningItemsList.getChildren().add(createEmptyLabel("No hay archivos innecesarios listados todavia."));
            return;
        }
        for (CleaningItem item : result.items()) {
            cleaningItemsList.getChildren().add(createCleaningRow(item));
        }
    }

    private HBox createCleaningRow(CleaningItem item) {
        HBox row = new HBox(12);
        row.getStyleClass().add("cleaning-row");

        Label icon = new Label(switch (item.getCategory()) {
            case TEMPORARY -> "TMP";
            case CACHE -> "C";
            case LOGS -> "LOG";
            case JUNK -> "J";
        });
        icon.getStyleClass().addAll("cleaning-icon", categoryStyle(item.getCategory()));

        VBox info = new VBox(4);
        Label name = new Label(item.getName());
        name.getStyleClass().add("item-title");
        Label path = new Label(item.getPath() == null ? "" : item.getPath().toString());
        path.getStyleClass().add("item-path");
        path.setWrapText(true);
        info.getChildren().addAll(name, path);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox meta = new VBox(6);
        meta.getStyleClass().add("item-meta");
        Label badge = new Label(item.getCategory().getDisplayName());
        badge.getStyleClass().addAll("category-badge", categoryStyle(item.getCategory()));
        Label size = new Label(cleaningService.formatBytes(item.getSizeBytes()));
        size.getStyleClass().add("item-size");
        Label date = new Label(item.getModifiedAtFormatted());
        date.getStyleClass().add("item-date");
        meta.getChildren().addAll(badge, size, date);

        row.getChildren().addAll(icon, info, meta);
        animateNode(row);
        return row;
    }

    private void updateRecommendations(CleaningScanResult result) {
        recommendationsList.getChildren().clear();
        for (String recommendation : result.recommendations()) {
            HBox row = new HBox(10);
            row.getStyleClass().add("recommendation-item");
            Label icon = new Label("i");
            icon.getStyleClass().add("recommendation-icon");
            Label text = new Label(recommendation);
            text.getStyleClass().add("recommendation-text");
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);
            row.getChildren().addAll(icon, text);
            recommendationsList.getChildren().add(row);
        }
    }

    private void refreshProgramsToReview() {
        programsList.getChildren().clear();
        List<Programa> programs = cleaningService.suggestedProgramsToReview();
        if (programs.isEmpty()) {
            programsList.getChildren().add(createEmptyLabel("Ejecute un escaneo de programas para ver sugerencias."));
            return;
        }
        for (Programa program : programs) {
            HBox row = new HBox(10);
            row.getStyleClass().add("program-review-row");
            VBox info = new VBox(3);
            Label name = new Label(program.getNombre());
            name.getStyleClass().add("program-title");
            Label publisher = new Label(program.getPublisher().isBlank() ? "Editor desconocido" : program.getPublisher());
            publisher.getStyleClass().add("program-publisher");
            info.getChildren().addAll(name, publisher);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label size = new Label(program.getSizeText().isBlank() ? "N/D" : program.getSizeText());
            size.getStyleClass().add("program-size");
            row.getChildren().addAll(info, size);
            programsList.getChildren().add(row);
        }
    }

    private void refreshActivity() {
        activityList.getChildren().clear();
        List<HistorialItem> items = historialService.getEvents().stream()
                .filter(item -> item.getTipoEvento().toLowerCase().contains("limpieza")
                        || item.getTipoEvento().toLowerCase().contains("optim")
                        || item.getTipoEvento().toLowerCase().contains("elimin")
                        || item.getTipoEvento().toLowerCase().contains("ram"))
                .limit(5)
                .toList();

        if (items.isEmpty()) {
            activityList.getChildren().add(createEmptyLabel("Aun no hay actividad de limpieza."));
            return;
        }
        for (HistorialItem item : items) {
            HBox row = new HBox(10);
            row.getStyleClass().add("activity-item");
            Label icon = new Label(item.getIcono());
            icon.getStyleClass().add("activity-icon");
            VBox text = new VBox(3);
            Label title = new Label(item.getTipoEvento());
            title.getStyleClass().add("activity-title");
            Label desc = new Label(item.getDescripcion());
            desc.getStyleClass().add("activity-desc");
            Label time = new Label(item.getFechaHoraFormateada());
            time.getStyleClass().add("activity-time");
            text.getChildren().addAll(title, desc, time);
            HBox.setHgrow(text, Priority.ALWAYS);
            row.getChildren().addAll(icon, text);
            activityList.getChildren().add(row);
        }
    }

    private void bindTask(Task<?> task) {
        currentTask = task;
        actionProgress.progressProperty().bind(task.progressProperty());
    }

    private void startTask(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isBusy() {
        return currentTask != null && currentTask.isRunning();
    }

    private void setBusy(boolean busy, String message) {
        analyzeButton.setDisable(busy);
        cleanNowButton.setDisable(busy);
        freeRamButton.setDisable(busy);
        uninstallButton.setDisable(busy);
        scanProgress.setVisible(busy);
        scanProgress.setManaged(busy);
        actionProgress.setVisible(busy);
        actionProgress.setManaged(busy);
        actionStatusLabel.setText(message);
    }

    private void finishBusy() {
        actionProgress.progressProperty().unbind();
        actionProgress.setProgress(0);
        setBusy(false, "Listo para optimizar.");
    }

    private String categoryStyle(CleaningCategory category) {
        return switch (category) {
            case TEMPORARY -> "category-temp";
            case CACHE -> "category-cache";
            case LOGS -> "category-logs";
            case JUNK -> "category-junk";
        };
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("cleaning-empty");
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
