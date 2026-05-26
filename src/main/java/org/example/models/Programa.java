package org.example.models;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Programa {

    private final String id;
    private String nombre;
    private String descripcion;
    private String version;
    private String publisher;
    private Double sizeMB;
    private String sizeText;
    private final boolean installed;

    public Programa(String nombre, String descripcion) {
        this(UUID.randomUUID().toString(), nombre, descripcion, "", "", null, "", false);
    }

    public Programa(
            String id,
            String nombre,
            String descripcion,
            String version,
            String publisher,
            Double sizeMB,
            String sizeText,
            boolean installed
    ) {
        this.id = id;
        this.nombre = nombre == null ? "" : nombre.trim();
        this.descripcion = descripcion == null ? "" : descripcion;
        this.version = version == null ? "" : version.trim();
        this.publisher = publisher == null ? "" : publisher.trim();
        this.sizeMB = sizeMB;
        this.sizeText = sizeText == null ? "" : sizeText;
        this.installed = installed;
    }

    public static Programa fromInstalled(
            String nombre,
            String version,
            String publisher,
            Integer sizeKb
    ) {
        String safeName = nombre == null ? "" : nombre.trim();
        String safePublisher = publisher == null ? "" : publisher.trim();
        String id = UUID.nameUUIDFromBytes(
                (safeName + "|" + safePublisher).toLowerCase().getBytes(StandardCharsets.UTF_8)
        ).toString();

        Double sizeMB = null;
        String sizeText = "Desconocido";
        if (sizeKb != null && sizeKb > 0) {
            sizeMB = sizeKb / 1024.0;
            sizeText = String.format("%.1f MB", sizeMB);
        }

        String descripcion = buildDescripcion(safePublisher, version, sizeText);

        return new Programa(
                id,
                safeName,
                descripcion,
                version == null ? "" : version.trim(),
                safePublisher,
                sizeMB,
                sizeText,
                true
        );
    }

    private static String buildDescripcion(String publisher, String version, String sizeText) {
        String pub = publisher.isBlank() ? "Editor desconocido" : publisher;
        String ver = version == null || version.isBlank() ? "sin versión" : "v" + version.trim();
        return pub + " · " + ver + " · " + sizeText;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion == null ? "" : descripcion;
    }

    public String getVersion() {
        return version;
    }

    public String getPublisher() {
        return publisher;
    }

    public Double getSizeMB() {
        return sizeMB;
    }

    public String getSizeText() {
        return sizeText;
    }

    public boolean isInstalled() {
        return installed;
    }
}

