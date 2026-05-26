package org.example.models;

import java.time.LocalDateTime;

public class UserProfile {
    private String id;
    private String nombre;
    private String correo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimoAcceso;
    private String avatarPath;

    public UserProfile(String id, String nombre, String correo, LocalDateTime fechaRegistro, LocalDateTime ultimoAcceso, String avatarPath) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.fechaRegistro = fechaRegistro;
        this.ultimoAcceso = ultimoAcceso;
        this.avatarPath = avatarPath;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
}
