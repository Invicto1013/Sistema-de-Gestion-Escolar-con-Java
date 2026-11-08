package com.unev.sige.model;

import java.time.LocalDateTime;

/**
 * Comunicado o circular escolar (tabla `anuncios_escolares`).
 */
public class Anuncio {
    private int id;
    private String titulo;
    private String contenido;
    private int idAutor;
    private String autorNombre;
    private String destinatarios; // TODOS, PROFESORES, ESTUDIANTES, PADRES
    private String prioridad;     // NORMAL, IMPORTANTE, URGENTE
    private LocalDateTime fechaPublicacion;
    private boolean activo;

    public Anuncio() {
    }

    public Anuncio(int id, String titulo, String contenido, int idAutor, String autorNombre,
                   String destinatarios, String prioridad, LocalDateTime fechaPublicacion, boolean activo) {
        this.id = id;
        this.titulo = titulo;
        this.contenido = contenido;
        this.idAutor = idAutor;
        this.autorNombre = autorNombre;
        this.destinatarios = destinatarios;
        this.prioridad = prioridad;
        this.fechaPublicacion = fechaPublicacion;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public int getIdAutor() { return idAutor; }
    public void setIdAutor(int idAutor) { this.idAutor = idAutor; }

    public String getAutorNombre() { return autorNombre; }
    public void setAutorNombre(String autorNombre) { this.autorNombre = autorNombre; }

    public String getDestinatarios() { return destinatarios; }
    public void setDestinatarios(String destinatarios) { this.destinatarios = destinatarios; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
