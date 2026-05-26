package org.example.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.models.HistorialItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistorialService {

    public static final String CATEGORIA_TODAS = "Todas";
    public static final String CATEGORIA_AUTH = "Autenticación";
    public static final String CATEGORIA_PROGRAMAS = "Programas";
    public static final String CATEGORIA_SISTEMA = "Sistema";
    public static final String CATEGORIA_ERROR = "Error";
    public static final String CATEGORIA_NAVEGACION = "Navegación";

    private static final HistorialService INSTANCE = new HistorialService();

    private final ObservableList<HistorialItem> events = FXCollections.observableArrayList();

    private HistorialService() {
    }

    public static HistorialService getInstance() {
        return INSTANCE;
    }

    public ObservableList<HistorialItem> getEvents() {
        return events;
    }

    public HistorialItem addEvent(
            String tipoEvento,
            String descripcion,
            String categoria,
            String icono
    ) {
        HistorialItem item = new HistorialItem(tipoEvento, descripcion, categoria, icono);
        events.add(0, item);
        return item;
    }

    public void addEvent(String tipoEvento, String descripcion, String categoria) {
        addEvent(tipoEvento, descripcion, categoria, iconoPorCategoria(categoria));
    }

    public List<HistorialItem> filtrar(String consulta, String categoria) {
        String termino = consulta == null ? "" : consulta.trim().toLowerCase(Locale.ROOT);
        boolean filtrarTexto = !termino.isEmpty();
        boolean filtrarCategoria = categoria != null
                && !categoria.isBlank()
                && !CATEGORIA_TODAS.equalsIgnoreCase(categoria);

        List<HistorialItem> resultado = new ArrayList<>();
        for (HistorialItem item : events) {
            if (filtrarCategoria && !item.getCategoria().equalsIgnoreCase(categoria)) {
                continue;
            }
            if (filtrarTexto && !coincide(item, termino)) {
                continue;
            }
            resultado.add(item);
        }
        return resultado;
    }

    public List<String> getCategorias() {
        return List.of(
                CATEGORIA_TODAS,
                CATEGORIA_AUTH,
                CATEGORIA_PROGRAMAS,
                CATEGORIA_SISTEMA,
                CATEGORIA_ERROR,
                CATEGORIA_NAVEGACION
        );
    }

    public void clearHistory() {
        events.clear();
    }

    public int count() {
        return events.size();
    }

    private boolean coincide(HistorialItem item, String termino) {
        return contains(item.getTipoEvento(), termino)
                || contains(item.getDescripcion(), termino)
                || contains(item.getCategoria(), termino)
                || contains(item.getFechaHoraFormateada(), termino);
    }

    private boolean contains(String value, String termino) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(termino);
    }

    private String iconoPorCategoria(String categoria) {
        if (CATEGORIA_AUTH.equalsIgnoreCase(categoria)) {
            return "🔐";
        }
        if (CATEGORIA_PROGRAMAS.equalsIgnoreCase(categoria)) {
            return "📦";
        }
        if (CATEGORIA_SISTEMA.equalsIgnoreCase(categoria)) {
            return "⚙";
        }
        if (CATEGORIA_ERROR.equalsIgnoreCase(categoria)) {
            return "⚠";
        }
        if (CATEGORIA_NAVEGACION.equalsIgnoreCase(categoria)) {
            return "🧭";
        }
        return "•";
    }
}

