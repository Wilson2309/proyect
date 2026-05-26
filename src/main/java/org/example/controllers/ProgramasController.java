package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.models.Programa;
import org.example.models.ProgramaTipo;
import org.example.services.HistorialService;
import org.example.services.InstalledProgramsService;
import org.example.services.InstalledProgramsService.ScanResult;
import org.example.services.ProgramasService;
import org.example.util.FormSubmitHelper;

import java.util.List;

public class ProgramasController {

    private static final Duration CARD_FADE = Duration.millis(280);
    private static final Duration LIST_FADE = Duration.millis(180);

    private final ProgramasService programasService = ProgramasService.getInstance();
    private final InstalledProgramsService installedProgramsService = new InstalledProgramsService();
    private final HistorialService historialService = HistorialService.getInstance();

    private Task<ScanResult> scanTask;
    private ProgramaTipo tipoActivo = ProgramaTipo.APLICACION;

    @FXML
    private ToggleButton appsToggle;

    @FXML
    private ToggleButton systemToggle;

    @FXML
    private Label resultCountLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Button refreshButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label statusLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private ScrollPane programsScroll;

    @FXML
    private VBox programsList;

    @FXML
    private void initialize() {
        ToggleGroup typeGroup = new ToggleGroup();
        appsToggle.setToggleGroup(typeGroup);
        systemToggle.setToggleGroup(typeGroup);
        appsToggle.setSelected(true);

        typeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                appsToggle.setSelected(true);
                return;
            }
            ProgramaTipo nuevoTipo = newToggle == appsToggle
                    ? ProgramaTipo.APLICACION
                    : ProgramaTipo.SISTEMA;
            if (nuevoTipo != tipoActivo) {
                tipoActivo = nuevoTipo;
                historialService.addEvent(
                        "Filtro de programas",
                        "Vista: " + (nuevoTipo == ProgramaTipo.APLICACION
                                ? "Aplicaciones"
                                : "Controladores y Sistema"),
                        HistorialService.CATEGORIA_PROGRAMAS
                );
                refreshListWithTransition();
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshList());
        FormSubmitHelper.bindEnterAction(this::refreshList, searchField);
        startScan(false);
    }

    @FXML
    private void handleRefreshScan() {
        startScan(true);
    }

    private void startScan(boolean manualRefresh) {
        if (scanTask != null && scanTask.isRunning()) {
            return;
        }

        setLoading(true);
        clearMessage();
        setStatus("Escaneando programas instalados en Windows...");

        historialService.addEvent(
                manualRefresh ? "Escaneo actualizado" : "Escaneo iniciado",
                manualRefresh
                        ? "Actualización manual de programas instalados"
                        : "Detección automática al abrir el módulo Programas",
                HistorialService.CATEGORIA_PROGRAMAS
        );

        scanTask = new Task<>() {
            @Override
            protected ScanResult call() {
                return installedProgramsService.scanInstalledPrograms();
            }
        };

        scanTask.setOnSucceeded(event -> {
            handleScanResult(scanTask.getValue());
            setLoading(false);
        });

        scanTask.setOnFailed(event -> {
            String msg = "No se pudo completar el escaneo.";
            showError(msg);
            setStatus("Error durante el escaneo.");
            historialService.addEvent("Error de escaneo", msg, HistorialService.CATEGORIA_ERROR);
            setLoading(false);
            refreshList();
        });

        scanTask.setOnCancelled(event -> {
            setStatus("Escaneo cancelado.");
            setLoading(false);
        });

        Thread thread = new Thread(scanTask, "installed-programs-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleScanResult(ScanResult result) {
        if (result.ok()) {
            programasService.reemplazarInstalados(result.programas());
            int apps = programasService.contarPorTipo(ProgramaTipo.APLICACION);
            int system = programasService.contarPorTipo(ProgramaTipo.SISTEMA);
            int total = result.programas().size();
            setStatus("Detectados: " + apps + " aplicaciones, " + system + " componentes de sistema.");
            showSuccess("Escaneo completado correctamente.");
            historialService.addEvent(
                    "Escaneo completado",
                    total + " programas detectados (" + apps + " apps, " + system + " sistema)",
                    HistorialService.CATEGORIA_SISTEMA
            );
            refreshList();
            return;
        }

        programasService.clear();
        setStatus(result.errorMessage());
        showError(result.errorMessage());
        historialService.addEvent(
                "Error de escaneo",
                result.errorMessage(),
                HistorialService.CATEGORIA_ERROR
        );
        refreshList();
    }

    private void refreshListWithTransition() {
        FadeTransition fadeOut = new FadeTransition(LIST_FADE, programsList);
        fadeOut.setFromValue(programsList.getOpacity() > 0 ? programsList.getOpacity() : 1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            refreshList();
            FadeTransition fadeIn = new FadeTransition(LIST_FADE, programsList);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void refreshList() {
        programsList.getChildren().clear();
        List<Programa> items = programasService.filtrar(searchField.getText(), tipoActivo);

        updateResultCount(items.size());

        if (items.isEmpty()) {
            Label empty = new Label(buildEmptyMessage());
            empty.getStyleClass().add("programas-empty");
            empty.setWrapText(true);
            programsList.getChildren().add(empty);
            return;
        }

        for (Programa programa : items) {
            programsList.getChildren().add(createProgramCard(programa));
        }
    }

    private String buildEmptyMessage() {
        boolean hasSearch = searchField.getText() != null && !searchField.getText().isBlank();
        if (programasService.getProgramas().isEmpty()) {
            return "No hay programas para mostrar. Pulse «Actualizar escaneo».";
        }
        if (hasSearch) {
            return "No hay resultados para la búsqueda en esta categoría.";
        }
        return tipoActivo == ProgramaTipo.APLICACION
                ? "No se detectaron aplicaciones en esta categoría. Pruebe «Controladores y Sistema»."
                : "No se detectaron controladores o paquetes de sistema en esta categoría.";
    }

    private void updateResultCount(int visible) {
        String label = tipoActivo == ProgramaTipo.APLICACION ? "aplicaciones" : "componentes de sistema";
        resultCountLabel.setText("Mostrando " + visible + " " + label);
    }

    private HBox createProgramCard(Programa programa) {
        HBox card = new HBox();
        card.getStyleClass().add("programa-card");
        card.setOpacity(0);

        VBox info = new VBox(4);
        Label nameLabel = new Label(programa.getNombre());
        nameLabel.getStyleClass().add("programa-name");

        Label metaLabel = new Label(buildMetaText(programa));
        metaLabel.getStyleClass().add("programa-desc");
        metaLabel.setWrapText(true);
        metaLabel.setMaxWidth(480);

        Label detailLabel = new Label(programa.getDescripcion());
        detailLabel.getStyleClass().add("programa-meta");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(480);

        info.getChildren().addAll(nameLabel, metaLabel, detailLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label badge = new Label(tipoActivo == ProgramaTipo.APLICACION ? "Aplicación" : "Sistema");
        badge.getStyleClass().add(
                tipoActivo == ProgramaTipo.APLICACION ? "programa-badge-app" : "programa-badge-system"
        );

        card.getChildren().addAll(info, badge);

        FadeTransition fadeIn = new FadeTransition(CARD_FADE, card);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        return card;
    }

    private String buildMetaText(Programa programa) {
        String publisher = programa.getPublisher().isBlank() ? "Editor desconocido" : programa.getPublisher();
        String version = programa.getVersion().isBlank() ? "Sin versión" : "v" + programa.getVersion();
        String size = programa.getSizeText().isBlank() ? "Tamaño desconocido" : programa.getSizeText();
        return publisher + "  ·  " + version + "  ·  " + size;
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
        refreshButton.setDisable(loading);
        appsToggle.setDisable(loading);
        systemToggle.setDisable(loading);

        if (loading) {
            searchField.setDisable(true);
        } else {
            searchField.setDisable(programasService.getProgramas().isEmpty());
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showError(String text) {
        messageLabel.getStyleClass().remove("programas-message-success");
        if (!messageLabel.getStyleClass().contains("programas-message-error")) {
            messageLabel.getStyleClass().add("programas-message-error");
        }
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String text) {
        messageLabel.getStyleClass().remove("programas-message-error");
        if (!messageLabel.getStyleClass().contains("programas-message-success")) {
            messageLabel.getStyleClass().add("programas-message-success");
        }
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("programas-message-error", "programas-message-success");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}

