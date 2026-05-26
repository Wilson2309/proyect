package org.example.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.models.Programa;
import org.example.models.ProgramaTipo;

import java.util.List;
import java.util.Locale;

public class ProgramasService {

    private static final ProgramasService INSTANCE = new ProgramasService();

    private final ObservableList<Programa> programas = FXCollections.observableArrayList();
    private final ProgramaClassifier classifier = ProgramaClassifier.getInstance();

    private ProgramasService() {
    }

    public static ProgramasService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Programa> getProgramas() {
        return programas;
    }

    public void reemplazarInstalados(List<Programa> detectados) {
        if (!org.example.services.SettingsService.getInstance().isIncluirControladores()) {
            detectados = detectados.stream()
                .filter(p -> classifier.classify(p) != ProgramaTipo.SISTEMA)
                .toList();
        }
        programas.setAll(detectados);
    }

    public void clear() {
        programas.clear();
    }

    public int contarPorTipo(ProgramaTipo tipo) {
        return (int) programas.stream()
                .filter(p -> classifier.classify(p) == tipo)
                .count();
    }

    public List<Programa> filtrar(String consulta, ProgramaTipo tipo) {
        String termino = consulta == null ? "" : consulta.trim().toLowerCase(Locale.ROOT);
        boolean filtrarTexto = !termino.isEmpty();

        return programas.stream()
                .filter(p -> classifier.classify(p) == tipo)
                .filter(p -> !filtrarTexto || coincide(p, termino))
                .toList();
    }

    private boolean coincide(Programa programa, String termino) {
        return contains(programa.getNombre(), termino)
                || contains(programa.getDescripcion(), termino)
                || contains(programa.getPublisher(), termino)
                || contains(programa.getVersion(), termino)
                || contains(programa.getSizeText(), termino);
    }

    private boolean contains(String value, String termino) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(termino);
    }
}

