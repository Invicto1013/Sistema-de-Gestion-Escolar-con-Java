package com.unev.sige.model;

import java.time.LocalDateTime;

/**
 * Incidencia o reconocimiento en el cuaderno de conducta (tabla `observaciones_conducta`).
 */
public class ObservacionConducta {
    private int id;
    private int idEstudiante;
    private String estudianteNombre;
    private String matricula;
    private int idProfesor;
    private String profesorNombre;
    private Integer idSeccion;
    private String asignaturaNombre;
    private String tipo; // MERITO, CONDUCTA, TARDANZA, TAREA, NOTA_INFORMATIVA
    private String titulo;
    private String descripcion;
    private LocalDateTime fecha;

    public ObservacionConducta() {
    }

    public ObservacionConducta(int id, int idEstudiante, String estudianteNombre, String matricula,
                               int idProfesor, String profesorNombre, Integer idSeccion, String asignaturaNombre,
                               String tipo, String titulo, String descripcion, LocalDateTime fecha) {
        this.id = id;
        this.idEstudiante = idEstudiante;
        this.estudianteNombre = estudianteNombre;
        this.matricula = matricula;
        this.idProfesor = idProfesor;
        this.profesorNombre = profesorNombre;
        this.idSeccion = idSeccion;
        this.asignaturaNombre = asignaturaNombre;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(int idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getEstudianteNombre() { return estudianteNombre; }
    public void setEstudianteNombre(String estudianteNombre) { this.estudianteNombre = estudianteNombre; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public int getIdProfesor() { return idProfesor; }
    public void setIdProfesor(int idProfesor) { this.idProfesor = idProfesor; }

    public String getProfesorNombre() { return profesorNombre; }
    public void setProfesorNombre(String profesorNombre) { this.profesorNombre = profesorNombre; }

    public Integer getIdSeccion() { return idSeccion; }
    public void setIdSeccion(Integer idSeccion) { this.idSeccion = idSeccion; }

    public String getAsignaturaNombre() { return asignaturaNombre; }
    public void setAsignaturaNombre(String asignaturaNombre) { this.asignaturaNombre = asignaturaNombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
