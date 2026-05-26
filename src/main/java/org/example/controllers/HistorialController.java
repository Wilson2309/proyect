package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.models.HistorialItem;
import org.example.services.HistorialService;
import org.example.util.FormSubmitHelper;

import java.util.List;

public class HistorialController {

    private static final Duration ENTRY_FADE = Duration.millis(260);
    private static final Duration LIST_FADE = Duration.millis(180);

    private final HistorialService historialService = HistorialService.getInstance();

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private Button clearButton;

    @FXML
    private Label resultCountLabel;

    @FXML
    private ScrollPane historyScroll;

    @FXML
    private VBox timelineList;

    @FXML
    private void initialize() {
        categoryFilter.getItems().setAll(historialService.getCategorias());
        categoryFilter.getSelectionModel().select(HistorialService.CATEGORIA_TODAS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshTimeline());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshTimelineWithTransition());
        FormSubmitHelper.bindEnterAction(this::refreshTimeline, searchField);

        historialService.getEvents().addListener((ListChangeListener<HistorialItem>) change -> refreshTimeline());

        historialService.addEvent(
                "Módulo abierto",
                "Historial de análisis visualizado",
                HistorialService.CATEGORIA_NAVEGACION
        );

        refreshTimeline();
    }

    @FXML
    private void handleClearHistory() {
        historialService.clearHistory();
        refreshTimelineWithTransition();
    }

    private void refreshTimelineWithTransition() {
        FadeTransition fadeOut = new FadeTransition(LIST_FADE, timelineList);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            refreshTimeline();
            FadeTransition fadeIn = new FadeTransition(LIST_FADE, timelineList);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void refreshTimeline() {
        timelineList.getChildren().clear();

        String categoria = categoryFilter.getSelectionModel().getSelectedItem();
        if (categoria == null || categoria.isBlank()) {
            categoria = HistorialService.CATEGORIA_TODAS;
        }

        List<HistorialItem> items = historialService.filtrar(searchField.getText(), categoria);
        resultCountLabel.setText("Mostrando " + items.size() + " de " + historialService.count() + " eventos");

        if (items.isEmpty()) {
            Label empty = new Label(
                    historialService.count() == 0
                            ? "No hay eventos registrados todavía."
                            : "No hay eventos que coincidan con los filtros actuales."
            );
            empty.getStyleClass().add("historial-empty");
            empty.setWrapText(true);
            timelineList.getChildren().add(empty);
            return;
        }

        for (HistorialItem item : items) {
            timelineList.getChildren().add(createTimelineEntry(item));
        }
    }

    private HBox createTimelineEntry(HistorialItem item) {
        HBox entry = new HBox();
        entry.getStyleClass().add("historial-entry");
        entry.setOpacity(0);

        Label icon = new Label(item.getIcono());
        icon.getStyleClass().addAll("historial-icon", styleClassForCategory(item.getCategoria()));
        icon.setWrapText(false);

        VBox content = new VBox(4);
        Label typeLabel = new Label(item.getTipoEvento());
        typeLabel.getStyleClass().add("historial-event-type");

        Label descLabel = new Label(item.getDescripcion());
        descLabel.getStyleClass().add("historial-event-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(480);

        Label timeLabel = new Label(item.getFechaHoraFormateada());
        timeLabel.getStyleClass().add("historial-event-time");

        content.getChildren().addAll(typeLabel, descLabel, timeLabel);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label badge = new Label(item.getCategoria());
        badge.getStyleClass().add("historial-category-badge");

        entry.getChildren().addAll(icon, content, badge);

        FadeTransition fadeIn = new FadeTransition(ENTRY_FADE, entry);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        return entry;
    }

    private String styleClassForCategory(String categoria) {
        if (HistorialService.CATEGORIA_AUTH.equalsIgnoreCase(categoria)) {
            return "historial-icon-auth";
        }
        if (HistorialService.CATEGORIA_PROGRAMAS.equalsIgnoreCase(categoria)) {
            return "historial-icon-programas";
        }
        if (HistorialService.CATEGORIA_SISTEMA.equalsIgnoreCase(categoria)) {
            return "historial-icon-sistema";
        }
        if (HistorialService.CATEGORIA_ERROR.equalsIgnoreCase(categoria)) {
            return "historial-icon-error";
        }
        if (HistorialService.CATEGORIA_NAVEGACION.equalsIgnoreCase(categoria)) {
            return "historial-icon-nav";
        }
        return "";
    }
}

