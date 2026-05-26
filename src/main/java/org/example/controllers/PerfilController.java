package org.example.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.example.models.HistorialItem;
import org.example.models.UserProfile;
import org.example.services.HistorialService;
import org.example.services.ProgramasService;
import org.example.services.UserProfileService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PerfilController {

    @FXML private ScrollPane rootPane;
    
    @FXML private Label avatarLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    
    @FXML private Label idLabel;
    @FXML private Label regDateLabel;
    @FXML private Label lastAccessLabel;
    
    @FXML private Label programsCountLabel;
    @FXML private Label eventsCountLabel;
    
    @FXML private VBox activityListContainer;

    private final UserProfileService profileService = UserProfileService.getInstance();
    private final ProgramasService programasService = ProgramasService.getInstance();
    private final HistorialService historialService = HistorialService.getInstance();
    
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML
    private void initialize() {
        // Animation
        rootPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), rootPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        Platform.runLater(this::loadData);
    }

    private void loadData() {
        // Load User Profile
        UserProfile user = profileService.getCurrentUser();
        if (user != null) {
            String name = user.getNombre();
            nameLabel.setText(name);
            emailLabel.setText(user.getCorreo());
            idLabel.setText(user.getId());
            
            if (name != null && !name.isEmpty()) {
                avatarLabel.setText(name.substring(0, 1).toUpperCase());
            }

            if (user.getFechaRegistro() != null) {
                regDateLabel.setText(user.getFechaRegistro().format(formatter));
            }
            if (user.getUltimoAcceso() != null) {
                lastAccessLabel.setText(user.getUltimoAcceso().format(formatter));
            }
        }

        // Load Stats
        programsCountLabel.setText(String.valueOf(programasService.getProgramas().size()));
        eventsCountLabel.setText(String.valueOf(historialService.count()));

        // Load Recent Activity (max 5)
        List<HistorialItem> allEvents = historialService.getEvents();
        int maxEvents = Math.min(5, allEvents.size());
        
        if (maxEvents > 0) {
            activityListContainer.getChildren().clear();
            for (int i = 0; i < maxEvents; i++) {
                activityListContainer.getChildren().add(createActivityItem(allEvents.get(i)));
            }
        }
    }

    private HBox createActivityItem(HistorialItem item) {
        HBox hbox = new HBox();
        hbox.getStyleClass().add("activity-item");

        Label iconLabel = new Label(item.getIcono());
        iconLabel.getStyleClass().add("activity-icon");

        VBox textBox = new VBox(2);
        
        Label titleLabel = new Label(item.getTipoEvento());
        titleLabel.getStyleClass().add("activity-title");
        
        Label descLabel = new Label(item.getDescripcion());
        descLabel.getStyleClass().add("activity-desc");
        
        textBox.getChildren().addAll(titleLabel, descLabel);

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label timeLabel = new Label(item.getFechaHoraFormateada());
        timeLabel.getStyleClass().add("activity-time");

        hbox.getChildren().addAll(iconLabel, textBox, spacer, timeLabel);
        return hbox;
    }

    @FXML
    private void handleEdit() {
        System.out.println("Editar perfil clickeado");
    }

    // --- MANEJO DE EXPORTACIÓN ---

    private void exportData(String type, String format) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar Reporte de " + type);
        fileChooser.setInitialFileName("Reporte_" + type + "_" + System.currentTimeMillis() + "." + format.toLowerCase());
        
        javafx.stage.FileChooser.ExtensionFilter extFilter = new javafx.stage.FileChooser.ExtensionFilter(format + " files (*." + format.toLowerCase() + ")", "*." + format.toLowerCase());
        fileChooser.getExtensionFilters().add(extFilter);
        
        java.io.File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            boolean success = false;
            if (type.equals("Programas")) {
                success = org.example.services.ExportService.getInstance().exportarProgramas(file, format);
            } else if (type.equals("Historial")) {
                success = org.example.services.ExportService.getInstance().exportarHistorial(file, format);
            } else if (type.equals("Sistema")) {
                success = org.example.services.ExportService.getInstance().exportarSistema(file, format);
            }

            if (success) {
                historialService.addEvent("Exportación", "Se exportó el reporte de " + type + " en formato " + format, "Sistema");
                org.example.util.NotificationUtil.showToast(rootPane.getScene(), "Reporte de " + type + " exportado con éxito");
            } else {
                org.example.util.NotificationUtil.showToast(rootPane.getScene(), "Error al exportar reporte de " + type);
            }
        }
    }

    @FXML private void exportProgramasPdf() { exportData("Programas", "PDF"); }
    @FXML private void exportProgramasCsv() { exportData("Programas", "CSV"); }
    @FXML private void exportProgramasTxt() { exportData("Programas", "TXT"); }

    @FXML private void exportHistorialPdf() { exportData("Historial", "PDF"); }
    @FXML private void exportHistorialCsv() { exportData("Historial", "CSV"); }
    @FXML private void exportHistorialTxt() { exportData("Historial", "TXT"); }

    @FXML private void exportSistemaPdf() { exportData("Sistema", "PDF"); }
    @FXML private void exportSistemaCsv() { exportData("Sistema", "CSV"); }
    @FXML private void exportSistemaTxt() { exportData("Sistema", "TXT"); }
}
