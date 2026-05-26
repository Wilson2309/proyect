package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.models.HistorialItem;
import org.example.models.Programa;
import org.example.models.ProgramaTipo;
import org.example.services.HistorialService;
import org.example.services.InstalledProgramsService;
import org.example.services.ProgramasService;
import org.example.services.SettingsService;
import org.example.services.SystemMonitorService;
import org.example.services.SystemStatsService;
import org.example.util.NotificationUtil;

import java.text.DecimalFormat;
import java.util.List;

public class DashboardController {

    private static final int HISTORY_LIMIT = 32;
    private static final DecimalFormat PERCENT = new DecimalFormat("0");
    private static final DecimalFormat SIZE = new DecimalFormat("0.0");

    private final SystemMonitorService monitorService = SystemMonitorService.getInstance();
    private final ProgramasService programasService = ProgramasService.getInstance();
    private final HistorialService historialService = HistorialService.getInstance();
    private final SettingsService settingsService = SettingsService.getInstance();

    private ShellController shellController;
    private int sampleIndex;
    private XYChart.Series<Number, Number> cpuMiniSeries;
    private XYChart.Series<Number, Number> cpuSeries;
    private XYChart.Series<Number, Number> ramSeries;

    @FXML private VBox dashboardRoot;
    @FXML private FlowPane kpiFlow;
    @FXML private FlowPane actionsFlow;
    @FXML private Label cpuValueLabel;
    @FXML private Label cpuStatusLabel;
    @FXML private StackPane cpuMiniChartHost;
    @FXML private Label ramValueLabel;
    @FXML private Label ramDetailLabel;
    @FXML private ProgressBar ramProgress;
    @FXML private Label diskValueLabel;
    @FXML private Label diskDetailLabel;
    @FXML private ProgressIndicator diskIndicator;
    @FXML private Label systemStateLabel;
    @FXML private Label systemStateDescription;
    @FXML private Label systemStateIcon;
    @FXML private VBox systemStateCard;
    @FXML private VBox activityList;
    @FXML private VBox alertsList;
    @FXML private StackPane cpuChartHost;
    @FXML private StackPane ramChartHost;
    @FXML private GridPane programsGrid;
    @FXML private Button quickScanButton;
    @FXML private Button quickCleanButton;
    @FXML private Button securityButton;
    @FXML private Button manageProgramsButton;

    @FXML
    private void initialize() {
        configureResponsiveLayout();
        configureCharts();
        configureQuickActions();
        bindDataSources();
        refreshActivity();
        refreshPrograms();
        monitorService.start();
        updateSnapshot(monitorService.getSnapshot());
        playEntranceAnimation();
    }

    public void setShellController(ShellController shellController) {
        this.shellController = shellController;
    }

    private void configureResponsiveLayout() {
        kpiFlow.prefWrapLengthProperty().bind(dashboardRoot.widthProperty().subtract(36));
        actionsFlow.prefWrapLengthProperty().bind(dashboardRoot.widthProperty().subtract(36));
    }

    private void configureCharts() {
        cpuMiniSeries = new XYChart.Series<>();
        cpuSeries = new XYChart.Series<>();
        ramSeries = new XYChart.Series<>();

        LineChart<Number, Number> miniChart = createLineChart(false);
        miniChart.getData().add(cpuMiniSeries);
        cpuMiniChartHost.getChildren().setAll(miniChart);

        LineChart<Number, Number> cpuChart = createLineChart(true);
        cpuChart.getData().add(cpuSeries);
        cpuChartHost.getChildren().setAll(cpuChart);

        LineChart<Number, Number> ramChart = createLineChart(true);
        ramChart.getData().add(ramSeries);
        ramChartHost.getChildren().setAll(ramChart);
    }

    private LineChart<Number, Number> createLineChart(boolean fullSize) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 25);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setTickLabelsVisible(fullSize);
        yAxis.setTickMarkVisible(false);
        yAxis.setMinorTickVisible(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setHorizontalGridLinesVisible(fullSize);
        chart.setVerticalGridLinesVisible(false);
        chart.getStyleClass().add(fullSize ? "dashboard-chart" : "dashboard-mini-chart");
        return chart;
    }

    private void configureQuickActions() {
        quickScanButton.setOnAction(event -> runQuickScan());
        quickCleanButton.setOnAction(event -> runQuickClean());
        securityButton.setOnAction(event -> navigateToSecurity());
        manageProgramsButton.setOnAction(event -> navigateToPrograms());
    }

    private void bindDataSources() {
        monitorService.snapshotProperty().addListener((obs, oldValue, newValue) -> updateSnapshot(newValue));
        historialService.getEvents().addListener((javafx.collections.ListChangeListener<HistorialItem>) change -> refreshActivity());
        programasService.getProgramas().addListener((javafx.collections.ListChangeListener<Programa>) change -> {
            refreshPrograms();
            updateSnapshot(monitorService.getSnapshot());
        });
    }

    private void updateSnapshot(SystemStatsService.SystemSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        double cpu = snapshot.cpuUsage() * 100;
        double ram = snapshot.memoryUsage() * 100;
        double disk = snapshot.diskUsage() * 100;

        cpuValueLabel.setText(PERCENT.format(cpu) + "%");
        cpuStatusLabel.setText(cpu < 55 ? "Carga estable" : cpu < 82 ? "Uso elevado" : "Carga critica");

        ramValueLabel.setText(PERCENT.format(ram) + "%");
        ramDetailLabel.setText(formatBytes(snapshot.usedMemoryBytes()) + " de " + formatBytes(snapshot.totalMemoryBytes()));
        ramProgress.setProgress(snapshot.memoryUsage());

        diskValueLabel.setText(PERCENT.format(disk) + "%");
        diskDetailLabel.setText(formatBytes(snapshot.freeDiskBytes()) + " libres de " + formatBytes(snapshot.totalDiskBytes()));
        diskIndicator.setProgress(snapshot.diskUsage());

        updateSeries(cpuMiniSeries, cpu);
        updateSeries(cpuSeries, cpu);
        updateSeries(ramSeries, ram);
        updateSystemState(snapshot);
        refreshAlerts(snapshot);
        sampleIndex++;
    }

    private void updateSeries(XYChart.Series<Number, Number> series, double value) {
        series.getData().add(new XYChart.Data<>(sampleIndex, value));
        if (series.getData().size() > HISTORY_LIMIT) {
            series.getData().remove(0);
        }
    }

    private void updateSystemState(SystemStatsService.SystemSnapshot snapshot) {
        int alerts = buildAlerts(snapshot).stream()
                .mapToInt(alert -> switch (alert.priority()) {
                    case RISK -> 2;
                    case WARNING -> 1;
                    case INFO -> 0;
                })
                .sum();

        systemStateCard.getStyleClass().removeAll("state-safe", "state-warning", "state-risk");

        if (alerts >= 4) {
            systemStateLabel.setText("Riesgo");
            systemStateDescription.setText("El equipo requiere atencion inmediata.");
            systemStateIcon.setText("!");
            systemStateCard.getStyleClass().add("state-risk");
        } else if (alerts >= 1) {
            systemStateLabel.setText("Atencion");
            systemStateDescription.setText("Hay puntos que conviene revisar pronto.");
            systemStateIcon.setText("i");
            systemStateCard.getStyleClass().add("state-warning");
        } else {
            systemStateLabel.setText("Seguro");
            systemStateDescription.setText("El sistema opera dentro de rangos saludables.");
            systemStateIcon.setText("OK");
            systemStateCard.getStyleClass().add("state-safe");
        }
    }

    private void refreshActivity() {
        activityList.getChildren().clear();
        List<HistorialItem> events = historialService.getEvents().stream().limit(6).toList();
        if (events.isEmpty()) {
            activityList.getChildren().add(createEmptyLabel("Sin actividad reciente registrada."));
            return;
        }

        for (HistorialItem item : events) {
            activityList.getChildren().add(createActivityItem(item));
        }
    }

    private HBox createActivityItem(HistorialItem item) {
        HBox row = new HBox(12);
        row.getStyleClass().add("timeline-item");

        Label dot = new Label(item.getIcono());
        dot.getStyleClass().add("timeline-icon");

        VBox textBox = new VBox(3);
        Label title = new Label(item.getTipoEvento());
        title.getStyleClass().add("timeline-title");
        Label description = new Label(item.getDescripcion());
        description.getStyleClass().add("timeline-description");
        description.setWrapText(true);
        Label time = new Label(item.getFechaHoraFormateada());
        time.getStyleClass().add("timeline-time");
        textBox.getChildren().addAll(title, description, time);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(dot, textBox);
        animateNode(row);
        return row;
    }

    private void refreshAlerts(SystemStatsService.SystemSnapshot snapshot) {
        alertsList.getChildren().clear();
        for (SmartAlert alert : buildAlerts(snapshot)) {
            alertsList.getChildren().add(createAlertItem(alert));
        }
    }

    private List<SmartAlert> buildAlerts(SystemStatsService.SystemSnapshot snapshot) {
        int programCount = programasService.getProgramas().size();
        long heavyPrograms = programasService.getProgramas().stream()
                .filter(programa -> programa.getSizeMB() != null && programa.getSizeMB() >= 1024)
                .count();

        var alerts = FXCollections.<SmartAlert>observableArrayList();
        if (snapshot.memoryUsage() >= 0.88) {
            alerts.add(new SmartAlert(AlertPriority.RISK, "RAM critica", "El consumo supera el 88%."));
        } else if (snapshot.memoryUsage() >= 0.72) {
            alerts.add(new SmartAlert(AlertPriority.WARNING, "RAM elevada", "Hay presion de memoria activa."));
        }

        if (snapshot.freeDiskBytes() > 0 && snapshot.freeDiskBytes() < 15L * 1024 * 1024 * 1024) {
            alerts.add(new SmartAlert(AlertPriority.WARNING, "Poco espacio", "Quedan menos de 15 GB disponibles."));
        }

        if (heavyPrograms >= 5) {
            alerts.add(new SmartAlert(AlertPriority.WARNING, "Programas pesados", heavyPrograms + " apps superan 1 GB."));
        }

        if (programCount >= 120) {
            alerts.add(new SmartAlert(AlertPriority.INFO, "Inventario amplio", programCount + " programas detectados."));
        }

        if (alerts.isEmpty()) {
            alerts.add(new SmartAlert(AlertPriority.INFO, "Sistema seguro", "No hay alertas relevantes ahora."));
        }
        return alerts;
    }

    private HBox createAlertItem(SmartAlert alert) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("alert-item", switch (alert.priority()) {
            case INFO -> "alert-info";
            case WARNING -> "alert-warning";
            case RISK -> "alert-risk";
        });

        Label icon = new Label(switch (alert.priority()) {
            case INFO -> "i";
            case WARNING -> "!";
            case RISK -> "x";
        });
        icon.getStyleClass().add("alert-icon");

        VBox textBox = new VBox(3);
        Label title = new Label(alert.title());
        title.getStyleClass().add("alert-title");
        Label description = new Label(alert.description());
        description.getStyleClass().add("alert-description");
        description.setWrapText(true);
        textBox.getChildren().addAll(title, description);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(icon, textBox);
        animateNode(row);
        return row;
    }

    private void refreshPrograms() {
        programsGrid.getChildren().clear();
        programsGrid.getRowConstraints().clear();

        addProgramHeader();
        List<Programa> programs = programasService.getProgramas().stream()
                .filter(programa -> programa.getNombre() != null && !programa.getNombre().isBlank())
                .limit(5)
                .toList();

        if (programs.isEmpty()) {
            Label empty = createEmptyLabel("Ejecuta un escaneo para listar programas detectados.");
            programsGrid.add(empty, 0, 1, 3, 1);
            return;
        }

        int row = 1;
        for (Programa programa : programs) {
            addProgramRow(programa, row++);
        }
    }

    private void addProgramHeader() {
        addGridLabel("Programa", "program-table-header", 0, 0);
        addGridLabel("Tamano", "program-table-header", 1, 0);
        addGridLabel("Editor", "program-table-header", 2, 0);
    }

    private void addProgramRow(Programa programa, int row) {
        Label name = addGridLabel(programa.getNombre(), "program-table-cell-strong", 0, row);
        name.setGraphic(new Label(classifyProgramIcon(programa)));
        name.setGraphicTextGap(8);

        addGridLabel(programa.getSizeText().isBlank() ? "N/D" : programa.getSizeText(), "program-table-cell", 1, row);
        String publisher = programa.getPublisher().isBlank() ? "Desconocido" : programa.getPublisher();
        addGridLabel(publisher, "program-table-cell", 2, row);
    }

    private Label addGridLabel(String text, String styleClass, int column, int row) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setMargin(label, new Insets(0, 0, 8, 0));
        programsGrid.add(label, column, row);
        return label;
    }

    private String classifyProgramIcon(Programa programa) {
        ProgramaTipo tipo = ProgramaTipo.APLICACION;
        try {
            tipo = org.example.services.ProgramaClassifier.getInstance().classify(programa);
        } catch (Exception ignored) {
            // Keep the dashboard resilient if a partial program row is loaded.
        }
        return tipo == ProgramaTipo.SISTEMA ? "SYS" : "APP";
    }

    private void runQuickScan() {
        quickScanButton.setDisable(true);
        historialService.addEvent("Escaneo rapido", "Iniciado desde Dashboard", HistorialService.CATEGORIA_PROGRAMAS);

        Task<InstalledProgramsService.ScanResult> task = new Task<>() {
            @Override
            protected InstalledProgramsService.ScanResult call() {
                return new InstalledProgramsService().scanInstalledPrograms();
            }
        };

        task.setOnSucceeded(event -> {
            quickScanButton.setDisable(false);
            InstalledProgramsService.ScanResult result = task.getValue();
            if (result.ok()) {
                programasService.reemplazarInstalados(result.programas());
                historialService.addEvent("Escaneo rapido completado", result.programas().size() + " programas detectados", HistorialService.CATEGORIA_SISTEMA);
                showToast("Escaneo rapido completado");
            } else {
                historialService.addEvent("Error de escaneo", result.errorMessage(), HistorialService.CATEGORIA_ERROR);
                showToast(result.errorMessage());
            }
        });

        task.setOnFailed(event -> {
            quickScanButton.setDisable(false);
            historialService.addEvent("Error de escaneo", "No se pudo ejecutar el escaneo rapido", HistorialService.CATEGORIA_ERROR);
            showToast("No se pudo ejecutar el escaneo");
        });

        Thread thread = new Thread(task, "dashboard-quick-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void runQuickClean() {
        historialService.addEvent("Limpieza rapida", "Revision ligera iniciada desde Dashboard", HistorialService.CATEGORIA_SISTEMA);
        showToast("Limpieza rapida registrada");
        navigateToClean();
    }

    private void navigateToPrograms() {
        if (shellController != null) {
            shellController.goToProgramas();
        }
    }

    private void navigateToSecurity() {
        if (shellController != null) {
            shellController.goToSeguridad();
        }
    }

    private void navigateToClean() {
        if (shellController != null) {
            shellController.goToLimpieza();
        }
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-empty");
        label.setWrapText(true);
        return label;
    }

    private void showToast(String message) {
        if (dashboardRoot.getScene() != null) {
            NotificationUtil.showToast(dashboardRoot.getScene(), message);
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
        double mb = bytes / 1024.0 / 1024.0;
        return SIZE.format(mb) + " MB";
    }

    private void playEntranceAnimation() {
        if (!settingsService.isAnimacionesActivas()) {
            return;
        }
        dashboardRoot.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(360), dashboardRoot);
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

    private enum AlertPriority {
        INFO,
        WARNING,
        RISK
    }

    private record SmartAlert(AlertPriority priority, String title, String description) {
    }
}
