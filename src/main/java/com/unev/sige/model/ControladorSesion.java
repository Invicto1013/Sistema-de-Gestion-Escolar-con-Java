package com.unev.sige.model;

import java.time.LocalDateTime;

public class ControladorSesion {

    private int idSesion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;
    private boolean estadoActivo;
    private String direccionIP;
    private int idUsuario;
    private Usuario usuario;

    // Constructor por defecto
    public ControladorSesion() {
        this.fechaInicio = LocalDateTime.now();
        this.estadoActivo = true;
        this.direccionIP = "127.0.0.1";
    }

    // Constructor completo
    public ControladorSesion(int idSesion, int idUsuario, String direccionIP) {
        this.idSesion = idSesion;
        this.idUsuario = idUsuario;
        this.fechaInicio = LocalDateTime.now();
        this.estadoActivo = true;
        this.direccionIP = direccionIP;
    }

    // Getters y Setters
    public int getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(int idSesion) {
        this.idSesion = idSesion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public boolean isEstadoActivo() {
        return estadoActivo;
    }

    public void setEstadoActivo(boolean estadoActivo) {
        this.estadoActivo = estadoActivo;
    }

    public String getDireccionIP() {
        return direccionIP;
    }

    public void setDireccionIP(String direccionIP) {
        this.direccionIP = direccionIP;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}