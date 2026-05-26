package org.example.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class HistorialItem {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String id;
    private final String tipoEvento;
    private final String descripcion;
    private final LocalDateTime fechaHora;
    private final String icono;
    private final String categoria;

    public HistorialItem(
            String tipoEvento,
            String descripcion,
            String categoria,
            String icono
    ) {
        this(
                UUID.randomUUID().toString(),
                tipoEvento,
                descripcion,
                LocalDateTime.now(),
                icono,
                categoria
        );
    }

    public HistorialItem(
            String id,
            String tipoEvento,
            String descripcion,
            LocalDateTime fechaHora,
            String icono,
            String categoria
    ) {
        this.id = id;
        this.tipoEvento = tipoEvento == null ? "" : tipoEvento;
        this.descripcion = descripcion == null ? "" : descripcion;
        this.fechaHora = fechaHora == null ? LocalDateTime.now() : fechaHora;
        this.icono = icono == null ? "•" : icono;
        this.categoria = categoria == null ? "General" : categoria;
    }

    public String getId() {
        return id;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getIcono() {
        return icono;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getFechaHoraFormateada() {
        return fechaHora.format(FORMATTER);
    }
}

